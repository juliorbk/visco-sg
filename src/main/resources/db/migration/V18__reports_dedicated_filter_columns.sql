-- Add dedicated columns for the top-level report filters that used to
-- be packed into the JSON `filters` payload under `_warehouseId`,
-- `_categoryId` and `_search`. Putting them in their own columns
-- means we can rebuild a CreateReportRequest on download without
-- re-parsing JSON and without risking the system-level fields being
-- shadowed by user-supplied additionalFilters.

ALTER TABLE reports
    ADD COLUMN warehouse_id BIGINT,
    ADD COLUMN category_id  BIGINT,
    ADD COLUMN search       VARCHAR(255);

-- Index the most common filter (warehouse) so the list page and any
-- future "reports for warehouse X" query stays fast.
CREATE INDEX IF NOT EXISTS idx_reports_warehouse_id ON reports(warehouse_id);
