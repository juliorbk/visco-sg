package com.visco.backend.repositories;

import com.visco.backend.models.entities.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
  Optional<Product> findByInternalCode(String internalCode);

  Optional<Product> findBySku(String sku);

  @Query(
    "SELECT p FROM Product p LEFT JOIN FETCH p.supplier LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId"
  )
  Page<Product> findByCategoryIdWithFetch(
    @Param("categoryId") Long categoryId,
    Pageable pageable
  );

  @Query(
    """
      SELECT p FROM Product p
      LEFT JOIN FETCH p.supplier
      LEFT JOIN FETCH p.category
      JOIN StockLevel s ON s.product.id = p.id
      WHERE s.warehouse.id = :warehouseId
    """
  )
  Page<Product> findByWarehouse(
    @Param("warehouseId") Long warehouseId,
    Pageable pageable
  );

  Page<Product> findAll(Pageable pageable);

  @Query(
    "SELECT COUNT(DISTINCT p) FROM Product p " +
      "JOIN StockLevel s ON s.product.id = p.id " +
      "WHERE s.currentStock <= p.reorderPoint AND p.active = true"
  )
  Long countProductsBelowReorderPoint();

  @Query(
    "SELECT p FROM Product p " +
      "JOIN StockLevel s ON s.product.id = p.id " +
      "WHERE s.currentStock <= p.reorderPoint AND p.active = true " +
      "ORDER BY s.currentStock ASC"
  )
  List<Product> findProductsBelowReorderPoint();

  @Query("SELECT COALESCE(SUM(s.currentStock), 0) FROM StockLevel s")
  BigDecimal getTotalInventoryUnits();

  @Query(
    "SELECT p.id as productId, p.name as productName, p.sku as sku, " +
      "COALESCE(SUM(s.currentStock), 0) as currentStock, " +
      "p.reorderPoint as reorderPoint, " +
      "p.maxStock as maxStock " +
      "FROM Product p JOIN StockLevel s ON s.product.id = p.id " +
      "WHERE s.currentStock <= p.reorderPoint AND p.active = true " +
      "GROUP BY p.id, p.name, p.sku, p.reorderPoint, p.maxStock " +
      "ORDER BY currentStock ASC"
  )
  List<CriticalProductProjection> findCriticalInventory();

  @Query(
    "SELECT p.id as productId, p.name as productName, p.sku as sku, " +
      "COALESCE(SUM(s.currentStock), 0) as currentStock, " +
      "p.reorderPoint as reorderPoint, " +
      "p.maxStock as maxStock " +
      "FROM Product p JOIN StockLevel s ON s.product.id = p.id " +
      "WHERE s.currentStock >= p.maxStock AND p.active = true " +
      "GROUP BY p.id, p.name, p.sku, p.reorderPoint, p.maxStock " +
      "ORDER BY currentStock DESC"
  )
  List<CriticalProductProjection> findOverstockInventory();

  @Query(
    value = "SELECT nextval('product_internal_code_seq')",
    nativeQuery = true
  )
  Long getNextInternalCodeSequence();

  @Query(
    """
    SELECT p FROM Product p
    LEFT JOIN FETCH p.supplier
    LEFT JOIN FETCH p.category
    WHERE
      (CAST(:search AS String) IS NULL
        OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(p.sku) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(CAST(p.internalCode AS String)) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%'))
      AND (CAST(:category AS String) IS NULL OR LOWER(p.category.name) = LOWER(CAST(:category AS String)))
    """
  )
  Page<Product> findBySearchAndCategory(
    Pageable pageable,
    @Param("search") String search,
    @Param("category") Long category
  );

  // ─────────────────────────────────────────────────────────────
  // Stock sort: LEFT JOIN + GROUP BY instead of a correlated
  // subquery in ORDER BY. The old version ran one subquery per
  // row before pagination, which timed out on Render's free
  // Postgres and returned a 502.
  // ─────────────────────────────────────────────────────────────

  @Query(
    value = """
    SELECT p FROM Product p
    LEFT JOIN FETCH p.supplier
    LEFT JOIN FETCH p.category
    LEFT JOIN StockLevel sl ON sl.product.id = p.id
    WHERE
      (CAST(:search AS String) IS NULL
        OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(p.sku) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(CAST(p.internalCode AS String)) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%'))
      AND (CAST(:category AS Long) IS NULL OR p.category.id = CAST(:category AS Long))
    GROUP BY p.id
    ORDER BY COALESCE(SUM(sl.currentStock), 0) ASC
    """,
    countQuery = """
    SELECT COUNT(DISTINCT p.id) FROM Product p
    WHERE
      (CAST(:search AS String) IS NULL
        OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(p.sku) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(CAST(p.internalCode AS String)) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%'))
      AND (CAST(:category AS Long) IS NULL OR p.category.id = CAST(:category AS Long))
    """
  )
  Page<Product> findBySearchAndCategoryOrderByStockAsc(
    Pageable pageable,
    @Param("search") String search,
    @Param("category") Long category
  );

  @Query(
    value = """
    SELECT p FROM Product p
    LEFT JOIN FETCH p.supplier
    LEFT JOIN FETCH p.category
    LEFT JOIN StockLevel sl ON sl.product.id = p.id
    WHERE
      (CAST(:search AS String) IS NULL
        OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(p.sku) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(CAST(p.internalCode AS String)) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%'))
      AND (CAST(:category AS Long) IS NULL OR p.category.id = CAST(:category AS Long))
    GROUP BY p.id
    ORDER BY COALESCE(SUM(sl.currentStock), 0) DESC
    """,
    countQuery = """
    SELECT COUNT(DISTINCT p.id) FROM Product p
    WHERE
      (CAST(:search AS String) IS NULL
        OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(p.sku) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%')
        OR LOWER(CAST(p.internalCode AS String)) LIKE CONCAT('%', LOWER(CAST(:search AS String)), '%'))
      AND (CAST(:category AS Long) IS NULL OR p.category.id = CAST(:category AS Long))
    """
  )
  Page<Product> findBySearchAndCategoryOrderByStockDesc(
    Pageable pageable,
    @Param("search") String search,
    @Param("category") Long category
  );

  @Query(
    """
        SELECT COUNT(p.id)
        FROM Product p
        WHERE (
            SELECT COALESCE(SUM(s.currentStock), 0)
            FROM StockLevel s
            WHERE s.product = p
        ) <= 0
    """
  )
  long countProductsOutOfStock();

  interface CriticalProductProjection {
    Long getProductId();
    String getProductName();
    String getSku();
    BigDecimal getCurrentStock();
    BigDecimal getReorderPoint();
    BigDecimal getMaxStock();
  }

  @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
  long countTotalActiveProducts();
}
