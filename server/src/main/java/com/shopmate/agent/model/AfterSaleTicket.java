package com.shopmate.agent.model;

/** 售后工单 */
public record AfterSaleTicket(
        String ticketNo,
        String orderNo,
        String type,
        String reason,
        String status,
        String createdAt) {
}