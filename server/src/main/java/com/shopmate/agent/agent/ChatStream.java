package com.shopmate.agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次对话的 SSE 输出通道。工具调用、token、卡片数据都从这里推给前端。
 */
public class ChatStream {

    private final String sessionId;
    private final SseEmitter emitter;
    private final ObjectMapper mapper;
    private final Object lock = new Object();
    private volatile boolean closed;

    public ChatStream(String sessionId, SseEmitter emitter, ObjectMapper mapper) {
        this.sessionId = sessionId;
        this.emitter = emitter;
        this.mapper = mapper;
    }

    public String sessionId() {
        return sessionId;
    }

    public void send(String event, Object data) {
        synchronized (lock) {
            if (closed) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name(event).data(mapper.writeValueAsString(data)));
            } catch (Exception e) {
                closed = true;
            }
        }
    }

    public void meta(String mode, String model) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("mode", mode);
        data.put("model", model);
        send("meta", data);
    }

    public void token(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        send("token", Map.of("text", text));
    }

    public void toolStart(String id, String name, String label, Map<String, Object> args) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("label", label);
        data.put("status", "running");
        data.put("args", args);
        send("tool", data);
    }

    public void toolDone(String id, String name, String summary, Object payload, long durationMs) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("status", "done");
        data.put("summary", summary);
        data.put("durationMs", durationMs);
        if (payload != null) {
            data.put("payload", payload);
        }
        send("tool", data);
    }

    public void toolFail(String id, String name, String message, long durationMs) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("status", "error");
        data.put("summary", message);
        data.put("durationMs", durationMs);
        send("tool", data);
    }

    public void error(String message) {
        send("error", Map.of("message", message == null ? "未知错误" : message));
    }

    public void done() {
        send("done", Map.of("sessionId", sessionId));
    }

    public void close() {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 客户端断开时忽略
            }
        }
    }
}