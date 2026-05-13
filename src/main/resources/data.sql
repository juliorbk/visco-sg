-- ============================================================
-- SEED DATA — visco-sg
-- Orden de inserción respetando FK:
--   1. app_user (no FK, needed for other tables referencing created_by)
--   2. warehouses
--   3. locations
--   4. categories
--   5. suppliers
--   6. products
--   7. stock_levels
--
-- TRUNCATE al inicio para que sea idempotente (dev only)
-- ============================================================

-- Ensure missing FK columns exist (workaround for Hibernate ddl-auto=update gaps)
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS created_by_id UUID;
ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_created_by FOREIGN KEY (created_by_id) REFERENCES app_user(id);

TRUNCATE TABLE stock_levels,
             purchase_order_items,
             purchase_orders,
             good_receipt_items,
             good_receipts,
             products,
             supplier_phones,
             supplier_representatives,
             suppliers,
             categories,
             locations,
             warehouses,
             app_user
RESTART IDENTITY CASCADE;

-- 1. ALMACÉN
INSERT INTO warehouses (name, physical_address, description, sap_center_code, is_active)
VALUES ('Almacén Principal Matanzas', 'Zona Industrial Matanzas, Ciudad Guayana', 'Almacén principal de materia prima e insumos', 'WH01', true);

-- 2. UBICACIÓN dentro del almacén
INSERT INTO locations (warehouse_id, aisle, shelf, bin_level, location_code)
VALUES (1, 'A', '1', 'B', 'MP-A1-B');

-- 3. CATEGORÍA
INSERT INTO categories (name) VALUES ('Materia Prima');

-- 4. PROVEEDOR
INSERT INTO suppliers (name, address, email, description, created_at, updated_at, is_active, currency)
VALUES ('Químicos del Orinoco C.A.', 'Av. Principal, Zona Industrial Los Pinos, Puerto Ordaz',
        'ventas@quimicosorinoco.com', 'Proveedor de insumos químicos industriales',
        NOW(), NOW(), true, 'USD');

-- 5. PRODUCTOS
INSERT INTO products (internal_code, sku, name, description, sap_code, uom, reorder_point, is_active, supplier_id, category_id)
VALUES ('PROD-001', 'SKU-RX-001', 'Resina XR-1000', 'Resina epóxica de alta temperatura', 'SAP-RX-001', 'KILOGRAMO', 500.000, true, 1, 1);

INSERT INTO products (internal_code, sku, name, description, sap_code, uom, reorder_point, is_active, supplier_id, category_id)
VALUES ('PROD-002', 'SKU-CA-002', 'Catalizador C-200', 'Catalizador para reacciones de polimerización', 'SAP-CA-002', 'LITRO', 200.000, true, 1, 1);

INSERT INTO products (internal_code, sku, name, description, sap_code, uom, reorder_point, is_active, supplier_id, category_id)
VALUES ('PROD-003', 'SKU-SV-003', 'Solvente SV-300', 'Solvente industrial para limpieza de equipos', 'SAP-SV-003', 'GALON', 1000.000, true, 1, 1);

-- 6. STOCK LEVEL para cada producto
INSERT INTO stock_levels (product_id, location_id, current_stock, pending_stock)
VALUES (1, 1, 850.000, 0.000);

INSERT INTO stock_levels (product_id, location_id, current_stock, pending_stock)
VALUES (2, 1, 150.000, 0.000);

INSERT INTO stock_levels (product_id, location_id, current_stock, pending_stock)
VALUES (3, 1, 1200.000, 0.000);
