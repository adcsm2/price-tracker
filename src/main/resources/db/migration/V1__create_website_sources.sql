CREATE TABLE website_sources (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    base_url        VARCHAR(255) NOT NULL,
    scraper_type    VARCHAR(50)  NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    last_scraped_at TIMESTAMP,
    successful_scrapes INTEGER DEFAULT 0,
    failed_scrapes     INTEGER DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
