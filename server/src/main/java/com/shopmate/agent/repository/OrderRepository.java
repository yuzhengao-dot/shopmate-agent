package com.shopmate.agent.repository;

import com.shopmate.agent.model.AfterSaleTicket;
import com.shopmate.agent.model.Order;
import com.shopmate.agent.model.OrderItem;
import com.shopmate.agent.model.TrackingNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Repository
public class OrderRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TICKET_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final RowMapper<Order> MAPPER = (rs, rowNum) -> new Order(
            rs.getString("order_no"),
            rs.getString("status"),
            rs.getString("status_text"),
            rs.getString("created_at"),
            rs.getBigDecimal("total_amount"),
            rs.getString("receiver"),
            rs.getString("address"),
            rs.getString("carrier"),
            rs.getString("logistics_no"),
            List.of(),
            List.of());

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Order> findByUser(String userId, String status) {
        String sql = "SELECT * FROM shop_order WHERE user_id = ?";
        boolean filtered = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);
        if (filtered) {
            sql += " AND status = ?";
            return jdbc.query(sql + " ORDER BY created_at DESC", MAPPER, userId, status.toUpperCase())
                    .stream().map(this::withDetails).toList();
        }
        return jdbc.query(sql + " ORDER BY created_at DESC", MAPPER, userId)
                .stream().map(this::withDetails).toList();
    }

    public Optional<Order> findByNo(String orderNo) {
        return jdbc.query("SELECT * FROM shop_order WHERE order_no = ?", MAPPER, orderNo)
                .stream().findFirst().map(this::withDetails);
    }

    public Optional<Order> findLatest(String userId) {
        return jdbc.query("SELECT * FROM shop_order WHERE user_id = ? ORDER BY created_at DESC LIMIT 1", MAPPER, userId)
                .stream().findFirst().map(this::withDetails);
    }

    public Optional<AfterSaleTicket> findTicketByOrder(String orderNo) {
        return jdbc.query(
                        "SELECT * FROM after_sale_ticket WHERE order_no = ? ORDER BY created_at DESC LIMIT 1",
                        (rs, rowNum) -> new AfterSaleTicket(
                                rs.getString("ticket_no"),
                                rs.getString("order_no"),
                                rs.getString("type"),
                                rs.getString("reason"),
                                rs.getString("status"),
                                rs.getString("created_at")),
                        orderNo)
                .stream().findFirst();
    }

    /**
     * 创建售后工单。仅已签收 / 已完成的订单可申请。
     */
    public AfterSaleTicket createTicket(String orderNo, String type, String reason) {
        Order order = findByNo(orderNo).orElseThrow(() -> new IllegalArgumentException("订单不存在：" + orderNo));
        if (!"DELIVERED".equals(order.status()) && !"COMPLETED".equals(order.status())) {
            throw new IllegalStateException("订单 " + orderNo + " 当前状态为「" + order.statusText() + "」，暂不支持申请售后");
        }
        Optional<AfterSaleTicket> existing = findTicketByOrder(orderNo);
        if (existing.isPresent()) {
            throw new IllegalStateException("订单 " + orderNo + " 已有售后工单：" + existing.get().ticketNo());
        }

        String ticketNo = "AS" + LocalDateTime.now().format(TICKET_TS)
                + ThreadLocalRandom.current().nextInt(100, 1000);
        String createdAt = LocalDateTime.now().format(TS);
        String safeType = (type == null || type.isBlank()) ? "退货退款" : type.trim();
        String safeReason = (reason == null || reason.isBlank()) ? "用户申请售后，待客服核实" : reason.trim();

        jdbc.update("INSERT INTO after_sale_ticket (ticket_no, order_no, type, reason, status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                ticketNo, orderNo, safeType, safeReason, "待审核", createdAt);

        return new AfterSaleTicket(ticketNo, orderNo, safeType, safeReason, "待审核", createdAt);
    }

    public BigDecimal sumByUser(String userId) {
        BigDecimal sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM shop_order WHERE user_id = ?", BigDecimal.class, userId);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private Order withDetails(Order order) {
        List<OrderItem> items = jdbc.query(
                "SELECT product_name, quantity, price FROM order_item WHERE order_no = ?",
                (rs, rowNum) -> new OrderItem(
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("price")),
                order.orderNo());
        List<TrackingNode> tracking = jdbc.query(
                "SELECT ts, text FROM order_tracking WHERE order_no = ? ORDER BY ts",
                (rs, rowNum) -> new TrackingNode(rs.getString("ts"), rs.getString("text")),
                order.orderNo());
        return new Order(order.orderNo(), order.status(), order.statusText(), order.createdAt(),
                order.totalAmount(), order.receiver(), order.address(), order.carrier(),
                order.logisticsNo(), items, tracking);
    }
}