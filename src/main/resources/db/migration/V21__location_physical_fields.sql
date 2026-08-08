-- ─────────────────────────────────────────────────────────────
-- V21: Physical layout fields on locations (warehouse inventory map)
-- ─────────────────────────────────────────────────────────────
-- Adds structured physical coordinates to each location so the
-- warehouse can be rendered as a 2D map (zone / aisle / rack /
-- level / grid position). All columns are nullable to keep
-- existing rows valid; the existing location_code remains the
-- unique stable identifier per warehouse.

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS zone VARCHAR(64);

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS aisle VARCHAR(64);

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS rack VARCHAR(64);

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS level VARCHAR(32);

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS position_x INTEGER;

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS position_y INTEGER;

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS description VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_location_warehouse_zone
    ON locations (warehouse_id, zone);