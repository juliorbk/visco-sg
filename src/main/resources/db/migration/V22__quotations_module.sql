-- Quotations module: suppliers' responses to approved requisitions.
--
-- This migration adds the three new tables (quotations, quotation_items,
-- quotation_awards) and links purchase_orders back to a quotation when a PO
-- is generated from an awarded quotation.
--
-- Notes:
--   * `quotation_number` is unique, format "COT-2026-0001" (generated in service).
--   * `quantity_mismatch_warning` is a BOOLEAN flag set by the service when
--     offered_quantity != requested_quantity; it never blocks persistence.
--   * `quotation_awards.requisition_item_id` is UNIQUE: at most one award per
--     RequisitionItem. Re-awarding the same line overwrites the previous row.
--   * `purchase_orders.quotation_id` is NULLABLE: legacy POs and direct POs
--     (created without a quotation) keep this column null.

-- ─────────────────────────────────────────────────────────────────────
-- 1. quotations
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quotations (
    id BIGSERIAL PRIMARY KEY,
    quotation_number VARCHAR(50) UNIQUE NOT NULL,
    requisition_id BIGINT NOT NULL REFERENCES requisitions(id),
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    created_by_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    valid_until TIMESTAMP,
    shipping_conditions VARCHAR(1000),
    payment_conditions VARCHAR(1000),
    payment_method VARCHAR(30) NOT NULL,
    warranty_terms VARCHAR(1000),
    notes VARCHAR(1000),
    edit_reason VARCHAR(1000),
    submitted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_q_status CHECK (status IN (
        'DRAFT',
        'SUBMITTED',
        'UNDER_REVIEW',
        'AWARDED',
        'PARTIALLY_AWARDED',
        'REJECTED',
        'EXPIRED',
        'CANCELLED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_q_requisition ON quotations(requisition_id);
CREATE INDEX IF NOT EXISTS idx_q_supplier    ON quotations(supplier_id);
CREATE INDEX IF NOT EXISTS idx_q_status      ON quotations(status);
CREATE INDEX IF NOT EXISTS idx_q_currency    ON quotations(currency);
CREATE INDEX IF NOT EXISTS idx_q_created_at  ON quotations(created_at);

-- ─────────────────────────────────────────────────────────────────────
-- 2. quotation_items
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quotation_items (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    requisition_item_id BIGINT NOT NULL REFERENCES requisition_items(id),
    offered_product_id BIGINT REFERENCES products(id),
    line_number INTEGER,
    offered_description VARCHAR(500),
    offered_sku VARCHAR(100),
    brand VARCHAR(100),
    model VARCHAR(100),
    requested_quantity NUMERIC(18,4) NOT NULL,
    offered_quantity   NUMERIC(18,4) NOT NULL,
    unit_price         NUMERIC(18,4) NOT NULL,
    delivery_days INTEGER,
    quantity_mismatch_warning BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_qi_quotation      ON quotation_items(quotation_id);
CREATE INDEX IF NOT EXISTS idx_qi_req_item       ON quotation_items(requisition_item_id);
CREATE INDEX IF NOT EXISTS idx_qi_product        ON quotation_items(offered_product_id);
CREATE INDEX IF NOT EXISTS idx_qi_line           ON quotation_items(line_number);

-- ─────────────────────────────────────────────────────────────────────
-- 3. quotation_awards
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quotation_awards (
    id BIGSERIAL PRIMARY KEY,
    requisition_id BIGINT NOT NULL REFERENCES requisitions(id),
    requisition_item_id BIGINT NOT NULL UNIQUE REFERENCES requisition_items(id),
    winning_quotation_item_id BIGINT NOT NULL REFERENCES quotation_items(id),
    awarded_supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    awarded_quantity NUMERIC(18,4) NOT NULL,
    awarded_unit_price NUMERIC(18,4) NOT NULL,
    awarded_subtotal NUMERIC(18,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    justification VARCHAR(1000),
    awarded_by_id UUID NOT NULL REFERENCES users(id),
    awarded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_qa_status CHECK (status IN (
        'PENDING',
        'AWARDED',
        'REJECTED',
        'OVERRIDDEN'
    ))
);

CREATE INDEX IF NOT EXISTS idx_qa_requisition     ON quotation_awards(requisition_id);
CREATE INDEX IF NOT EXISTS idx_qa_quotation_item  ON quotation_awards(winning_quotation_item_id);
CREATE INDEX IF NOT EXISTS idx_qa_supplier        ON quotation_awards(awarded_supplier_id);

-- ─────────────────────────────────────────────────────────────────────
-- 4. Link purchase_orders back to the originating quotation
-- ─────────────────────────────────────────────────────────────────────
ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS quotation_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_po_quotation'
    ) THEN
        ALTER TABLE purchase_orders
            ADD CONSTRAINT fk_po_quotation
            FOREIGN KEY (quotation_id) REFERENCES quotations(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_po_quotation ON purchase_orders(quotation_id);
