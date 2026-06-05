-- Indexes for foreign-key and frequently filtered columns that were
-- missing from the entity definitions. Hibernate's ddl-auto=update
-- does not always add new indexes, so we do it explicitly here for
-- environments running with ddl-auto=validate.
--
-- IF NOT EXISTS makes the migration idempotent for environments
-- where the indexes may have been added by Hibernate or by an
-- earlier manual run.

CREATE INDEX IF NOT EXISTS idx_req_requested_by
  ON requisitions (requested_by_id);
CREATE INDEX IF NOT EXISTS idx_req_cost_center
  ON requisitions (cost_center_id);
CREATE INDEX IF NOT EXISTS idx_req_approved_by
  ON requisitions (approved_by_id);
CREATE INDEX IF NOT EXISTS idx_req_status
  ON requisitions (status);
CREATE INDEX IF NOT EXISTS idx_req_created_at
  ON requisitions (created_at);

CREATE INDEX IF NOT EXISTS idx_ri_requisition
  ON requisition_items (requisition_id);
CREATE INDEX IF NOT EXISTS idx_ri_product
  ON requisition_items (product_id);

CREATE INDEX IF NOT EXISTS idx_category_parent
  ON categories (parent_id);

CREATE INDEX IF NOT EXISTS idx_cost_center_management
  ON cost_centers (management_id);

CREATE INDEX IF NOT EXISTS idx_management_general_management
  ON management (general_management_id);

CREATE INDEX IF NOT EXISTS idx_employee_cost_center
  ON employees (cost_center_id);
