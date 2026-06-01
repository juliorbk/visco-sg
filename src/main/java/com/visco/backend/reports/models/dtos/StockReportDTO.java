package com.visco.backend.reports.models.dtos;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockReportDTO {
    private Long productId;
    private String internalCode;
    private String sku;
    private String productName;
    private BigDecimal currentStock;
    private BigDecimal pendingStock;
    private BigDecimal maxStock;
    private BigDecimal reorderPoint;
    private String category;
    private String supplier;
    private String uom;
    private String status;
    private List<WarehouseStockInfo> warehouseDetails;

    @Data
    @Builder
    public static class WarehouseStockInfo {
        private String warehouseName;
        private BigDecimal stock;
    }
}
