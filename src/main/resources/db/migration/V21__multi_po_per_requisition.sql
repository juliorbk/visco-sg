-- Allow multiple purchase orders per requisition.
--
-- 1. Extend the requisition status CHECK constraint to admit the new
--    PARTIALLY_CONVERTED value (awarded by at least one PO but not all
--    items are fully awarded yet).
--
-- 2. Add a nullable requisition_item_id FK to purchase_order_items so each
--    PO line item can be linked back to the specific requisition line it
--    is fulfilling. This enables per-line award tracking, over-award
--    validation, and accurate per-requisition reporting.
--
-- The migration is idempotent where possible and safe to run on databases
-- that already contain approved / partially converted requisitions.

-- 1.1 Drop the old status CHECK constraint
ALTER TABLE requisitions DROP CONSTRAINT IF EXISTS requisitions_status_check;

-- 1.2 Re-create it with the new value
ALTER TABLE requisitions
  ADD CONSTRAINT requisitions_status_check
  CHECK (status IN (
    'DRAFT',
    'PENDING',
    'AWAITING_APPROVAL',
    'APPROVED',
    'PARTIALLY_CONVERTED',
    'CONVERTED',
    'REJECTED',
    'CANCELLED'
  ));

-- 2.1 Add the new FK column (nullable; POs created without a source
--     requisition keep this column NULL).
ALTER TABLE purchase_order_items
  ADD COLUMN IF NOT EXISTS requisition_item_id BIGINT;

-- 2.2 FK constraint
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_poi_requisition_item'
  ) THEN
    ALTER TABLE purchase_order_items
      ADD CONSTRAINT fk_poi_requisition_item
      FOREIGN KEY (requisition_item_id)
      REFERENCES requisition_items(id)
      ON DELETE SET NULL;
  END IF;
END
$$;

-- 2.3 Index for the new FK (used by award aggregation queries)
CREATE INDEX IF NOT EXISTS idx_poi_requisition_item
  ON purchase_order_items (requisition_item_id);
