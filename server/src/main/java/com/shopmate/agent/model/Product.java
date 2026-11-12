package com.shopmate.agent.model;

import java.math.BigDecimal;
import java.util.List;

/** 商品 */
public record Product(
        long id,
        String name,
        String brand,
        String category,
        BigDecimal price,
        BigDecimal originalPrice,
        BigDecimal rating,
        int stock,
        List<String> tags,
        String summary) {
}