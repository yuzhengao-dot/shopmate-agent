package com.shopmate.agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmate.agent.model.AfterSaleTicket;
import com.shopmate.agent.model.KnowledgeHit;
import com.shopmate.agent.model.Order;
import com.shopmate.agent.model.OrderItem;
import com.shopmate.agent.model.Product;
import com.shopmate.agent.repository.OrderRepository;
import com.shopmate.agent.repository.ProductRepository;
import com.shopmate.agent.search.KnowledgeIndex;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 电商 Agent 的工具集：商品检索、订单查询、物流查询、售后建单。
 * 每个方法既能被 LLM 调用，也会把结果以结构化卡片的形式推给前端。
 */
@Component
public class ShopTools {

    public static final String DEMO_USER_ID = "U1001";

    private final ProductRepository products;
    private final OrderRepository orders;
    private final KnowledgeIndex knowledge;
    private final ToolTraceEmitter trace;
    private final ObjectMapper mapper = new ObjectMapper();

    public ShopTools(ProductRepository products, OrderRepository orders,
                     KnowledgeIndex knowledge, ToolTraceEmitter trace) {
        this.products = products;
        this.orders = orders;
        this.knowledge = knowledge;
        this.trace = trace;
    }

    @Tool("检索在售商品。可组合关键词、品类、价格区间过滤，返回商品的 id、名称、品牌、价格、库存、评分与标签。")
    public String searchProducts(
            @P("关键词，如 手机、降噪耳机、扫地机器人、咖啡、精华；可为空") String keyword,
            @P("品类，取值：数码/家电/美妆/食品；可为空") String category,
            @P("价格下限（元），可为空") Double minPrice,
            @P("价格上限（元），可为空") Double maxPrice,
            @ToolMemoryId String memoryId) {

        String id = trace.begin(memoryId, "searchProducts", "检索商品",
                args("keyword", keyword, "category", category, "minPrice", minPrice, "maxPrice", maxPrice));
        try {
            List<Product> found = products.search(keyword, category, minPrice, maxPrice, 8);
            String summary = found.isEmpty() ? "没有匹配的商品" : "找到 " + found.size() + " 件商品";
            trace.done(memoryId, id, "searchProducts", summary,
                    Map.of("type", "products", "items", found));
            if (found.isEmpty()) {
                return "{\"total\":0,\"items\":[]}";
            }
            return mapper.writeValueAsString(Map.of("total", found.size(), "items", found));
        } catch (Exception e) {
            trace.fail(memoryId, id, "searchProducts", e.getMessage());
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("按商品 id 查询单个商品的详细信息，包括库存、评分、卖点与到手价。")
    public String getProductDetail(
            @P("商品 id，例如 1") long productId,
            @ToolMemoryId String memoryId) {

        String id = trace.begin(memoryId, "getProductDetail", "查询商品详情", args("productId", productId));
        try {
            Optional<Product> found = products.findById(productId);
            if (found.isEmpty()) {
                trace.done(memoryId, id, "getProductDetail", "商品不存在", null);
                return "{\"error\":\"商品不存在\"}";
            }
            Product product = found.get();
            Product p = product;
            trace.done(memoryId, id, "getProductDetail", p.name() + " · 库存 " + p.stock(),
                    Map.of("type", "products", "items", List.of(p)));
            return mapper.writeValueAsString(p);
        } catch (Exception e) {
            trace.fail(memoryId, id, "getProductDetail", e.getMessage());
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("查询当前登录用户（U1001）的订单列表，可按状态过滤，返回订单号、状态、金额与商品明细。")
    public String queryOrders(
            @P("状态过滤：ALL / PAID（待发货）/ SHIPPED（已发货）/ DELIVERED（已签收）/ COMPLETED（已完成）") String status,
            @ToolMemoryId String memoryId) {

        String filter = (status == null || status.isBlank()) ? "ALL" : status.toUpperCase();
        String id = trace.begin(memoryId, "queryOrders", "查询我的订单", args("status", filter));
        try {
            List<Order> list = orders.findByUser(DEMO_USER_ID, filter);
            String summary = list.isEmpty() ? "没有符合条件的订单" : "共 " + list.size() + " 笔订单";
            trace.done(memoryId, id, "queryOrders", summary, Map.of("type", "orders", "items", list));
            if (list.isEmpty()) {
                return "{\"total\":0,\"orders\":[]}";
            }
            return mapper.writeValueAsString(Map.of("total", list.size(), "orders", list));
        } catch (Exception e) {
            trace.fail(memoryId, id, "queryOrders", e.getMessage());
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("按订单号查询订单详情，包含收货信息、商品明细与完整物流轨迹。")
    public String getOrderDetail(
            @P("订单号，例如 SM20261105001") String orderNo,
            @ToolMemoryId String memoryId) {

        String id = trace.begin(memoryId, "getOrderDetail", "查询订单详情", args("orderNo", orderNo));
        try {
            Optional<Order> found = orders.findByNo(orderNo);
            if (found.isEmpty()) {
                trace.done(memoryId, id, "getOrderDetail", "订单不存在", null);
                return "{\"error\":\"订单不存在：" + orderNo + "\"}";
            }
            Order order = found.get();
            trace.done(memoryId, id, "getOrderDetail", order.orderNo() + " · " + order.statusText(),
                    Map.of("type", "orders", "items", List.of(order)));
            return mapper.writeValueAsString(order);
        } catch (Exception e) {
            trace.fail(memoryId, id, "getOrderDetail", e.getMessage());
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("查询订单的物流轨迹与承运商信息。用户问「到哪了」「什么时候到」时调用。")
    public String checkLogistics(
            @P("订单号，例如 SM20261105001") String orderNo,
            @ToolMemoryId String memoryId) {

        String id = trace.begin(memoryId, "checkLogistics", "查询物流", args("orderNo", orderNo));
        try {
            Optional<Order> found = orders.findByNo(orderNo);
            if (found.isEmpty()) {
                trace.done(memoryId, id, "checkLogistics", "订单不存在", null);
                return "{\"error\":\"订单不存在：" + orderNo + "\"}";
            }
            Order order = found.get();
            String summary = order.tracking().isEmpty()
                    ? "暂无物流轨迹（" + order.statusText() + "）"
                    : "最新：" + order.tracking().get(order.tracking().size() - 1).text();
            trace.done(memoryId, id, "checkLogistics", summary, Map.of("type", "orders", "items", List.of(order)));
            return mapper.writeValueAsString(Map.of(
                    "orderNo", order.orderNo(),
                    "statusText", order.statusText(),
                    "carrier", order.carrier(),
                    "logisticsNo", order.logisticsNo(),
                    "tracking", order.tracking()));
        } catch (Exception e) {
            trace.fail(memoryId, id, "checkLogistics", e.getMessage());
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("""
            检索平台规则知识库。凡是不依赖具体订单的政策类问题都先查这里：
            退换货规则、退货运费、退款到账时间、换货时效、保修政策、发票开具、发货与配送时效、
            签收验货、优惠券与积分、价格保护、售后处理时效、支付方式、客服时间、安装服务。
            回答要依据检索到的规则，并点明依据的是哪一条。""")
    public String searchKnowledge(
            @P("用户的问题或关键词，例如 退货运费谁承担、保修多久、多久能发货") String query,
            @ToolMemoryId String memoryId) {

        String id = trace.begin(memoryId, "searchKnowledge", "检索知识库", args("query", query));
        try {
            List<KnowledgeHit> hits = knowledge.search(query, 3);
            if (hits.isEmpty()) {
                trace.done(memoryId, id, "searchKnowledge", "知识库没有匹配的规则", null);
                return "{\"total\":0,\"items\":[]}";
            }
            trace.done(memoryId, id, "searchKnowledge", "命中 " + hits.size() + " 条平台规则",
                    Map.of("type", "knowledge", "items", hits));
            return mapper.writeValueAsString(Map.of("total", hits.size(), "items", hits));
        } catch (Exception e) {
            trace.fail(memoryId, id, "searchKnowledge", e.getMessage());
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("""
            为已签收或已完成的订单生成售后申请草稿（退货退款 / 换货 / 维修）。
            该工具只做资格校验并返回待确认草稿，不会真正创建工单；工单必须由用户在界面点击「确认提交」后才会写入。
            因此调用后不要说工单已创建，而要请用户确认。""")
    public String prepareAfterSaleTicket(
            @P("订单号，例如 SM20261028002") String orderNo,
            @P("售后类型：退货退款 / 换货 / 维修") String type,
            @P("用户描述的问题，例如 吸力变弱、外包装破损") String reason,
            @ToolMemoryId String memoryId) {

        String id = trace.begin(memoryId, "prepareAfterSaleTicket", "售后预检",
                args("orderNo", orderNo, "type", type, "reason", reason));
        try {
            Optional<Order> found = orders.findByNo(orderNo);
            if (found.isEmpty()) {
                trace.fail(memoryId, id, "prepareAfterSaleTicket", "订单不存在");
                return "{\"error\":\"订单不存在：" + orderNo + "\"}";
            }
            Order order = found.get();
            if (!"DELIVERED".equals(order.status()) && !"COMPLETED".equals(order.status())) {
                trace.fail(memoryId, id, "prepareAfterSaleTicket", "当前状态不可申请售后");
                return "{\"error\":\"订单 " + orderNo + " 当前为「" + order.statusText()
                        + "」，只有已签收或已完成的订单可以申请售后\"}";
            }
            Optional<AfterSaleTicket> existing = orders.findTicketByOrder(orderNo);
            if (existing.isPresent()) {
                trace.fail(memoryId, id, "prepareAfterSaleTicket", "已有售后工单");
                return "{\"error\":\"订单 " + orderNo + " 已有售后工单：" + existing.get().ticketNo()
                        + "，请勿重复提交\"}";
            }

            String ticketType = (type == null || type.isBlank()) ? "退货退款" : type.trim();
            String desc = (reason == null || reason.isBlank()) ? "用户未填写具体原因" : reason.trim();
            Map<String, Object> draft = new LinkedHashMap<>();
            draft.put("orderNo", order.orderNo());
            draft.put("statusText", order.statusText());
            draft.put("type", ticketType);
            draft.put("reason", desc);
            draft.put("totalAmount", order.totalAmount());
            draft.put("productName", order.items().isEmpty() ? "" : order.items().get(0).productName());

            trace.done(memoryId, id, "prepareAfterSaleTicket", "待用户确认：" + ticketType,
                    Map.of("type", "afterSaleDraft", "item", draft));
            return mapper.writeValueAsString(Map.of(
                    "draft", draft,
                    "nextStep", "草稿已生成，等待用户在界面点击「确认提交」后才会真正建单"));
        } catch (Exception e) {
            trace.fail(memoryId, id, "prepareAfterSaleTicket", e.getMessage());
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /** 供演示模式复用：默认取最近的订单 */
    public List<Order> recentOrders(int limit) {
        List<Order> all = orders.findByUser(DEMO_USER_ID, "ALL");
        return all.size() <= limit ? all : all.subList(0, limit);
    }

    public Optional<Order> latestOrder() {
        return orders.findLatest(DEMO_USER_ID);
    }

    public List<Product> allProducts() {
        return products.findAll();
    }

    public List<OrderItem> itemsOf(Order order) {
        return new ArrayList<>(order.items());
    }

    private Map<String, Object> args(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            Object value = pairs[i + 1];
            if (value != null && !"".equals(value)) {
                map.put(String.valueOf(pairs[i]), value);
            }
        }
        return map;
    }
}