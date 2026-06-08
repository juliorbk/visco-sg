package com.visco.backend.models.entities;

// Types of inventory movements: inbound, outbound, transfers, adjustments, and dispatches.
public enum MovementType {
  INPUT,
  OUTPUT,
  TRANSFER,
  ADJUSTMENT,
  DISPATCH,
}
