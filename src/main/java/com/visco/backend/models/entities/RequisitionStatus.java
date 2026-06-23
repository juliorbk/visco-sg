package com.visco.backend.models.entities;

// Status lifecycle for requisitions from draft through approval and conversion to PO.
//
// APPROVED             — requisition approved, no PO has been created yet
// PARTIALLY_CONVERTED  — at least one PO has been created from this requisition,
//                        but some RequisitionItem lines are still pending award
// CONVERTED            — every RequisitionItem line has been fully awarded to POs
public enum RequisitionStatus {
    DRAFT,
    PENDING,
    AWAITING_APPROVAL,
    APPROVED,
    PARTIALLY_CONVERTED,
    CONVERTED,
    REJECTED,
    CANCELLED
}
