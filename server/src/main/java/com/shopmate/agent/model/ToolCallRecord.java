package com.shopmate.agent.model;

/**
 * 一次工具调用的审计记录。
 *
 * @param status      RUNNING / DONE / ERROR
 * @param durationMs  耗时（毫秒），未结束时为 0
 */
public record ToolCallRecord(String traceId, String sessionId, String toolName, String status,
                             String args, String summary, long durationMs, String createdAt) {
}