package com.shopmate.agent.agent;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具调用轨迹发射器：LLM 模式与内置演示模式共用，
 * 保证两种模式下前端看到的工具面板完全一致。
 */
@Component
public class ToolTraceEmitter {

    private final ChatStreamRegistry registry;
    private final AtomicLong counter = new AtomicLong();

    public ToolTraceEmitter(ChatStreamRegistry registry) {
        this.registry = registry;
    }

    public String begin(String memoryId, String name, String label, Map<String, Object> args) {
        String id = "t" + counter.incrementAndGet();
        registry.get(memoryId).ifPresent(stream -> stream.toolStart(id, name, label, args));
        return id;
    }

    public void done(String memoryId, String id, String name, String summary, Object payload) {
        registry.get(memoryId).ifPresent(stream -> stream.toolDone(id, name, summary, payload));
    }

    public void fail(String memoryId, String id, String name, String message) {
        registry.get(memoryId).ifPresent(stream -> stream.toolFail(id, name, message));
    }
}