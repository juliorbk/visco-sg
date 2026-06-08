package com.visco.backend.reports.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
// DTO representing a product inventory alert with severity and action.
public class AlertReportDTO {
    private Long productId;
    private String internalCode;
    private String sku;
    private String productName;
    private BigDecimal currentStock;
    private BigDecimal reorderPoint;
    private BigDecimal maxStock;
    private String alertType;
    private String severity;
    private LocalDateTime detectedAt;
    private String recommendedAction;
    private List<String> affectedWarehouses;
}
