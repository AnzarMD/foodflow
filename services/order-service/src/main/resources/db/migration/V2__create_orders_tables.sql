CREATE TABLE orders (
                        id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        customer_id      UUID NOT NULL REFERENCES users(id),
                        restaurant_id    UUID NOT NULL,                          -- NO FK — cross-service
                        restaurant_name  VARCHAR(255) NOT NULL,                  -- denormalized copy
                        status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                        total_amount     DECIMAL(10,2),
                        delivery_address TEXT,
                        created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_restaurant_id ON orders(restaurant_id);

CREATE TABLE order_items (
                             id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             order_id      UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             menu_item_id  UUID NOT NULL,                             -- NO FK — cross-service
                             name          VARCHAR(255) NOT NULL,                     -- denormalized copy
                             unit_price    DECIMAL(10,2) NOT NULL,
                             quantity      INT NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);