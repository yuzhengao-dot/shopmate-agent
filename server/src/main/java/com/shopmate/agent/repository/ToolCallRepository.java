package com.shopmate.agent.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmate.agent.model.ToolCallRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Repository
public class ToolCallRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final RowMapper<ToolCallRecord> MAPPER = (rs, rowNum) -> new ToolCallRecord(
            rs.getString("trace_id"),
            rs.getString("session_id"),
            rs.getString("tool_name"),
            rs.getString("status"),
            rs.getString("args_json"),
            rs.getString("summary"),
            rs.getLong("duration_ms"),
            rs.getString("created_at"));

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public ToolCallRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 调用开始时先落一行 RUNNING，结束后再补耗时与结果，未完成的调用也留得下痕迹 */
    public void start(String traceId, String sessionId, String toolName, Map<String, Object> args) {
        String argsJson;
        try {
            argsJson = mapper.writeValueAsString(args == null ? Map.of() : args);
        } catch (Exception e) {
            argsJson = "{}";
        }
        if (argsJson.length() > 600) {
            argsJson = argsJson.substring(0, 600);
        }
        jdbc.update("""
                        INSERT INTO tool_call (trace_id, session_id, tool_name, status, args_json, created_at)
                        VALUES (?, ?, ?, ?, ?, ?)""",
                traceId, sessionId, toolName, "RUNNING", argsJson, LocalDateTime.now().format(TS));
    }

    public void finish(String traceId, String status, String summary, long durationMs) {
        String text = summary == null ? "" : summary;
        if (text.length() > 300) {
            text = text.substring(0, 300);
        }
        jdbc.update("UPDATE tool_call SET status = ?, summary = ?, duration_ms = ? WHERE trace_id = ?",
                status, text, durationMs, traceId);
    }

    public List<ToolCallRecord> recent(int limit) {
        return jdbc.query("SELECT * FROM tool_call ORDER BY id DESC LIMIT ?", MAPPER, limit);
    }
}