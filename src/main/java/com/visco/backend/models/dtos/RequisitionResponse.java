package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.RequisitionStatus;
import java.time.LocalDateTime;
import java.util.List;

// Response DTO for a complete requisition with items, approval status,
// and the purchase orders that have been awarded against it (so the UI can
// show partial-conversion progress and link to each child PO).
public record RequisitionResponse(
    Long id,
    String requisitionNumber,
    String description,
    String requestedBy,
    String areaName,
    RequisitionStatus status,
    String rejectionReason,
    String approvalNotes,
    String approvedBy,
    LocalDateTime approvedAt,
    LocalDateTime createdAt,
    List<RequisitionItemResponse> items,
    List<RequisitionPurchaseOrderSummary> purchaseOrders
) {
    public record RequisitionPurchaseOrderSummary(
        Long id,
        String orderNumber,
        String supplierName,
        String status,
        java.math.BigDecimal totalAmount,
        LocalDateTime createdAt
    ) {}
}
