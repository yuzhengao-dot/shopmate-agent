package com.shopmate.agent.agent;

import com.shopmate.agent.repository.ToolCallRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具调用轨迹发射器：LLM 模式与内置演示模式共用，
 * 既把轨迹实时推给前端，也把每次调用写进 tool_call 表留档。
 */
@Component
public class ToolTraceEmitter {

    private final ChatStreamRegistry registry;
    private final ToolCallRepository repository;
    private final AtomicLong counter = new AtomicLong();
    private final Map<String, Long> startedAt = new ConcurrentHashMap<>();

    public ToolTraceEmitter(ChatStreamRegistry registry, ToolCallRepository repository) {
        this.registry = registry;
        this.repository = repository;
    }

    public String begin(String memoryId, String name, String label, Map<String, Object> args) {
        String id = "t" + counter.incrementAndGet();
        startedAt.put(id, System.currentTimeMillis());
        audit(() -> repository.start(id, memoryId, name, args));
        registry.get(memoryId).ifPresent(stream -> stream.toolStart(id, name, label, args));
        return id;
    }

    public void done(String memoryId, String id, String name, String summary, Object payload) {
        long cost = stop(id);
        audit(() -> repository.finish(id, "DONE", summary, cost));
        registry.get(memoryId).ifPresent(stream -> stream.toolDone(id, name, summary, payload, cost));
    }

    public void fail(String memoryId, String id, String name, String message) {
        long cost = stop(id);
        audit(() -> repository.finish(id, "ERROR", message, cost));
        registry.get(memoryId).ifPresent(stream -> stream.toolFail(id, name, message, cost));
    }

    private long stop(String id) {
        Long start = startedAt.remove(id);
        return start == null ? 0 : System.currentTimeMillis() - start;
    }

    /** 审计是旁路，写库失败不能打断正在进行的对话 */
    private void audit(Runnable action) {
        try {
            action.run();
        } catch (Exception ignored) {
            // 忽略审计异常
        }
    }
}