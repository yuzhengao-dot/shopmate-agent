package com.shopmate.agent.web;

import com.shopmate.agent.model.ToolCallRecord;
import com.shopmate.agent.repository.ToolCallRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用审计查询：给调试和演示用，能看到每次调用的入参、耗时与状态。
 */
@RestController
@RequestMapping("/api")
public class ToolCallController {

    private final ToolCallRepository repository;

    public ToolCallController(ToolCallRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/tool-calls")
    public Map<String, Object> recent(@RequestParam(defaultValue = "20") int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        List<ToolCallRecord> items = repository.recent(size);

        long finished = items.stream().filter(item -> item.durationMs() > 0).count();
        double avg = items.stream()
                .filter(item -> item.durationMs() > 0)
                .mapToLong(ToolCallRecord::durationMs)
                .average()
                .orElse(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", items.size());
        result.put("finished", finished);
        result.put("avgDurationMs", Math.round(avg));
        result.put("items", items);
        return result;
    }
}