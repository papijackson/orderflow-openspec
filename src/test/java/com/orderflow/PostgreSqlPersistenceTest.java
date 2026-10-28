package com.orderflow;

import com.orderflow.entity.Order;
import com.orderflow.entity.OrderItem;
import com.orderflow.exception.IdempotencyConflictException;
import com.orderflow.repository.OrderIdempotencyRepository;
import com.orderflow.repository.OrderRepository;
import com.orderflow.service.OrderItemInput;
import com.orderflow.service.OrderResult;
import com.orderflow.service.OrderService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class PostgreSqlPersistenceTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderIdempotencyRepository orderIdempotencyRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void shouldInitializeFlywayAndValidateTheOrderSchema() {
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.info().pending()).isEmpty();

        var tableNames = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'",
                String.class);

        assertThat(tableNames)
                .contains("flyway_schema_history", "orders", "order_items", "order_idempotency");
    }

    @Test
    @Transactional
    void shouldPersistDuplicateItemsInTheirOriginalOrder() {
        var productId = UUID.randomUUID();
        var order = new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-10-28T10:00:00Z"));
        order.addItem(productId, 2);
        order.addItem(productId, 3);

        entityManager.persist(order);
        entityManager.flush();
        var orderId = order.getId();
        entityManager.clear();

        var restoredOrder = entityManager.find(Order.class, orderId);

        assertThat(restoredOrder.getItems())
                .extracting(OrderItem::getProductId)
                .containsExactly(productId, productId);
        assertThat(restoredOrder.getItems())
                .extracting(OrderItem::getQuantity)
                .containsExactly(2, 3);
    }

    @Test
    void shouldRejectNonPositivePersistedItemQuantity() {
        var orderId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO orders (id, customer_id, status, created_at) VALUES (?, ?, ?, ?)",
                orderId,
                UUID.randomUUID(),
                "CREATED",
                Timestamp.from(Instant.now()));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO order_items (id, order_id, line_number, product_id, quantity) "
                        + "VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                orderId,
                0,
                UUID.randomUUID(),
                0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldReserveIdempotencyKeysOncePerCustomer() {
        var firstCustomerId = UUID.randomUUID();
        var secondCustomerId = UUID.randomUUID();
        var firstOrderId = UUID.randomUUID();
        var secondOrderId = UUID.randomUUID();
        var key = "submission-1";

        transactionTemplate.executeWithoutResult(status -> {
            entityManager.persist(new Order(firstOrderId, firstCustomerId, Instant.now()));
            entityManager.persist(new Order(secondOrderId, secondCustomerId, Instant.now()));
            entityManager.flush();

            assertThat(orderIdempotencyRepository.reserve(firstCustomerId, key, firstOrderId)).isTrue();
            assertThat(orderIdempotencyRepository.reserve(firstCustomerId, key, UUID.randomUUID())).isFalse();
            assertThat(orderIdempotencyRepository.findOrderId(firstCustomerId, key))
                    .contains(firstOrderId);
            assertThat(orderIdempotencyRepository.reserve(secondCustomerId, key, secondOrderId)).isTrue();
        });
    }

    @Test
    void shouldReplayIdenticalRequestWithoutCreatingAnotherOrder() {
        var customerId = UUID.randomUUID();
        var items = List.of(new OrderItemInput(UUID.randomUUID(), 2));
        var idempotencyKey = "retry-" + UUID.randomUUID();
        var initialOrderCount = orderRepository.count();

        var created = orderService.create(customerId, items, idempotencyKey);
        var replay = orderService.create(customerId, items, idempotencyKey);

        assertThat(replay).isEqualTo(created);
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount + 1);
    }

    @Test
    void shouldRejectChangedDataForAnExistingKeyWithoutCreatingAnotherOrder() {
        var customerId = UUID.randomUUID();
        var idempotencyKey = "conflict-" + UUID.randomUUID();
        var productId = UUID.randomUUID();
        var initialOrderCount = orderRepository.count();
        orderService.create(customerId, List.of(new OrderItemInput(productId, 2)), idempotencyKey);

        assertThatThrownBy(() -> orderService.create(
                customerId,
                List.of(new OrderItemInput(productId, 3)),
                idempotencyKey))
                .isInstanceOf(IdempotencyConflictException.class);

        assertThat(orderRepository.count()).isEqualTo(initialOrderCount + 1);
    }

    @Test
    void shouldScopeIdenticalKeysByCustomer() {
        var firstCustomerId = UUID.randomUUID();
        var secondCustomerId = UUID.randomUUID();
        var items = List.of(new OrderItemInput(UUID.randomUUID(), 1));
        var idempotencyKey = "customer-scope-" + UUID.randomUUID();
        var initialOrderCount = orderRepository.count();

        var first = orderService.create(firstCustomerId, items, idempotencyKey);
        var second = orderService.create(secondCustomerId, items, idempotencyKey);

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount + 2);
    }

    @Test
    void shouldCreateOnlyOneOrderForConcurrentIdenticalRequests() throws Exception {
        var customerId = UUID.randomUUID();
        var items = List.of(new OrderItemInput(UUID.randomUUID(), 1));
        var idempotencyKey = "concurrent-" + UUID.randomUUID();
        var initialOrderCount = orderRepository.count();
        var startGate = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var firstRequest = executor.submit(() -> {
                startGate.await();
                return orderService.create(customerId, items, idempotencyKey);
            });
            var secondRequest = executor.submit(() -> {
                startGate.await();
                return orderService.create(customerId, items, idempotencyKey);
            });
            startGate.countDown();

            OrderResult first = firstRequest.get(20, TimeUnit.SECONDS);
            OrderResult second = secondRequest.get(20, TimeUnit.SECONDS);

            assertThat(second).isEqualTo(first);
            assertThat(orderRepository.count()).isEqualTo(initialOrderCount + 1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRollbackIdempotencyReservationWhenOrderItemPersistenceFails() {
        var customerId = UUID.randomUUID();
        var idempotencyKey = "rollback-" + UUID.randomUUID();
        var initialOrderCount = orderRepository.count();
        jdbcTemplate.execute(
                "ALTER TABLE order_items ADD CONSTRAINT ck_test_force_order_item_failure "
                        + "CHECK (quantity <> 2) NOT VALID");

        try {
            assertThatThrownBy(() -> orderService.create(
                    customerId,
                    List.of(new OrderItemInput(UUID.randomUUID(), 2)),
                    idempotencyKey))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.execute("ALTER TABLE order_items DROP CONSTRAINT ck_test_force_order_item_failure");
        }

        assertThat(orderIdempotencyRepository.findOrderId(customerId, idempotencyKey)).isEmpty();
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount);
    }
}
