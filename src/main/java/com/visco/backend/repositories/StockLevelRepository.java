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
    // ─────────────────────────────────────────────────────────────
    // Búsquedas para actualizaciones (Capa de Servicio)
    // ─────────────────────────────────────────────────────────────

    // Todas las filas de StockLevel para un producto dado (en todas las
    // ubicaciones)
    List<StockLevel> findByProductId(Long productId);

    // Búsqueda exacta para sumar/restar stock en un estante/ubicación específica
    Optional<StockLevel> findByProductIdAndLocationId(Long productId, Long locationId);

    // Obtener todos los registros de un producto dentro de un almacén específico
    List<StockLevel> findByProductIdAndLocationWarehouseId(Long productId, Long warehouseId);

    // ─────────────────────────────────────────────────────────────
    // Consultas para Reportes y Kardex
    // ─────────────────────────────────────────────────────────────

    // 1. Stock físico total en todas las ubicaciones
    @Query("SELECT SUM(s.currentStock) FROM StockLevel s WHERE s.product.id = :productId")
    BigDecimal getTotalStockByProductId(@Param("productId") Long productId);

    // 2. Stock de un producto en un almacén específico
    @Query(
        "SELECT SUM(s.currentStock) FROM StockLevel s " +
            "WHERE s.product.id = :productId AND s.location.warehouse.id = :warehouseId"
    )
    BigDecimal getStockByProductAndWarehouse(
        @Param("productId") Long productId,
        @Param("warehouseId") Long warehouseId
    );

    // 3. Stock de un producto agrupado por almacén
    @Query(
        "SELECT s.location.warehouse.id as warehouseId, " +
            "s.location.warehouse.name as warehouseName, " +
            "SUM(s.currentStock) as currentStock, " +
            "SUM(s.pendingStock) as pendingStock " +
            "FROM StockLevel s WHERE s.product.id = :productId " +
            "GROUP BY s.location.warehouse.id, s.location.warehouse.name"
    )
    List<WarehouseStockProjection> getStockByProductGroupedByWarehouse(
        @Param("productId") Long productId
    );

    // 4. Resumen global de stock por almacén
    @Query(
        "SELECT s.location.warehouse.id as warehouseId, " +
            "s.location.warehouse.name as warehouseName, " +
            "SUM(s.currentStock) as currentStock, " +
            "SUM(s.pendingStock) as pendingStock " +
            "FROM StockLevel s GROUP BY s.location.warehouse.id, s.location.warehouse.name"
    )
    List<WarehouseStockProjection> getGlobalStockByWarehouse();

    interface WarehouseStockProjection {
        Long getWarehouseId();

        String getWarehouseName();

        java.math.BigDecimal getCurrentStock();

        java.math.BigDecimal getPendingStock();
    }
}
