package com.orderflow.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderIdempotencyRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderIdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean reserve(UUID customerId, String idempotencyKey, UUID orderId) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");

        var insertedRows = jdbcTemplate.update("""
                INSERT INTO order_idempotency (customer_id, idempotency_key, order_id)
                VALUES (?, ?, ?)
                ON CONFLICT (customer_id, idempotency_key) DO NOTHING
                """, customerId, idempotencyKey, orderId);
        return insertedRows == 1;
    }

    public Optional<UUID> findOrderId(UUID customerId, String idempotencyKey) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");

        return jdbcTemplate.query("""
                        SELECT order_id
                        FROM order_idempotency
                        WHERE customer_id = ? AND idempotency_key = ?
                        """,
                resultSet -> resultSet.next()
                        ? Optional.of(resultSet.getObject("order_id", UUID.class))
                        : Optional.empty(),
                customerId,
                idempotencyKey);
    }
}
