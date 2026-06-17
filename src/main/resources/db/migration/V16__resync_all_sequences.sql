DO $$
DECLARE
  r RECORD;
  max_id BIGINT;
  seq_name TEXT;
BEGIN
  FOR r IN
    SELECT
      c.relname AS tabla,
      pg_get_serial_sequence(c.relname, 'id') AS seq
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'r'
      AND n.nspname = 'public'
      AND pg_get_serial_sequence(c.relname, 'id') IS NOT NULL
  LOOP
    EXECUTE format('SELECT COALESCE(MAX(id), 0) FROM %I', r.tabla) INTO max_id;
    seq_name := r.seq;
    EXECUTE format('SELECT setval(%L, GREATEST(%s + 1, (SELECT last_value FROM %s)), true)', seq_name, max_id, seq_name);
    RAISE NOTICE 'Resynced % -> %', r.tabla, max_id + 1;
  END LOOP;
END $$;