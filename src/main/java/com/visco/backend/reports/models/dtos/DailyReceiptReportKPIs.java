package com.visco.backend.reports.models.dtos;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyReceiptReportKPIs {
    // KPIs are now PO-based (distinct purchase orders touched in the
    // report period), not receipt-based, so that partial deliveries of
    // the same PO are not double-counted.
    private int totalReceipts;
    private int totalOrders;
    private int totalPartial;
    private int totalCompleted;
    private int totalItemsReceived;
    private int totalItemsExpected;
    private double overallCompletenessPct;
    private String topSupplier;
    private String topProduct;
    private LocalDateTime generatedAt;
    private String warehouseName;
    private List<DailyReceiptReportDTO> rows;
}