package com.shopmate.agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 会话 -> SSE 通道，供工具方法按 memoryId 回推调用轨迹 */
@Component
public class ChatStreamRegistry {

    private final Map<String, ChatStream> active = new ConcurrentHashMap<>();

    public ChatStream open(String sessionId, SseEmitter emitter, ObjectMapper mapper) {
        ChatStream stream = new ChatStream(sessionId, emitter, mapper);
        ChatStream previous = active.put(sessionId, stream);
        if (previous != null) {
            previous.close();
        }
        return stream;
    }

    public Optional<ChatStream> get(String sessionId) {
        return Optional.ofNullable(sessionId).map(active::get);
    }

    public void remove(String sessionId, ChatStream stream) {
        active.remove(sessionId, stream);
    }
}