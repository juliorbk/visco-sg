package com.visco.backend.models.entities;

// Status lifecycle for invoice matching against purchase orders and payment tracking.
public enum InvoiceStatus {
    PENDING, MATCHED, PARTIALLY_MATCHED, UNMATCHED, PAID, OVERDUE, CANCELLED
}
