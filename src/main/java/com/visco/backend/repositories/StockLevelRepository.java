package com.visco.backend.repositories;

import com.visco.backend.models.entities.StockLevel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {
  List<StockLevel> findByProductId(Long productId, Pageable pageable);

  @Query(
    """
    SELECT sl FROM StockLevel sl
    JOIN FETCH sl.product p
    LEFT JOIN FETCH p.category
    JOIN FETCH sl.warehouse
    WHERE sl.product.id IN :productIds
    """
  )
  List<StockLevel> findByProductIdIn(
    @Param("productIds") List<Long> productIds
  );

  Optional<StockLevel> findByProductIdAndWarehouseId(
    Long productId,
    Long warehouseId
  );

  List<StockLevel> findByProductIdAndWarehouseIdIn(
    Long productId,
    List<Long> warehouseIds
  );

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

  @Query(
    "SELECT s.warehouse.id                 as warehouseId, " +
      "       s.warehouse.name             as warehouseName, " +
      "       COALESCE(SUM(s.currentStock), 0) as currentStock, " +
      "       COALESCE(SUM(s.pendingStock), 0) as pendingStock " +
      "FROM StockLevel s GROUP BY s.warehouse.id, s.warehouse.name"
  )
  List<GlobalStockProjection> getGlobalStockByWarehouse();

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

  @Query(
    value = """
    SELECT sl FROM StockLevel sl JOIN FETCH sl.product p
    WHERE sl.warehouse.id = :warehouseId AND sl.currentStock > 0
    AND (CAST(:search AS string) IS NULL
      OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(p.sku) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(CAST(p.internalCode AS string)) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(p.sapCode) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """,
    countQuery = """
    SELECT COUNT(sl) FROM StockLevel sl JOIN sl.product p
    WHERE sl.warehouse.id = :warehouseId AND sl.currentStock > 0
    AND (CAST(:search AS string) IS NULL
      OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(p.sku) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(CAST(p.internalCode AS string)) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(p.sapCode) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """
  )
  Page<StockLevel> findStockWithProductByWarehouse(
    Pageable pageable,
    @Param("warehouseId") Long warehouseId,
    @Param("search") String search
  );

  @Query(
    value = """
    SELECT sl FROM StockLevel sl
    JOIN FETCH sl.product p
    LEFT JOIN FETCH p.category
    JOIN FETCH sl.warehouse
    WHERE sl.warehouse.id = :warehouseId
    AND (CAST(:search AS string) IS NULL
      OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(p.sku) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(CAST(p.internalCode AS string)) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """,
    countQuery = """
    SELECT COUNT(sl) FROM StockLevel sl JOIN sl.product p
    WHERE sl.warehouse.id = :warehouseId
    AND (CAST(:search AS string) IS NULL
      OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(p.sku) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(CAST(p.internalCode AS string)) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """
  )
  Page<StockLevel> findAllStockByWarehouse(
    Pageable pageable,
    @Param("warehouseId") Long warehouseId,
    @Param("search") String search
  );

  // ─────────────────────────────────────────────────────────────
  // Atomic stock operations (sin optimistic locking)
  // ─────────────────────────────────────────────────────────────

  @Modifying
  @Query(
    value = """
    INSERT INTO stock_levels (product_id, warehouse_id, current_stock, pending_stock)
    VALUES (:productId, :warehouseId, :quantity, 0)
    ON CONFLICT ON CONSTRAINT uk_stock_levels_product_warehouse
    DO UPDATE SET current_stock = stock_levels.current_stock + :quantity
    """,
    nativeQuery = true
  )
  int addCurrentStockAtomic(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId,
    @Param("quantity") BigDecimal quantity
  );

  @Modifying
  @Query(
    value = """
    INSERT INTO stock_levels (product_id, warehouse_id, current_stock, pending_stock)
    VALUES (:productId, :warehouseId, 0, :quantity)
    ON CONFLICT ON CONSTRAINT uk_stock_levels_product_warehouse
    DO UPDATE SET pending_stock = stock_levels.pending_stock + :quantity
    """,
    nativeQuery = true
  )
  int addPendingStockAtomic(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId,
    @Param("quantity") BigDecimal quantity
  );

  @Modifying
  @Query(
    value = """
    UPDATE stock_levels
    SET current_stock = GREATEST(current_stock - :quantity, 0)
    WHERE product_id = :productId AND warehouse_id = :warehouseId
      AND current_stock >= :quantity
    """,
    nativeQuery = true
  )
  int subtractCurrentStockAtomic(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId,
    @Param("quantity") BigDecimal quantity
  );

  @Query(
    "SELECT COALESCE(SUM(s.currentStock), 0) FROM StockLevel s WHERE s.product.id = :productId AND s.warehouse.id = :warehouseId"
  )
  BigDecimal getCurrentStock(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId
  );

  @Query(
    "SELECT COALESCE(SUM(s.pendingStock), 0) FROM StockLevel s WHERE s.product.id = :productId AND s.warehouse.id = :warehouseId"
  )
  BigDecimal getPendingStock(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId
  );

  @Modifying
  @Query(
    value = """
    UPDATE stock_levels
    SET pending_stock = GREATEST(pending_stock - :quantity, 0)
    WHERE product_id = :productId AND warehouse_id = :warehouseId
    """,
    nativeQuery = true
  )
  int subtractPendingStockAtomic(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId,
    @Param("quantity") BigDecimal quantity
  );

  @Modifying
  @Query(
    value = """
    INSERT INTO stock_levels (product_id, warehouse_id, current_stock, pending_stock)
    VALUES (:productId, :warehouseId, :newStock, 0)
    ON CONFLICT ON CONSTRAINT uk_stock_levels_product_warehouse
    DO UPDATE SET current_stock = :newStock
    """,
    nativeQuery = true
  )
  int setCurrentStockAtomic(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId,
    @Param("newStock") BigDecimal newStock
  );

  interface WarehouseStockProjection {
    Long getWarehouseId();
    String getWarehouseName();
    BigDecimal getCurrentStock();
    BigDecimal getPendingStock();
  }

  interface GlobalStockProjection {
    Long getWarehouseId();
    String getWarehouseName();
    BigDecimal getCurrentStock();
    BigDecimal getPendingStock();
  }
}
