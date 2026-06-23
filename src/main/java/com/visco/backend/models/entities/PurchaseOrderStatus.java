package com.visco.backend.models.entities;

// Status lifecycle for purchase orders from creation through delivery and payment.
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
