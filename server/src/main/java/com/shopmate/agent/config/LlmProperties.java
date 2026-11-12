package com.shopmate.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型配置。留空 api-key 时后端自动切换到内置规则引擎（演示模式），
 * 保证没有密钥也能完整跑通工具调用链路。
 */
@ConfigurationProperties(prefix = "shopmate.llm")
public record LlmProperties(String baseUrl, String apiKey, String model, Double temperature) {

    public LlmProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        if (model == null || model.isBlank()) {
            model = "qwen-plus";
        }
        if (temperature == null) {
            temperature = 0.3;
        }
    }
}