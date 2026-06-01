package com.visco.backend.reports.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MovementReportDTO {
    private Long id;
    private LocalDateTime movementDate;
    private String movementType;
    private Long productId;
    private String productCode;
    private String sku;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal stockBefore;
    private BigDecimal stockAfter;
    private String warehouseName;
    private String warehouseDestination;
    private String userName;
    private String reference;
    private String reason;
}
