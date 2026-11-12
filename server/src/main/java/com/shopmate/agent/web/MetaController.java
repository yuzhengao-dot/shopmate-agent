package com.shopmate.agent.web;

import com.shopmate.agent.agent.AgentFactory;
import com.shopmate.agent.repository.OrderRepository;
import com.shopmate.agent.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 前端初始化所需的元信息 */
@RestController
@RequestMapping("/api")
public class MetaController {

    private final AgentFactory agentFactory;
    private final ProductRepository products;
    private final OrderRepository orders;

    public MetaController(AgentFactory agentFactory, ProductRepository products, OrderRepository orders) {
        this.agentFactory = agentFactory;
        this.products = products;
        this.orders = orders;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", agentFactory.mode());
        data.put("model", agentFactory.modelName());
        data.put("user", Map.of("id", "U1001", "name", "林晓"));
        data.put("suggestions", List.of(
                "预算 3000 元以内，推荐一款拍照好的手机",
                "我的订单到哪了",
                "降噪耳机有什么推荐",
                "我想退掉 SM20261028002 这单"));
        data.put("categories", products.categories());
        data.put("productCount", products.findAll().size());
        data.put("orderCount", orders.findByUser("U1001", "ALL").size());
        return data;
    }
}