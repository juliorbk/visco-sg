-- Drop exit_unit_price column from inventory movements and dispatch note items.
-- Exit pricing is no longer tracked on dispatches.

ALTER TABLE inventory_movements
    DROP COLUMN IF EXISTS exit_unit_price;

ALTER TABLE dispatch_note_items
    DROP COLUMN IF EXISTS exit_unit_price;
