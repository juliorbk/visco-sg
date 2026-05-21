package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resumen de stock por almacén para el endpoint GET /api/warehouse/stock-summary
 *
 * totalStock        — Stock físico presente (currentStock)
 * totalPendingStock — En tránsito, aún no recibido
 */
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
