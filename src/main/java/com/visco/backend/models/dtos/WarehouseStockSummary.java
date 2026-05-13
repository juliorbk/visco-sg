package com.visco.backend.models.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseStockSummary {
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal totalStock;
    private BigDecimal totalPendingStock;
}
