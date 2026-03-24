CREATE TABLE IF NOT EXISTS order_outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    publish_attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    last_attempt_at DATETIME(6) NULL,
    last_error VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_outbox_order_id (order_id)
);
