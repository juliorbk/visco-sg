-- ─────────────────────────────────────────────────────────────
-- V14: RIF / Tax ID on suppliers
-- ─────────────────────────────────────────────────────────────
-- Stores the supplier's tax identification (RIF in Venezuela,
-- RFC in Mexico, EIN in the US, etc.). Nullable to keep existing
-- rows valid; the supplier modal makes it a required field for
-- new suppliers going forward.

ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS tax_id VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_supplier_tax_id
    ON suppliers (tax_id);
