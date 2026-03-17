CREATE TABLE IF NOT EXISTS orders (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(64)     NOT NULL,
    product_id  VARCHAR(64)     NOT NULL,
    amount      DECIMAL(10, 2)  NOT NULL,
    status      VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status  ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at DESC);

INSERT INTO orders (user_id, product_id, amount, status)
SELECT
    'user-' || (random()*1000)::int,
    'product-' || (random()*100)::int,
    (random()*1000)::numeric(10,2),
    (ARRAY['PENDING','CONFIRMED','SHIPPED','DELIVERED'])[floor(random()*4+1)]
FROM generate_series(1, 1000);
