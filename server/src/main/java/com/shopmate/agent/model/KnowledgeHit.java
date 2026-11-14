package com.shopmate.agent.model;

/**
 * 检索命中结果。
 *
 * @param content 规则全文，给模型作答用
 * @param snippet 命中位置附近的摘录，给前端展示用
 * @param match   相对相关度（0~1，按本次检索的最高分归一化）
 */
public record KnowledgeHit(long id, String title, String category, String content, String snippet, double match) {
}