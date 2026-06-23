-- V22: ensure the unique_sap_code constraint on products(sap_code) exists.
--
-- Production has the constraint (per the live DB), but the Product
-- entity did not declare @Column(unique=true) for sap_code, so
-- `ddl-auto=update` would not generate it on a fresh dev DB. A
-- developer creating a product with a duplicate SAP code on a
-- database missing the constraint would pass the service-level SKU
-- check and only blow up at INSERT time with a generic
--   ERROR: duplicate key value violates unique constraint
-- The DB constraint is the last line of defence; this migration
-- guarantees it is in place.

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'unique_sap_code'
      AND conrelid = 'public.products'::regclass
  ) THEN
    -- Drop any pre-existing non-unique indexes on sap_code that
    -- would conflict with the implicit index the unique constraint
    -- creates. The functional GIN trigram index in V10 and the
    -- B-tree in V12 are NOT on plain sap_code and are kept.
    ALTER TABLE products
      ADD CONSTRAINT unique_sap_code UNIQUE (sap_code);
  END IF;
END $$;
