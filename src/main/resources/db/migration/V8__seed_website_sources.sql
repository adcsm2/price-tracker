-- Seed the supported scraper sources.
-- These are fixed configuration data, not user data.
INSERT INTO website_sources (name, base_url, scraper_type, status)
VALUES
    ('Amazon ES',     'https://www.amazon.es',     'AMAZON',     'ACTIVE'),
    ('MediaMarkt ES', 'https://www.mediamarkt.es', 'MEDIAMARKT', 'ACTIVE');
