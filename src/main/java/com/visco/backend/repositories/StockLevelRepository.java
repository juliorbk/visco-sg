package com.visco.backend.repositories;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visco.backend.models.entities.StockLevel;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    // 1. Total physical stock across all locations
    @Query("SELECT SUM(s.currentStock) FROM StockLevel s WHERE s.product.id = :productId")
    BigDecimal getTotalStockByProductId(@Param("productId") Long productId);

    // 2. Stock for a product in a specific warehouse
    @Query("SELECT SUM(s.currentStock) FROM StockLevel s "
            + "WHERE s.product.id = :productId AND s.location.warehouse.id = :warehouseId")
    BigDecimal getStockByProductAndWarehouse(@Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // 3. Stock for a product grouped by warehouse
    @Query("SELECT s.location.warehouse.id as warehouseId, " +
           "s.location.warehouse.name as warehouseName, " +
           "SUM(s.currentStock) as currentStock, " +
           "SUM(s.pendingStock) as pendingStock " +
           "FROM StockLevel s WHERE s.product.id = :productId " +
           "GROUP BY s.location.warehouse.id, s.location.warehouse.name")
    List<WarehouseStockProjection> getStockByProductGroupedByWarehouse(@Param("productId") Long productId);

    // 4. All StockLevel rows for a given product (across all locations)
    List<StockLevel> findByProductId(Long productId);

    // 5. Global stock summary per warehouse
    @Query("SELECT s.location.warehouse.id as warehouseId, " +
           "s.location.warehouse.name as warehouseName, " +
           "SUM(s.currentStock) as currentStock, " +
           "SUM(s.pendingStock) as pendingStock " +
           "FROM StockLevel s GROUP BY s.location.warehouse.id, s.location.warehouse.name")
    List<WarehouseStockProjection> getGlobalStockByWarehouse();

    interface WarehouseStockProjection {
        Long getWarehouseId();
        String getWarehouseName();
        java.math.BigDecimal getCurrentStock();
        java.math.BigDecimal getPendingStock();
    }
}
