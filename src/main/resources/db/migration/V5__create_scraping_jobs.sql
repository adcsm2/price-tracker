CREATE TABLE scraping_jobs (
    id             BIGSERIAL PRIMARY KEY,
    source_id      BIGINT REFERENCES website_sources(id),
    search_keyword VARCHAR(255),
    category       VARCHAR(100),
    status         VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    items_found    INTEGER,
    error_message  TEXT,
    started_at     TIMESTAMP,
    completed_at   TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);
