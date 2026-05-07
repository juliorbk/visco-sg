package com.visco.backend.repositories;

import com.visco.backend.models.entities.StockLevel;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {
  // 1. Obtener el stock global sumando todas las ubicaciones
  @Query(
    "SELECT SUM(s.currentStock) FROM StockLevel s WHERE s.product.id = :productId"
  )
  BigDecimal getTotalStockByProductId(@Param("productId") Long productId);

  // 2. Obtener el stock de un producto en un almacén específico
  @Query(
    "SELECT SUM(s.currentStock) FROM StockLevel s WHERE s.product.id = :productId AND s.location.warehouse.id = :warehouseId"
  )
  BigDecimal getStockByProductAndWarehouse(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId
  );
}
