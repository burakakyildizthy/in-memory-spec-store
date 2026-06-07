-- Insert test users
INSERT INTO users (id, name, email, status)
VALUES (1, 'John Doe', 'john@example.com', 'ACTIVE'),
       (2, 'Jane Smith', 'jane@example.com', 'ACTIVE'),
       (3, 'Bob Johnson', 'bob@example.com', 'INACTIVE'),
       (4, 'Alice Brown', 'alice@example.com', 'ACTIVE'),
       (5, 'Charlie Wilson', 'charlie@example.com', 'ACTIVE');

-- Insert test orders
INSERT INTO orders (id, user_id, order_number, total_amount, status)
VALUES (1, 1, 'ORD-001', 150.00, 'COMPLETED'),
       (2, 1, 'ORD-002', 75.50, 'PENDING'),
       (3, 2, 'ORD-003', 200.00, 'COMPLETED'),
       (4, 2, 'ORD-004', 125.75, 'SHIPPED'),
       (5, 4, 'ORD-005', 300.00, 'COMPLETED'),
       (6, 4, 'ORD-006', 89.99, 'PENDING');

-- Insert test order items
INSERT INTO order_items (id, order_id, product_name, quantity, unit_price)
VALUES (1, 1, 'Laptop', 1, 100.00),
       (2, 1, 'Mouse', 2, 25.00),
       (3, 2, 'Keyboard', 1, 75.50),
       (4, 3, 'Monitor', 1, 200.00),
       (5, 4, 'Headphones', 1, 125.75),
       (6, 5, 'Tablet', 2, 150.00),
       (7, 6, 'Phone Case', 3, 29.99);

-- Insert test events
INSERT INTO events (id, user_id, event_type, event_data)
VALUES (1, 1, 'LOGIN', '{"ip": "192.168.1.1", "browser": "Chrome"}'),
       (2, 1, 'PURCHASE', '{"order_id": 1, "amount": 150.00}'),
       (3, 1, 'LOGOUT', '{"session_duration": 3600}'),
       (4, 2, 'LOGIN', '{"ip": "192.168.1.2", "browser": "Firefox"}'),
       (5, 2, 'PURCHASE', '{"order_id": 3, "amount": 200.00}'),
       (6, 3, 'LOGIN', '{"ip": "192.168.1.3", "browser": "Safari"}'),
       (7, 4, 'LOGIN', '{"ip": "192.168.1.4", "browser": "Chrome"}'),
       (8, 4, 'PURCHASE', '{"order_id": 5, "amount": 300.00}');

-- Insert test metrics for aggregation testing
INSERT INTO user_metrics (id, user_id, metric_name, metric_value)
VALUES (1, 1, 'page_views', 150.0),
       (2, 1, 'session_time', 3600.0),
       (3, 1, 'clicks', 45.0),
       (4, 2, 'page_views', 200.0),
       (5, 2, 'session_time', 4800.0),
       (6, 2, 'clicks', 67.0),
       (7, 3, 'page_views', 50.0),
       (8, 3, 'session_time', 1200.0),
       (9, 4, 'page_views', 300.0),
       (10, 4, 'session_time', 7200.0),
       (11, 4, 'clicks', 89.0);