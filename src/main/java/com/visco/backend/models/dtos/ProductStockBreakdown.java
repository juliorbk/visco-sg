package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductStockBreakdown {
    private Long productId;
    private BigDecimal totalStock;
    private BigDecimal totalPendingStock;
    private List<WarehouseStockEntry> warehouses;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WarehouseStockEntry {
        private Long warehouseId;
        private String warehouseName;
        private BigDecimal currentStock;
        private BigDecimal pendingStock;
    }
}
