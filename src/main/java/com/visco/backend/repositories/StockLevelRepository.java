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

  @Query(
    "SELECT SUM(s.currentStock) FROM StockLevel s WHERE s.product.id = :productId"
  )
  BigDecimal getTotalStockByProductId(@Param("productId") Long productId);

  @Query(
    "SELECT SUM(s.currentStock) FROM StockLevel s " +
      "WHERE s.product.id = :productId AND s.warehouse.id = :warehouseId"
  )
  BigDecimal getStockByProductAndWarehouse(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId
  );

  @Query(
    "SELECT s.warehouse.id as warehouseId, " +
      "s.warehouse.name as warehouseName, " +
      "SUM(s.currentStock) as currentStock, " +
      "SUM(s.pendingStock) as pendingStock " +
      "FROM StockLevel s WHERE s.product.id = :productId " +
      "GROUP BY s.warehouse.id, s.warehouse.name"
  )
  List<WarehouseStockProjection> getStockByProductGroupedByWarehouse(
    @Param("productId") Long productId
  );

  @Query(
    "SELECT s.warehouse.id as warehouseId, " +
      "s.warehouse.name as warehouseName, " +
      "SUM(s.currentStock) as currentStock, " +
      "SUM(s.pendingStock) as pendingStock " +
      "FROM StockLevel s GROUP BY s.warehouse.id, s.warehouse.name"
  )
  List<WarehouseStockProjection> getGlobalStockByWarehouse();

  interface WarehouseStockProjection {
    Long getWarehouseId();

    String getWarehouseName();

    java.math.BigDecimal getCurrentStock();

    java.math.BigDecimal getPendingStock();
  }

  @Query(
    "SELECT sl.product.id, SUM(sl.currentStock), SUM(sl.pendingStock) " +
      "FROM StockLevel sl WHERE sl.product.id IN :productIds " +
      "GROUP BY sl.product.id"
  )
  List<Object[]> sumStockByProductIds(
    @Param("productIds") List<Long> productIds
  );
}
