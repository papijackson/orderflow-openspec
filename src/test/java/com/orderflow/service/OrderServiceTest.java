package com.orderflow.service;

import com.orderflow.entity.Order;
import com.orderflow.entity.OrderStatus;
import com.orderflow.exception.OrderNotFoundException;
import com.orderflow.repository.OrderIdempotencyRepository;
import com.orderflow.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderIdempotencyRepository orderIdempotencyRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderIdempotencyRepository = mock(OrderIdempotencyRepository.class);
        orderService = new OrderService(orderRepository, orderIdempotencyRepository);
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateOrderWithServerGeneratedValuesAndOrderedItems() {
        var customerId = UUID.randomUUID();
        var repeatedProductId = UUID.randomUUID();
        var before = Instant.now();

        var result = orderService.create(customerId, List.of(
                new OrderItemInput(repeatedProductId, 2),
                new OrderItemInput(repeatedProductId, 3)), null);

        var after = Instant.now();
        assertThat(result.id()).isNotNull();
        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.createdAt()).isBetween(before, after);
        assertThat(result.items())
                .containsExactly(
                        new OrderResult.Item(repeatedProductId, 2),
                        new OrderResult.Item(repeatedProductId, 3));
    }

    @Test
    void shouldCreateDistinctOrdersForRequestsWithoutAnIdempotencyKey() {
        var customerId = UUID.randomUUID();
        var items = List.of(new OrderItemInput(UUID.randomUUID(), 1));

        var first = orderService.create(customerId, items, null);
        var second = orderService.create(customerId, items, null);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void shouldRetrieveExistingOrderByUuid() {
        var order = new Order(UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        order.addItem(UUID.randomUUID(), 4);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        var result = orderService.get(order.getId());

        assertThat(result.id()).isEqualTo(order.getId());
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void shouldFailWhenOrderDoesNotExist() {
        var orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
