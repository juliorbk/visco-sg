package com.visco.backend.repositories;

import com.visco.backend.models.entities.StockLevel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {
  List<StockLevel> findByProductId(Long productId);

  Optional<StockLevel> findByProductIdAndWarehouseId(
    Long productId,
    Long warehouseId
  );

  List<StockLevel> findByProductIdAndWarehouseIdIn(
    Long productId,
    List<Long> warehouseIds
  );

  // ─────────────────────────────────────────────────────────────
  // Totales por producto (usados en ProductDTO y ProductService)
  // ─────────────────────────────────────────────────────────────

  @Query(
    "SELECT COALESCE(SUM(s.currentStock), 0) FROM StockLevel s WHERE s.product.id = :productId"
  )
  BigDecimal getTotalStockByProductId(@Param("productId") Long productId);

  @Query(
    "SELECT COALESCE(SUM(s.pendingStock), 0) FROM StockLevel s WHERE s.product.id = :productId"
  )
  BigDecimal getTotalPendingStockByProductId(
    @Param("productId") Long productId
  );

  @Query(
    "SELECT COALESCE(SUM(s.currentStock), 0) FROM StockLevel s " +
      "WHERE s.product.id = :productId AND s.warehouse.id = :warehouseId"
  )
  BigDecimal getStockByProductAndWarehouse(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId
  );

  // ─────────────────────────────────────────────────────────────
  // Breakdown por almacén para un producto (ProductStockBreakdown)
  // ─────────────────────────────────────────────────────────────

  @Query(
    "SELECT s.warehouse.id               as warehouseId, " +
      "       s.warehouse.name             as warehouseName, " +
      "       COALESCE(SUM(s.currentStock), 0) as currentStock, " +
      "       COALESCE(SUM(s.pendingStock), 0) as pendingStock " +
      "FROM StockLevel s WHERE s.product.id = :productId " +
      "GROUP BY s.warehouse.id, s.warehouse.name"
  )
  List<WarehouseStockProjection> getStockByProductGroupedByWarehouse(
    @Param("productId") Long productId
  );

  // ─────────────────────────────────────────────────────────────
  // Resumen global por almacén (WarehouseStockSummary / dashboard)
  // ─────────────────────────────────────────────────────────────

  @Query(
    "SELECT s.warehouse.id                 as warehouseId, " +
      "       s.warehouse.name             as warehouseName, " +
      "       COUNT(CASE WHEN s.currentStock > 0 THEN 1 END) as currentStock, " +
      "       COUNT(CASE WHEN s.pendingStock > 0 THEN 1 END) as pendingStock " +
      "FROM StockLevel s GROUP BY s.warehouse.id, s.warehouse.name"
  )
  List<GlobalStockProjection> getGlobalStockByWarehouse();

  // ─────────────────────────────────────────────────────────────
  // Batch query para lista de productos (ProductService.getProducts)
  // ─────────────────────────────────────────────────────────────

  @Query(
    "SELECT sl.product.id, " +
      "       COALESCE(SUM(sl.currentStock), 0), " +
      "       COALESCE(SUM(sl.pendingStock), 0) " +
      "FROM StockLevel sl WHERE sl.product.id IN :productIds " +
      "GROUP BY sl.product.id"
  )
  List<Object[]> sumStockByProductIds(
    @Param("productIds") List<Long> productIds
  );

  // ─────────────────────────────────────────────────────────────
  // Projection interface
  // ─────────────────────────────────────────────────────────────

  interface WarehouseStockProjection {
    Long getSupplierId();
    Long getWarehouseId();
    String getWarehouseName();
    BigDecimal getCurrentStock();
    BigDecimal getPendingStock();
  }

  interface GlobalStockProjection {
    Long getWarehouseId();
    String getWarehouseName();
    Long getCurrentStock();
    Long getPendingStock();
  }
}
