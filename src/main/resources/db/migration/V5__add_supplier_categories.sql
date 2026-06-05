-- Supplier categorization
-- 1. Create supplier_categories table
CREATE TABLE supplier_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 2. Link suppliers to supplier_categories (nullable: a supplier can have no category)
ALTER TABLE suppliers
    ADD COLUMN category_id BIGINT;

ALTER TABLE suppliers
    ADD CONSTRAINT fk_suppliers_category
    FOREIGN KEY (category_id)
    REFERENCES supplier_categories(id)
    ON DELETE SET NULL;

-- 3. Index for category lookups
CREATE INDEX idx_suppliers_category_id ON suppliers(category_id);
