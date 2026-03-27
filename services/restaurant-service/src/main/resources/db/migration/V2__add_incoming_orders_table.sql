CREATE TABLE incoming_orders (
                                 id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 order_id         UUID NOT NULL UNIQUE,     -- ID from Order Service (no FK — cross-service)
                                 customer_id      UUID NOT NULL,             -- ID from Order Service (no FK — cross-service)
                                 restaurant_id    UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
                                 status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                                 total_amount     DECIMAL(10,2),
                                 delivery_address TEXT,
                                 notes            TEXT,
                                 created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
                                 updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incoming_orders_restaurant_id ON incoming_orders(restaurant_id);
CREATE INDEX idx_incoming_orders_status ON incoming_orders(status);
CREATE INDEX idx_incoming_orders_order_id ON incoming_orders(order_id);