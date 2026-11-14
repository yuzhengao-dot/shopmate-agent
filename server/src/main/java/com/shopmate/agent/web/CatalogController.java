package com.shopmate.agent.web;

import com.shopmate.agent.model.AfterSaleTicket;
import com.shopmate.agent.model.Order;
import com.shopmate.agent.model.Product;
import com.shopmate.agent.repository.OrderRepository;
import com.shopmate.agent.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final ProductRepository products;
    private final OrderRepository orders;

    public CatalogController(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @GetMapping("/products")
    public List<Product> products(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String category) {
        return products.search(keyword, category, null, null, 50);
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在"));
    }

    @GetMapping("/orders")
    public List<Order> orders(@RequestParam(required = false, defaultValue = "ALL") String status) {
        return orders.findByUser("U1001", status);
    }

    @GetMapping("/orders/{orderNo}")
    public Order order(@PathVariable String orderNo) {
        return orders.findByNo(orderNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
    }

    /** 售后建单：Agent 只生成草稿，用户在前端确认后走这里真正写库 */
    @PostMapping("/orders/{orderNo}/after-sale")
    public AfterSaleTicket afterSale(@PathVariable String orderNo,
                                     @RequestBody(required = false) AfterSaleRequest request) {
        String type = request == null ? null : request.type();
        String reason = request == null ? null : request.reason();
        return orders.createTicket(orderNo, type, reason);
    }

    public record AfterSaleRequest(String type, String reason) {
    }
}