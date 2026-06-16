CREATE INDEX IF NOT EXISTS idx_good_receipts_received_at
  ON good_receipts (received_at DESC);

CREATE INDEX IF NOT EXISTS idx_good_receipts_wh_received_at
  ON good_receipts (destination_warehouse_id, received_at DESC);