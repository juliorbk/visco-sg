-- Stock levels: add a unique constraint on (product_id, warehouse_id) and
-- a version column for optimistic locking. The constraint name must match
-- the one referenced by the native UPSERT in StockLevelRepository
-- (uk_stock_levels_product_warehouse).

-- Step 1: deduplicate existing rows (keep the most recently updated).
-- If duplicates exist, this is destructive: only one row per
-- (product_id, warehouse_id) survives. Operators must run this
-- migration before deploying the new application code.
DELETE FROM stock_levels a USING stock_levels b
WHERE a.id < b.id
  AND a.product_id = b.product_id
  AND a.warehouse_id = b.warehouse_id;

-- Step 2: add the unique constraint if it does not already exist.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'uk_stock_levels_product_warehouse'
  ) THEN
    ALTER TABLE stock_levels
      ADD CONSTRAINT uk_stock_levels_product_warehouse
      UNIQUE (product_id, warehouse_id);
  END IF;
END$$;

-- Step 3: add the version column for optimistic locking if missing.
ALTER TABLE stock_levels
  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Step 4: drop the redundant non-unique index (the unique constraint
-- creates its own index).
DROP INDEX IF EXISTS idx_sl_product_warehouse;
