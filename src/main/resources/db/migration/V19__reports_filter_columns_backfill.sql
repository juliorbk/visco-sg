-- V19: Defensive backfill of the dedicated filter columns on `reports`.
-- V18 (commit 28871a9) added warehouse_id, category_id, and search but
-- some production databases never received the migration: querying
-- Report rows raised
--   ERROR: column r1_0.category_id does not exist
-- because the entity declares the field and Hibernate tries to read it.
-- This migration is idempotent (PostgreSQL 9.6+ supports
-- `ADD COLUMN IF NOT EXISTS`) and safe to run on databases where V18
-- already applied.

ALTER TABLE reports
    ADD COLUMN IF NOT EXISTS warehouse_id BIGINT,
    ADD COLUMN IF NOT EXISTS category_id  BIGINT,
    ADD COLUMN IF NOT EXISTS search       VARCHAR(255);

-- Index from V18 (idempotent guard in case V18 was partially applied).
CREATE INDEX IF NOT EXISTS idx_reports_warehouse_id ON reports(warehouse_id);
