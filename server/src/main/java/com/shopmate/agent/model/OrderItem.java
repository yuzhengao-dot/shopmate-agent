package com.shopmate.agent.model;

import java.math.BigDecimal;

/** 订单明细 */
public record OrderItem(String productName, int quantity, BigDecimal price) {
}