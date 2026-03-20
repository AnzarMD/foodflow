CREATE TYPE user_role AS ENUM ('CUSTOMER', 'RESTAURANT_OWNER', 'DELIVERY_AGENT', 'ADMIN');

CREATE TABLE users (
                       id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email       VARCHAR(255) NOT NULL UNIQUE,
                       password    VARCHAR(255) NOT NULL,
                       full_name   VARCHAR(255) NOT NULL,
                       phone       VARCHAR(20),
                       role        user_role NOT NULL DEFAULT 'CUSTOMER',
                       created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_tokens (
                                id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token       VARCHAR(512) NOT NULL UNIQUE,
                                expires_at  TIMESTAMP NOT NULL,
                                revoked     BOOLEAN NOT NULL DEFAULT false,
                                created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);