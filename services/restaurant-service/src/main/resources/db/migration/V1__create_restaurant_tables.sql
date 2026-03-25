CREATE TABLE restaurants (
                             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             owner_id        UUID NOT NULL,
                             name            VARCHAR(255) NOT NULL,
                             address         TEXT,
                             cuisine_type    VARCHAR(100),
                             description     TEXT,
                             is_active       BOOLEAN NOT NULL DEFAULT true,
                             average_rating  DECIMAL(3,2) DEFAULT 0.0,
                             created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                             updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_restaurants_owner_id ON restaurants(owner_id);
CREATE INDEX idx_restaurants_is_active ON restaurants(is_active);

CREATE TABLE menu_items (
                            id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            restaurant_id   UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
                            name            VARCHAR(255) NOT NULL,
                            description     TEXT,
                            price           DECIMAL(10,2) NOT NULL,
                            category        VARCHAR(100),
                            is_available    BOOLEAN NOT NULL DEFAULT true,
                            created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_menu_items_restaurant_id ON menu_items(restaurant_id);
CREATE INDEX idx_menu_items_is_available ON menu_items(is_available);