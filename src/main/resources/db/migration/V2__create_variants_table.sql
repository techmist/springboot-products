CREATE TABLE IF NOT EXISTS variants (
    id BIGSERIAL PRIMARY KEY,
    external_id BIGINT UNIQUE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    sku VARCHAR(255),
    price VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_variants_product_id ON variants(product_id);
