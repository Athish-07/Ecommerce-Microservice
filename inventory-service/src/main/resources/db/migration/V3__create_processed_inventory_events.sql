CREATE TABLE IF NOT EXISTS processed_inventory_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    success BIT NOT NULL,
    message VARCHAR(500) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_processed_inventory_events_order_id (order_id)
);
