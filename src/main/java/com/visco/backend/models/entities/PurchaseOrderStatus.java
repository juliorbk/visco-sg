package com.visco.backend.models.entities;

public enum PurchaseOrderStatus {
  PENDING,
  IN_TRANSIT,
  DELIVERED,
  COMPLETED,
  PARTIALLY_DELIVERED,
  CANCELLED,
  AWAITING_APPROVAL,
  REJECTED,
  APPROVED,
  WAITING_PAYMENT,
  HELD_AT_CUSTOMS,
}
