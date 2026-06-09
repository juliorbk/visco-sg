-- Enable trigram extension for fuzzy text search indexes
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- GIN trigram indexes for ILIKE '%term%' search on product text fields
CREATE INDEX IF NOT EXISTS idx_products_name_trgm
  ON products USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_sku_trgm
  ON products USING gin (sku gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_internal_code_trgm
  ON products USING gin (internal_code gin_trgm_ops);

-- Partial composite index to accelerate EXISTS subquery for hasStock filter
CREATE INDEX IF NOT EXISTS idx_sl_product_stock
  ON stock_levels (product_id, current_stock)
  WHERE current_stock > 0;
