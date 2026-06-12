-- ─────────────────────────────────────────────────────────────
-- V13: Per-item storage location on goods receipt items
-- ─────────────────────────────────────────────────────────────
-- Each receipt item can now record the specific bin/aisle where the
-- received quantity was placed inside the destination warehouse.
-- The column is nullable to keep existing rows valid; the API allows
-- both a per-item and a receipt-level default locationId.

ALTER TABLE good_receipt_items
    ADD COLUMN IF NOT EXISTS location_id BIGINT;

ALTER TABLE good_receipt_items
    ADD CONSTRAINT fk_gri_location
    FOREIGN KEY (location_id) REFERENCES locations (id)
    NOT VALID;

ALTER TABLE good_receipt_items VALIDATE CONSTRAINT fk_gri_location;

CREATE INDEX IF NOT EXISTS idx_gri_location
    ON good_receipt_items (location_id);
