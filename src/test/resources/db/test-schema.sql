-- Test schema for DatabaseDataSource tests

CREATE TABLE IF NOT EXISTS test_entities (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN,
    created_at TIMESTAMP
);

CREATE INDEX idx_test_entities_name ON test_entities(name);
CREATE INDEX idx_test_entities_active ON test_entities(active);
