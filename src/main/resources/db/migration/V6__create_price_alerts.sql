CREATE TABLE price_alerts (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    user_email   VARCHAR(255)  NOT NULL,
    target_price NUMERIC(10,2) NOT NULL,
    status       VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE',
    triggered_at TIMESTAMP,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);
