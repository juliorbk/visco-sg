package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Breakdown de stock de un producto distribuido entre almacenes.
 * Usado en GET /api/warehouse/products/{productId}/stock-breakdown
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductStockBreakdown {

  private Long productId;
  private BigDecimal totalStock; // Suma de currentStock en todos los almacenes
  private BigDecimal totalPendingStock; // Suma de pendingStock en todos los almacenes
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
