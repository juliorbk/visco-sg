-- Resync all IDENTITY sequences with MAX(id) to prevent duplicate key errors.
-- V16 used pg_get_serial_sequence() which returns NULL for IDENTITY columns,
-- so this version uses pg_attribute.attidentity instead.
DO $$
DECLARE
  r RECORD;
  max_id BIGINT;
BEGIN
  FOR r IN
    SELECT
      c.relname AS tabla,
      a.attname AS col_name
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_attribute a ON a.attrelid = c.oid
      AND a.attidentity IN ('a', 'd')
    WHERE c.relkind = 'r'
      AND n.nspname = 'public'
  LOOP
    EXECUTE format('SELECT COALESCE(MAX(%I), 0) FROM %I', r.col_name, r.tabla) INTO max_id;
    EXECUTE format('ALTER TABLE %I ALTER COLUMN %I RESTART WITH %s', r.tabla, r.col_name, max_id + 1);
    RAISE NOTICE 'Resynced %.% -> %', r.tabla, r.col_name, max_id + 1;
  END LOOP;
END $$;