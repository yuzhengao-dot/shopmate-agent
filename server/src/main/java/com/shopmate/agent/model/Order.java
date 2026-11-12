package com.shopmate.agent.model;

import java.math.BigDecimal;
import java.util.List;

/** 订单（含明细与物流轨迹） */
public record Order(
        String orderNo,
        String status,
        String statusText,
        String createdAt,
        BigDecimal totalAmount,
        String receiver,
        String address,
        String carrier,
        String logisticsNo,
        List<OrderItem> items,
        List<TrackingNode> tracking) {
}