CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_email VARCHAR(255) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    total_amount FLOAT(53) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);
