CREATE TABLE orders
(
    id          UUID         NOT NULL,
    customer_id UUID         NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE TABLE order_items
(
    id          UUID         NOT NULL,
    order_id    UUID         NOT NULL,
    line_number INTEGER      NOT NULL,
    product_id  UUID         NOT NULL,
    quantity    INTEGER      NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT uk_order_items_order_line UNIQUE (order_id, line_number),
    CONSTRAINT ck_order_items_line_number CHECK (line_number >= 0),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);

CREATE TABLE order_idempotency
(
    customer_id     UUID         NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    order_id        UUID         NOT NULL,
    CONSTRAINT pk_order_idempotency PRIMARY KEY (customer_id, idempotency_key),
    CONSTRAINT uk_order_idempotency_order UNIQUE (order_id),
    CONSTRAINT fk_order_idempotency_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_order_idempotency_key_nonblank CHECK (length(btrim(idempotency_key)) > 0)
);
