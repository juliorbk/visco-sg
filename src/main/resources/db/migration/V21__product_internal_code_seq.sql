-- V21: ensure product_internal_code_seq exists and is in sync with
-- the largest numeric internal_code already in the products table.
--
-- The Product service (ProductService.generateNextInternalCode) uses
--   SELECT nextval('product_internal_code_seq')
-- and formats the result as a six-digit zero-padded string used as
-- the unique internal_code column. On databases where the sequence
-- was never created (e.g. created from scratch on a fresh prod DB),
-- Hibernate materializes it with START = 1, so the first few inserts
-- succeed but the Nth insert collides with an existing row:
--   ERROR: duplicate key value violates unique constraint
--          "ukm1yttaxcja39214v3x8io89ox"
--   Key (internal_code)=(235002) already exists.
--
-- This migration is idempotent: it creates the sequence if missing
-- and resets it to MAX(internal_code) + 1 so the next value will not
-- collide with any row already in the table.

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_class
    WHERE relname = 'product_internal_code_seq'
      AND relkind = 'S'
  ) THEN
    CREATE SEQUENCE product_internal_code_seq
      INCREMENT BY 1
      MINVALUE 1
      MAXVALUE 9223372036854775807
      START 1
      CACHE 1;
  END IF;
END $$;

-- Sync with the current largest numeric internal_code. The format
-- produced by the service is "%06d" so all generated codes are pure
-- digits; we still guard the cast with a regex in case an operator
-- inserted a hand-typed non-numeric code in the past.
SELECT setval(
  'product_internal_code_seq',
  GREATEST(
    COALESCE(
      (SELECT MAX(CAST(internal_code AS BIGINT))
       FROM products
       WHERE internal_code ~ '^[0-9]+$'),
      0
    ),
    1
  ),
  true
);
