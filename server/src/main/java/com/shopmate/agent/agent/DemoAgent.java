package com.shopmate.agent.agent;

import com.shopmate.agent.model.KnowledgeHit;
import com.shopmate.agent.model.Order;
import com.shopmate.agent.model.Product;
import com.shopmate.agent.model.TrackingNode;
import com.shopmate.agent.repository.OrderRepository;
import com.shopmate.agent.repository.ProductRepository;
import com.shopmate.agent.search.KnowledgeIndex;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 演示模式：未配置大模型密钥时，用规则引擎跑通同样的「意图识别 -> 工具调用 -> 流式回答」链路。
 * 它调用的是与 LLM 完全相同的 ShopTools，因此工具面板、商品/订单卡片都能正常展示。
 */
@Component
public class DemoAgent {

    private static final Pattern ORDER_NO = Pattern.compile("SM\\d{8,}");
    private static final Pattern AMOUNT = Pattern.compile("(\\d{3,6})\\s*(?:元|块|rmb|以内|左右|预算)?");

    private static final List<String> KEYWORDS = List.of(
            "手机", "耳机", "扫地机器人", "显示器", "净化器", "吹风机",
            "洁面", "精华", "咖啡", "音箱");

    /** 政策类问法的线索词：命中就先查知识库，而不是当成具体订单诉求 */
    private static final List<String> KNOWLEDGE_WORDS = List.of(
            "保修", "质保", "发票", "运费", "包邮", "优惠券", "积分", "无理由", "价格保护",
            "保价", "降价", "到账", "配送时效", "配送范围", "配送费", "多久发货", "什么时候发货",
            "多久能到", "什么时候能到", "几天到", "多久到", "什么时候到", "支付方式",
            "会员", "人工客服", "客服",
            "几点上班", "安装", "验货", "偏远", "处理时效", "多久处理", "退多少钱");

    private final ShopTools tools;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final KnowledgeIndex knowledge;

    public DemoAgent(ShopTools tools, ProductRepository products, OrderRepository orders,
                     KnowledgeIndex knowledge) {
        this.tools = tools;
        this.products = products;
        this.orders = orders;
        this.knowledge = knowledge;
    }

    public void reply(String sessionId, String message, ChatStream stream) {
        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) {
            say(stream, "我在。可以帮你挑商品、查订单物流，或者处理退货换货。");
            return;
        }

        // 政策类问题优先走知识库；带订单号时说明用户在问具体某一单，仍走订单链路
        if (extractOrderNo(text) == null
                && containsAny(text, KNOWLEDGE_WORDS.toArray(String[]::new))) {
            knowledge(sessionId, text, stream);
            return;
        }
        if (containsAny(text, "退货", "退款", "退掉", "退换", "售后", "换货", "维修", "坏了", "损坏", "质量", "不想要")) {
            afterSale(sessionId, text, stream);
            return;
        }
        if (containsAny(text, "物流", "快递", "发货", "到哪", "签收", "配送", "什么时候到")) {
            logistics(sessionId, text, stream);
            return;
        }
        if (containsAny(text, "订单", "买了", "我的单", "下单")) {
            orderList(sessionId, text, stream);
            return;
        }
        if (containsAny(text, "推荐", "买", "预算", "适合", "哪个", "怎么选", "对比")
                || KEYWORDS.stream().anyMatch(text::contains)) {
            recommend(sessionId, text, stream);
            return;
        }
        if (containsAny(text, "你好", "嗨", "hello", "hi", "在吗")) {
            say(stream, """
                    你好，我是 ShopMate 的导购助手小满。可以直接问我：

                    1. 预算 3000 元以内，推荐一款拍照好的手机
                    2. 我的订单到哪了
                    3. 我想退掉上周买的扫地机器人
                    4. 降噪耳机有什么推荐
                    """);
            return;
        }

        say(stream, """
                这个我暂时没有把握。你可以这样问我：

                - 商品：帮我推荐 500 元以内的耳机
                - 订单：我最近的订单是什么状态
                - 物流：SM20261105001 到哪了
                - 售后：我想退掉 SM20261028002 这单
                - 规则：退货的运费谁承担

                需要人工介入的话，回复「人工客服」。
                """);
    }

    private void recommend(String sessionId, String text, ChatStream stream) {
        String keyword = KEYWORDS.stream().filter(text::contains).findFirst().orElse(null);
        Double budget = null;
        Matcher matcher = AMOUNT.matcher(ORDER_NO.matcher(text).replaceAll(" "));
        while (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1));
            if (value >= 100 && value <= 50000) {
                budget = value;
            }
        }

        tools.searchProducts(keyword, null, null, budget, sessionId);
        List<Product> found = products.search(keyword, null, null, budget, 3);

        if (found.isEmpty()) {
            say(stream, String.format(Locale.CHINA,
                    "%s暂时没有符合条件的在售商品。可以放宽预算或换个品类，我按销量和评分再帮你筛一轮。",
                    budget == null ? "" : "这个预算内"));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(budget == null ? "按你的需求" : String.format(Locale.CHINA, "预算 %,.0f 元以内", budget));
        if (keyword != null) {
            sb.append("，").append(keyword).append("相关的");
        }
        sb.append("我选了 ").append(found.size()).append(" 件：\n\n");
        for (int i = 0; i < found.size(); i++) {
            Product p = found.get(i);
            sb.append(i + 1).append(". ").append(p.name())
                    .append(" · ¥").append(money(p.price())).append("\n   ")
                    .append(firstSentence(p.summary())).append("\n\n");
        }
        sb.append("需要看哪件的详情，或者想比较其中两款，告诉我就行。");
        say(stream, sb.toString());
    }

    private void orderList(String sessionId, String text, ChatStream stream) {
        String orderNo = extractOrderNo(text);
        if (orderNo != null) {
            logistics(sessionId, text, stream);
            return;
        }
        List<Order> list = orders.findByUser(ShopTools.DEMO_USER_ID, "ALL");
        tools.queryOrders("ALL", sessionId);
        if (list.isEmpty()) {
            say(stream, "你名下暂时没有订单。");
            return;
        }
        StringBuilder sb = new StringBuilder("你名下共有 ").append(list.size()).append(" 笔订单：\n\n");
        for (int i = 0; i < list.size(); i++) {
            Order o = list.get(i);
            sb.append(i + 1).append(". ").append(o.orderNo())
                    .append(" · ").append(o.statusText())
                    .append(" · ¥").append(money(o.totalAmount()))
                    .append(" · ").append(o.createdAt()).append("\n");
        }
        sb.append("\n想看物流或申请售后，把订单号发我就行。");
        say(stream, sb.toString());
    }

    private void logistics(String sessionId, String text, ChatStream stream) {
        String orderNo = extractOrderNo(text);
        Optional<Order> target = orderNo != null ? orders.findByNo(orderNo) : orders.findLatest(ShopTools.DEMO_USER_ID);
        if (target.isEmpty()) {
            say(stream, "没找到对应的订单，请确认订单号，格式类似 SM20261105001。");
            return;
        }
        Order order = target.get();
        tools.checkLogistics(order.orderNo(), sessionId);

        StringBuilder sb = new StringBuilder();
        sb.append("订单 ").append(order.orderNo()).append(" 当前状态：").append(order.statusText()).append("\n");
        if (order.logisticsNo() != null && !order.logisticsNo().isBlank()) {
            sb.append("承运商：").append(order.carrier()).append("（").append(order.logisticsNo()).append("）\n");
        }
        List<TrackingNode> tracking = order.tracking();
        if (tracking.isEmpty()) {
            sb.append("\n仓库还没有揽收，发货后我会同步轨迹。");
        } else {
            sb.append("\n最新轨迹：\n");
            for (int i = Math.max(0, tracking.size() - 2); i < tracking.size(); i++) {
                TrackingNode node = tracking.get(i);
                sb.append("- ").append(node.time()).append("  ").append(node.text()).append("\n");
            }
        }
        say(stream, sb.toString());
    }

    private void afterSale(String sessionId, String text, ChatStream stream) {
        String orderNo = extractOrderNo(text);
        Optional<Order> target;
        if (orderNo != null) {
            target = orders.findByNo(orderNo);
        } else {
            target = orders.findByUser(ShopTools.DEMO_USER_ID, "DELIVERED").stream().findFirst();
        }
        if (target.isEmpty()) {
            say(stream, "请把要售后的订单号发我，格式类似 SM20261028002。");
            return;
        }
        Order order = target.get();
        String type = text.contains("换") ? "换货" : (text.contains("维修") || text.contains("坏") ? "维修" : "退货退款");
        String result = tools.prepareAfterSaleTicket(order.orderNo(), type, text, sessionId);
        if (!result.contains("\"draft\"")) {
            say(stream, "这单暂时无法申请售后：" + order.orderNo() + "（" + order.statusText()
                    + "）。已签收或已完成的订单才能申请，其他情况请回复「人工客服」。");
            return;
        }
        String productName = order.items().isEmpty() ? "订单商品" : order.items().get(0).productName();
        say(stream, String.format("""
                已为订单 %s 生成「%s」申请草稿，还没有提交。

                - 商品：%s
                - 金额：¥%,.2f
                - 当前状态：%s

                请在下方卡片点击「确认提交」，确认后才会真正创建工单；不想提交可以点「取消」。
                """, order.orderNo(), type, productName, order.totalAmount(), order.statusText()));
    }

    private void knowledge(String sessionId, String text, ChatStream stream) {
        tools.searchKnowledge(text, sessionId);
        List<KnowledgeHit> hits = knowledge.search(text, 2);
        if (hits.isEmpty()) {
            say(stream, "这条我在平台规则里没查到。可以换个说法，或者回复「人工客服」让同事帮你确认。");
            return;
        }
        KnowledgeHit top = hits.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("按平台《").append(top.title()).append("》：\n\n").append(top.content()).append("\n\n");
        if (hits.size() > 1) {
            KnowledgeHit second = hits.get(1);
            sb.append("另外《").append(second.title()).append("》也相关：")
                    .append(firstSentence(second.content())).append("\n\n");
        }
        sb.append("以上是平台现行规则；具体到某一单的处理进度，把订单号发我就能查。");
        say(stream, sb.toString());
    }

    private String extractOrderNo(String text) {
        Matcher matcher = ORDER_NO.matcher(text.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group() : null;
    }

    private void say(ChatStream stream, String text) {
        int step = 6;
        for (int i = 0; i < text.length(); i += step) {
            stream.token(text.substring(i, Math.min(i + step, text.length())));
            try {
                Thread.sleep(12);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00" : String.format(Locale.CHINA, "%,.2f", value);
    }

    private static String firstSentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int idx = text.indexOf('。');
        return idx > 0 ? text.substring(0, idx + 1) : text;
    }

    private static boolean containsAny(String text, String... words) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (lower.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}