-- ─────────────────────────────────────────────────────────────
-- V12: Indexes optimizados para la nueva busqueda por campos
-- especificos (name, sapCode, sku)
-- ─────────────────────────────────────────────────────────────
-- Los indices trigram GIN de V9/V10 son utiles para busquedas
-- parciales (LIKE '%term%'), pero para la nueva estrategia de
-- busqueda con campos especificos, necesitamos B-tree dedicados
-- que aprovechen LIKE 'term%' (sin wildcard inicial) y matches
-- exactos, que son mucho mas rapidos para >100k productos.

-- 1. B-tree en sap_code para match exacto (no existia indice dedicado)
--    WHERE p.sap_code = '12345' → Index Scan O(log n)
CREATE INDEX IF NOT EXISTS idx_products_sap_code_btree
  ON products (sap_code);

-- 2. B-tree compuesto (is_active, name) con text_pattern_ops
--    Permite que LIKE 'term%' use el B-tree de forma eficiente
--    incluso con locales que no sean "C" (acentos).
--    El is_active primero filtra productos activos (la mayoria
--    de queries lo requieren implicitamente por @SQLRestriction).
CREATE INDEX IF NOT EXISTS idx_products_active_name_btree
  ON products (is_active, name text_pattern_ops);

-- 3. Los indices GIN V9/V10 (trigram unaccent) se mantienen
--    por si se necesitan busquedas parciales en el futuro.

-- 4. Refrescar estadisticas para que el planner use los nuevos indices
ANALYZE products;
