CREATE TABLE IF NOT EXISTS bootstrap_state (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bootstrap_key VARCHAR(255) NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bootstrap_state_bootstrap_key (bootstrap_key)
);
