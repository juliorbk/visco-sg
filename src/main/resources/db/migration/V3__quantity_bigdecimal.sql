-- Migrate purchase_order_items.quantity and requisition_items.quantity
-- from INTEGER to NUMERIC(18, 4) so that fractional quantities
-- (e.g., 1.5 kg, 0.25 units) can be stored.
--
-- USING NUMERIC casts the existing integer values to the new type
-- without loss. The precision is sufficient for any reasonable
-- stock-keeping unit.

ALTER TABLE purchase_order_items
  ALTER COLUMN quantity TYPE NUMERIC(18, 4)
  USING quantity::NUMERIC(18, 4);

ALTER TABLE purchase_order_items
  ALTER COLUMN unit_price TYPE NUMERIC(18, 4)
  USING unit_price::NUMERIC(18, 4);

ALTER TABLE requisition_items
  ALTER COLUMN quantity TYPE NUMERIC(18, 4)
  USING quantity::NUMERIC(18, 4);
