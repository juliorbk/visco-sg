package com.visco.backend.reports.models.dtos;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyReceiptReportKPIs {
    private int totalReceipts;
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