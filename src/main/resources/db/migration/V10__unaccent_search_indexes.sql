-- ─────────────────────────────────────────────────────────────
-- V10: Fast accent-insensitive product search
-- ─────────────────────────────────────────────────────────────
-- The product search bars use `unaccent(col) ILIKE unaccent('%term%')`.
-- The trigram GIN indexes added in V9 cover the raw columns, but a
-- function call on the left-hand side of ILIKE prevents Postgres from
-- using them — so the search degrades to a full table scan.
--
-- These functional trigram indexes let the planner use the GIN index
-- on `unaccent(col)`, which is what the queries actually evaluate.

CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE INDEX IF NOT EXISTS idx_products_name_unaccent_trgm
  ON products USING GIN (unaccent(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_sku_unaccent_trgm
  ON products USING GIN (unaccent(sku) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_internal_code_unaccent_trgm
  ON products USING GIN (unaccent(internal_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_sap_code_unaccent_trgm
  ON products USING GIN (unaccent(sap_code) gin_trgm_ops);
