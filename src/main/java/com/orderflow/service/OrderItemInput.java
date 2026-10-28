package com.orderflow.service;

import java.util.UUID;

public record OrderItemInput(UUID productId, int quantity) {
}
