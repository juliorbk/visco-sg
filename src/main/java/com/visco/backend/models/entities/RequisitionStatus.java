package com.visco.backend.models.entities;

// Status lifecycle for requisitions from draft through approval and conversion to PO.
public enum RequisitionStatus {
  DRAFT,
  PENDING,
  AWAITING_APPROVAL,
  APPROVED,
  REJECTED,
  CANCELLED,
  CONVERTED,
  PARTIALLY_CONVERTED,
}
