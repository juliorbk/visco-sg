package com.visco.backend.models.entities;

// Lifecycle status for a per-line QuotationAward decision.
//
// PENDING     — record exists but not yet finalized
// AWARDED     — this supplier won the line; ties back to a QuotationItem
// REJECTED    — line was explicitly rejected (e.g. technical validation failed)
// OVERRIDDEN  — the default "lowest price" winner was overridden by a MANAGER+
//               in favor of a different supplier (e.g. better warranty)
public enum AwardStatus {
    PENDING,
    AWARDED,
    REJECTED,
    OVERRIDDEN
}
