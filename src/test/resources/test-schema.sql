-- Users table (primary entities)
CREATE TABLE users
(
    id         BIGINT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    status     VARCHAR(50) DEFAULT 'ACTIVE'
);

-- Orders table (secondary entities with foreign key to users)
CREATE TABLE orders
(
    id           BIGINT PRIMARY KEY,
    user_id      BIGINT         NOT NULL,
    order_number VARCHAR(100)   NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    order_date   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    status       VARCHAR(50) DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Order items table (tertiary entities with foreign key to orders)
CREATE TABLE order_items
(
    id           BIGINT PRIMARY KEY,
    order_id     BIGINT         NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INTEGER        NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- Events table (secondary entities with foreign key to users)
CREATE TABLE events
(
    id         BIGINT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Aggregation test table (for testing COUNT, SUM, AVG operations)
CREATE TABLE user_metrics
(
    id           BIGINT PRIMARY KEY,
    user_id      BIGINT         NOT NULL,
    metric_name  VARCHAR(100)   NOT NULL,
    metric_value DECIMAL(10, 2) NOT NULL,
    recorded_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id)
);