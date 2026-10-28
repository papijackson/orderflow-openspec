package com.orderflow.service;

import com.orderflow.entity.Order;
import com.orderflow.exception.IdempotencyConflictException;
import com.orderflow.exception.OrderNotFoundException;
import com.orderflow.repository.OrderIdempotencyRepository;
import com.orderflow.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderIdempotencyRepository orderIdempotencyRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderIdempotencyRepository orderIdempotencyRepository) {
        this.orderRepository = orderRepository;
        this.orderIdempotencyRepository = orderIdempotencyRepository;
    }

    @Transactional
    public OrderResult create(UUID customerId, List<OrderItemInput> items, String idempotencyKey) {
        var orderId = UUID.randomUUID();

        if (idempotencyKey != null
                && !orderIdempotencyRepository.reserve(customerId, idempotencyKey, orderId)) {
            return replayOrReject(customerId, items, idempotencyKey);
        }

        var order = new Order(
                orderId,
                customerId,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        items.forEach(item -> order.addItem(item.productId(), item.quantity()));

        return OrderResult.from(orderRepository.saveAndFlush(order));
    }

    @Transactional(readOnly = true)
    public OrderResult get(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResult::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private OrderResult replayOrReject(
            UUID customerId,
            List<OrderItemInput> requestItems,
            String idempotencyKey) {
        var existingOrderId = orderIdempotencyRepository.findOrderId(customerId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Reserved order was not found"));
        var existingOrder = orderRepository.findById(existingOrderId)
                .map(OrderResult::from)
                .orElseThrow(() -> new IllegalStateException("Reserved order was not found"));
        var requestedItems = requestItems.stream()
                .map(item -> new OrderResult.Item(item.productId(), item.quantity()))
                .toList();

        if (!existingOrder.items().equals(requestedItems)) {
            throw new IdempotencyConflictException();
        }

        return existingOrder;
    }
}
