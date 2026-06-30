-- ============================================================
-- MIGRACIÓN: Optimización de modelo de datos - visco-sg
-- Versión: 1.0
-- Ejecutar en ambiente productivo con superusuario PostgreSQL
-- Ejemplo: psql -U postgres -d visco_db -f migracion_optimizacion_v1.sql
-- ============================================================

BEGIN;

-- ============================================================
-- FASE 1: COLUMNAS DE AUDITORÍA FALTANTES
-- ============================================================

ALTER TABLE products ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE categories ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE categories ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE locations ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE locations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE stock_levels ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE stock_levels ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE employees ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE employees ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE cost_centers ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE cost_centers ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE management ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE management ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE general_management ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE general_management ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

-- ============================================================
-- FASE 2: STOCK_LEVELS — ELIMINAR @Version + NOT NULL
-- ============================================================

-- Eliminar columna version (ya no usamos optimistic locking aquí)
ALTER TABLE stock_levels DROP COLUMN IF EXISTS version;

-- Hacer NOT NULL las FK que siempre deben tener valor
ALTER TABLE stock_levels ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE stock_levels ALTER COLUMN warehouse_id SET NOT NULL;

-- ============================================================
-- FASE 3: CHECK CONSTRAINTS PARA ENUMS (integridad a nivel BD)
-- ============================================================

ALTER TABLE products ADD CONSTRAINT chk_product_uom
  CHECK (uom IN ('EA','LB','ROL','CJ','TF','M','KIT','CIL','G','PAA','GLN','L','KG','LTS','YD','PAQ','CM','CTE','CA','UN','CL','FC','M2','TM','TO','BTO','PI3','M3','PI2','BOL','BOT','CTO','TON','PAI','MIL','AM','PUL','LOT','MTL','CEN','BL','GL','SB','TR'));

ALTER TABLE app_user ADD CONSTRAINT chk_user_role
  CHECK (role IN ('USER','MANAGER','ADMIN','PROCUREMENT','WAREHOUSEMAN'));

ALTER TABLE requisitions ADD CONSTRAINT chk_requisition_status
  CHECK (status IN ('DRAFT','PENDING','AWAITING_APPROVAL','APPROVED','REJECTED','CANCELLED','CONVERTED'));

ALTER TABLE invoices ADD CONSTRAINT chk_invoice_status
  CHECK (status IN ('PENDING','MATCHED','PARTIALLY_MATCHED','UNMATCHED','PAID','OVERDUE','CANCELLED'));

ALTER TABLE purchase_orders ADD CONSTRAINT chk_po_status
  CHECK (status IN ('PENDING','IN_TRANSIT','DELIVERED','COMPLETED','PARTIALLY_DELIVERED','CANCELLED','AWAITING_APPROVAL','REJECTED','APPROVED','WAITING_PAYMENT','HELD_AT_CUSTOMS'));

ALTER TABLE purchase_orders ADD CONSTRAINT chk_po_payment_method
  CHECK (payment_method IN ('CASH','BANK_TRANSFER','CHECK','USDT','PAYPAL','OTHER'));

ALTER TABLE purchase_orders ADD CONSTRAINT chk_po_type
  CHECK (type IN ('SERVICES','MATERIALS','MRO','CAPITAL_EQUIPMENT'));

ALTER TABLE inventory_movements ADD CONSTRAINT chk_movement_type
  CHECK (type IN ('INPUT','OUTPUT','TRANSFER','ADJUSTMENT'));

ALTER TABLE suppliers ADD CONSTRAINT chk_supplier_currency
  CHECK (currency IN ('VED','VES','ARS','BOB','BRL','CLP','COP','CRC','CUP','DOP','GTQ','HNL','MXN','NIO','PAB','PEN','PYG','UYU','USD','CAD','EUR','GBP','CHF','SEK','NOK','DKK','RUB','JPY','CNY','INR','AUD','NZD','KRW','SGD','HKD','AED','SAR','ZAR','EGP'));

-- ============================================================
-- FASE 4: ÍNDICES ADICIONALES PARA RENDIMIENTO
-- ============================================================

-- Índices en created_at para ordenamientos frecuentes
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at);
CREATE INDEX IF NOT EXISTS idx_requisitions_created_at ON requisitions(created_at);
CREATE INDEX IF NOT EXISTS idx_invoices_created_at ON invoices(created_at);
CREATE INDEX IF NOT EXISTS idx_dispatch_notes_created_at ON dispatch_notes(created_at);
CREATE INDEX IF NOT EXISTS idx_good_receipts_created_at ON good_receipts(created_at);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_created_at ON inventory_movements(created_at);

-- ============================================================
-- FASE 5: BORRAR CACHE DE CALIFICACIÓN (CREATED_AT NO NULO)
-- Actualizar los created_at existentes que estén en NULL
-- ============================================================

UPDATE products SET created_at = NOW() WHERE created_at IS NULL;
UPDATE categories SET created_at = NOW() WHERE created_at IS NULL;
UPDATE warehouses SET created_at = NOW() WHERE created_at IS NULL;
UPDATE locations SET created_at = NOW() WHERE created_at IS NULL;
UPDATE stock_levels SET created_at = NOW() WHERE created_at IS NULL;
UPDATE employees SET created_at = NOW() WHERE created_at IS NULL;
UPDATE cost_centers SET created_at = NOW() WHERE created_at IS NULL;
UPDATE management SET created_at = NOW() WHERE created_at IS NULL;
UPDATE general_management SET created_at = NOW() WHERE created_at IS NULL;

-- ============================================================
-- FASE 6: CREAR FUNCIÓN PARA ACTUALIZAR UPDATED_AUTOMÁTICAMENTE
-- ============================================================

CREATE OR REPLACE FUNCTION tg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar trigger a tablas con updated_at
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOR tbl IN 
        SELECT unnest(ARRAY['products', 'categories', 'warehouses', 'locations', 
                           'stock_levels', 'employees', 'cost_centers', 
                           'management', 'general_management'])
    LOOP
        EXECUTE format(
            'DROP TRIGGER IF EXISTS trg_%s_updated_at ON %s;', tbl, tbl
        );
        EXECUTE format(
            'CREATE TRIGGER trg_%s_updated_at BEFORE UPDATE ON %s FOR EACH ROW EXECUTE FUNCTION tg_set_updated_at();',
            tbl, tbl
        );
    END LOOP;
END;
$$;

COMMIT;
