package com.visco.backend.reports.models.dtos;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WarehouseAnalysisDTO {
    private Long warehouseId;
    private String warehouseName;
    private Integer productCount;
    private BigDecimal totalValue;
    private Double capacityUtilization;
    private Integer criticalProducts;
    private Integer lowStockProducts;
    private BigDecimal averageRotation;

    private List<TopProductDTO> topByValue;
    private List<TopProductDTO> topByQuantity;
    private List<CategoryDistributionDTO> categoryDistribution;

    @Data
    @Builder
    public static class TopProductDTO {
        private String productName;
        private BigDecimal value;
    }

    @Data
    @Builder
    public static class CategoryDistributionDTO {
        private String categoryName;
        private Integer quantity;
        private Double percentage;
    }
}
