package com.visco.backend.reports.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyTransferReportDTO {
    private Long movementId;
    private LocalDateTime transferDate;
    private Long productId;
    private String productInternalCode;
    private String productSku;
    private String productName;
    private BigDecimal quantity;
    private String fromWarehouseName;
    private String toWarehouseName;
    private String userName;
    private String reason;
}
