CREATE TABLE product_listings (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES products(id),
    source_id       BIGINT       NOT NULL REFERENCES website_sources(id),
    external_id     VARCHAR(255),
    url             VARCHAR(512) NOT NULL,
    current_price   NUMERIC(10, 2),
    currency        VARCHAR(10)  DEFAULT 'EUR',
    in_stock        BOOLEAN,
    last_scraped_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_source UNIQUE (product_id, source_id)
);
