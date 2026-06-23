package com.visco.backend.models.entities;

// Status lifecycle for a Quotation (RFQ response from a supplier).
//
// DRAFT              — created and being filled by PROCUREMENT; editable
// SUBMITTED          — sent for review; can still be edited with mandatory editReason
// UNDER_REVIEW       — being evaluated alongside other quotations of the same requisition
// AWARDED            — at least one line was awarded to this quotation
// PARTIALLY_AWARDED  — some lines were awarded, others not
// REJECTED           — not selected; either lost the price comparison or manually rejected
// EXPIRED            — past validUntil and not submitted
// CANCELLED          — cancelled by PROCUREMENT before award
public enum QuotationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    AWARDED,
    PARTIALLY_AWARDED,
    REJECTED,
    EXPIRED,
    CANCELLED
}
