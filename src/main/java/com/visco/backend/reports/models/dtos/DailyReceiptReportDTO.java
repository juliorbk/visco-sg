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
    private String status;
    private int itemCount;
    private BigDecimal totalExpectedQty;
    private BigDecimal totalReceivedQty;
    private BigDecimal completenessPct;
    private String receivedBy;
    private String notes;
}