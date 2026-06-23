package com.visco.backend.reports.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyTransferReportKPIs {
    private int totalTransfers;
    private BigDecimal totalQuantityTransferred;
    private String topTransferredProduct;
    private String topSourceWarehouse;
    private String topDestinationWarehouse;
    private LocalDateTime generatedAt;
    private List<DailyTransferReportDTO> rows;
}
