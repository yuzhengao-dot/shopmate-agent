package com.shopmate.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmate.agent.agent.AgentFactory;
import com.shopmate.agent.agent.ChatStream;
import com.shopmate.agent.agent.ChatStreamRegistry;
import com.shopmate.agent.agent.DemoAgent;
import com.shopmate.agent.agent.ShopAssistant;
import com.shopmate.agent.web.ChatRequest;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** 对话编排：初始化 SSE -> 调用 Agent -> 推送 token / 工具轨迹 / 结束事件 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final AgentFactory agentFactory;
    private final DemoAgent demoAgent;
    private final ChatStreamRegistry registry;
    private final ObjectMapper mapper;
    private final ExecutorService executor;

    public ChatService(AgentFactory agentFactory, DemoAgent demoAgent,
                       ChatStreamRegistry registry, ObjectMapper mapper) {
        this.agentFactory = agentFactory;
        this.demoAgent = demoAgent;
        this.registry = registry;
        this.mapper = mapper;
        AtomicInteger seq = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(16, runnable -> {
            Thread thread = new Thread(runnable, "shopmate-chat-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public SseEmitter chat(ChatRequest request) {
        String sessionId = (request.sessionId() == null || request.sessionId().isBlank())
                ? UUID.randomUUID().toString()
                : request.sessionId();

        SseEmitter emitter = new SseEmitter(0L);
        ChatStream stream = registry.open(sessionId, emitter, mapper);
        emitter.onCompletion(() -> registry.remove(sessionId, stream));
        emitter.onTimeout(() -> {
            registry.remove(sessionId, stream);
            stream.close();
        });
        emitter.onError(error -> registry.remove(sessionId, stream));

        stream.meta(agentFactory.mode(), agentFactory.modelName());

        executor.execute(() -> {
            try {
                Optional<ShopAssistant> assistant = agentFactory.assistant();
                if (assistant.isPresent()) {
                    assistant.get().chat(sessionId, request.message())
                            .onPartialResponse(stream::token)
                            .onError(error -> {
                                log.warn("模型调用失败: {}", error.getMessage());
                                stream.error("模型调用失败：" + rootMessage(error));
                            })
                            .start();
                } else {
                    demoAgent.reply(sessionId, request.message(), stream);
                }
            } catch (Exception e) {
                log.error("对话处理失败", e);
                stream.error("处理失败：" + rootMessage(e));
            } finally {
                stream.done();
                stream.close();
                registry.remove(sessionId, stream);
            }
        });

        return emitter;
    }

    private static String rootMessage(Throwable error) {
        Throwable cursor = error;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}