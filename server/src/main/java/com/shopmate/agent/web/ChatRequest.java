package com.shopmate.agent.web;

/** 对话请求 */
public record ChatRequest(String sessionId, String message) {
}