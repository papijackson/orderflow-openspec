package com.orderflow.controller;

import com.orderflow.controller.dto.CreateOrderRequest;
import com.orderflow.controller.dto.OrderResponse;
import com.orderflow.service.OrderItemInput;
import com.orderflow.service.OrderResult;
import com.orderflow.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false)
            @Pattern(regexp = ".*\\S.*") @Size(max = 128) String idempotencyKey) {
        var items = request.items().stream()
                .map(item -> new OrderItemInput(item.productId(), item.quantity()))
                .toList();
        var result = orderService.create(
                request.customerId(),
                items,
                idempotencyKey);

        return ResponseEntity.created(URI.create("/api/v1/orders/" + result.id()))
                .body(toResponse(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(orderService.get(id)));
    }

    private static OrderResponse toResponse(OrderResult result) {
        var items = result.items().stream()
                .map(item -> new OrderResponse.Item(item.productId(), item.quantity()))
                .toList();

        return new OrderResponse(
                result.id(),
                result.customerId(),
                items,
                result.status().name(),
                result.createdAt());
    }
}
