package com.visco.backend.reports.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyReceiptReportDTO {
    private String receiptNumber;
    private LocalDateTime receivedAt;
    private String purchaseOrderNumber;
    private String supplierName;
    private String supplierRif;
    private String supplierTaxId;
    // Per-reception fields (what happened in this single event).
    private int itemCount;
    private BigDecimal totalReceivedQty;
    // PO-level cumulative fields (state of the underlying purchase order
    // taking into account ALL receptions for it, not just this event).
    // Required to correctly classify a PO that was received in several
    // partial deliveries — the last partial can complete the PO.
    private BigDecimal totalOrderedQty;
    private BigDecimal cumulativeReceivedQty;
    private BigDecimal cumulativeCompletenessPct;
    private String status; // PO-level: COMPLETADA if cumulative >= ordered, else PARCIAL.
    private String receivedBy;
    private String notes;
}