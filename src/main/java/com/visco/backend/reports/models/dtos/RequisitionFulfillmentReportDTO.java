package com.visco.backend.reports.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Row in the "Requisition Fulfillment" report.
 *
 * <p>One row per requisition item, plus per-PO breakdown rows so the
 * reader can see how a single requisition was split across suppliers.
 */
@Data
@Builder
public class RequisitionFulfillmentReportDTO {

    // ── Requisition header ──────────────────────────────────────────
    private Long requisitionId;
    private String requisitionNumber;
    private String requisitionStatus;
    private LocalDateTime requisitionCreatedAt;
    private String requestedBy;
    private String costCenter;

    // ── Item line ───────────────────────────────────────────────────
    private Long requisitionItemId;
    private Long productId;
    private String productSku;
    private String productInternalCode;
    private String productName;
    private String uom;
    private BigDecimal requestedQuantity;
    private BigDecimal awardedQuantity;
    private BigDecimal pendingQuantity;
    private String fulfillmentState; // FULLY_AWARDED | PARTIALLY_AWARDED | NOT_AWARDED

    // ── Per-PO breakdown (may be empty when nothing awarded yet) ────
    private List<AwardedPoLine> awardedPos;

    @Data
    @Builder
    public static class AwardedPoLine {
        private Long purchaseOrderId;
        private String orderNumber;
        private String supplierName;
        private String status;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private LocalDateTime createdAt;
    }
}
