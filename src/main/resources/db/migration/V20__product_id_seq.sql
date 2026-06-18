-- V20: ensure the product_id_seq sequence exists and is in sync with MAX(id).
-- The Product entity (commit c8e561c) was changed to use
--   @SequenceGenerator(name = "product_seq",
--                     sequenceName = "product_id_seq",
--                     allocationSize = 1)
-- so Hibernate reads/writes product_id_seq on every Product insert.
-- On databases where the products table was created as IDENTITY
-- (no sequence) the first insert crashes with
--   ERROR: relation "product_id_seq" does not exist
-- This migration is idempotent: it creates the sequence if it is
-- missing, and resets it to MAX(id) + 1 so existing rows do not
-- collide with new inserts.
--
-- Note: we do NOT alter the column DEFAULT here. With Hibernate's
-- SEQUENCE strategy the sequence is consumed via
-- `SELECT nextval('product_id_seq')` during insert, regardless of
-- the column's DEFAULT. If the column is currently IDENTITY we
-- leave it alone (changing the generation strategy is a separate,
-- non-trivial change).

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_class
    WHERE relname = 'product_id_seq'
      AND relkind = 'S'
  ) THEN
    CREATE SEQUENCE product_id_seq
      INCREMENT BY 1
      MINVALUE 1
      MAXVALUE 9223372036854775807
      START 1
      CACHE 1;
  END IF;
END $$;

-- Sync the sequence with the current MAX(id) so the next insert does
-- not collide with an existing row. setval(..., true) makes the
-- next nextval() return this value + 1.
SELECT setval(
  'product_id_seq',
  GREATEST((SELECT COALESCE(MAX(id), 0) FROM products), 1),
  true
);
