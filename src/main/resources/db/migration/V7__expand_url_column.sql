-- Amazon URLs include long tracking parameters that exceed VARCHAR(512).
-- Expand to TEXT to accommodate any URL length.
ALTER TABLE product_listings ALTER COLUMN url TYPE TEXT;
