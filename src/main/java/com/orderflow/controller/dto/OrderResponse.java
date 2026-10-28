package com.orderflow.controller.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        List<Item> items,
        String status,
        Instant createdAt) {

    public OrderResponse {
        items = List.copyOf(items);
    }

    public record Item(UUID productId, int quantity) {
    }
}
