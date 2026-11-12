package com.shopmate.agent.agent;

import com.shopmate.agent.config.LlmProperties;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Agent 工厂：有 api-key 时构建 LangChain4j AI Service；
 * 没有密钥时退回内置规则引擎，界面与工具轨迹保持一致。
 */
@Component
public class AgentFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentFactory.class);

    private final LlmProperties properties;
    private final ShopTools tools;

    private ShopAssistant assistant;
    private String mode = "demo";
    private String modelName = "rule-engine";

    public AgentFactory(LlmProperties properties, ShopTools tools) {
        this.properties = properties;
        this.tools = tools;
    }

    @PostConstruct
    void init() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.warn("未配置 shopmate.llm.api-key，已启用内置规则引擎（演示模式）。");
            return;
        }
        try {
            StreamingChatModel model = OpenAiStreamingChatModel.builder()
                    .baseUrl(trimSlash(properties.baseUrl()))
                    .apiKey(properties.apiKey())
                    .modelName(properties.model())
                    .temperature(properties.temperature())
                    .build();

            this.assistant = AiServices.builder(ShopAssistant.class)
                    .streamingChatModel(model)
                    .tools(tools)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                    .build();
            this.mode = "llm";
            this.modelName = properties.model();
            log.info("已启用 LangChain4j Agent，模型：{}，网关：{}", properties.model(), properties.baseUrl());
        } catch (Exception e) {
            log.error("初始化大模型失败，退回演示模式：{}", e.getMessage());
        }
    }

    public Optional<ShopAssistant> assistant() {
        return Optional.ofNullable(assistant);
    }

    public String mode() {
        return mode;
    }

    public String modelName() {
        return modelName;
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}