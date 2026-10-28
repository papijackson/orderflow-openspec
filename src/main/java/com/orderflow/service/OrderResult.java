package com.orderflow.service;

import com.orderflow.entity.Order;
import com.orderflow.entity.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResult(
        UUID id,
        UUID customerId,
        List<Item> items,
        OrderStatus status,
        Instant createdAt) {

    public OrderResult {
        items = List.copyOf(items);
    }

    static OrderResult from(Order order) {
        var items = order.getItems().stream()
                .map(item -> new Item(item.getProductId(), item.getQuantity()))
                .toList();

        return new OrderResult(
                order.getId(),
                order.getCustomerId(),
                items,
                order.getStatus(),
                order.getCreatedAt());
    }

    public record Item(UUID productId, int quantity) {
    }
}
