CREATE TABLE price_history (
    id         BIGSERIAL PRIMARY KEY,
    listing_id BIGINT        NOT NULL REFERENCES product_listings(id),
    product_id BIGINT        NOT NULL REFERENCES products(id),
    price      NUMERIC(10,2) NOT NULL,
    in_stock   BOOLEAN,
    scraped_at TIMESTAMP     NOT NULL
);

CREATE INDEX idx_listing_scraped ON price_history (listing_id, scraped_at);
