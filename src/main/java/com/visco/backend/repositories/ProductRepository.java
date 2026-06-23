package com.visco.backend.repositories;

import com.visco.backend.models.entities.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
  extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

  Optional<Product> findFirstByInternalCode(String internalCode);

  Optional<Product> findFirstBySku(String sku);

  @Query(
    value = "SELECT p FROM Product p JOIN FETCH p.supplier JOIN FETCH p.category WHERE p.category.id = :categoryId",
    countQuery = "SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId"
  )
  Page<Product> findByCategoryIdWithFetch(
    @Param("categoryId") Long categoryId,
    Pageable pageable
  );

  @Query(
    value = """
    SELECT p FROM Product p
    JOIN FETCH p.supplier
    JOIN FETCH p.category
    JOIN StockLevel s ON s.product.id = p.id
    WHERE s.warehouse.id = :warehouseId
    """,
    countQuery = """
    SELECT COUNT(p) FROM Product p
    JOIN StockLevel s ON s.product.id = p.id
    WHERE s.warehouse.id = :warehouseId
    """
  )
  Page<Product> findByWarehouse(
    @Param("warehouseId") Long warehouseId,
    Pageable pageable
  );

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
  List<Product> findProductsBelowReorderPoint(Pageable pageable);

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
  List<CriticalProductProjection> findCriticalInventory(Pageable pageable);

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
  List<CriticalProductProjection> findOverstockInventory(Pageable pageable);

  @Query(value = "SELECT nextval('product_internal_code_seq')", nativeQuery = true)
  Long getNextInternalCodeSequence();

  interface CriticalProductProjection {
    Long getProductId();
    String getProductName();
    String getSku();
    BigDecimal getCurrentStock();
    BigDecimal getReorderPoint();
    BigDecimal getMaxStock();
  }

  @Query(
    value = "SELECT COUNT(*) FROM (SELECT 1 FROM products p LEFT JOIN stock_levels s ON s.product_id = p.id WHERE p.is_active = true GROUP BY p.id HAVING COALESCE(SUM(s.current_stock), 0) <= 0) sub",
    nativeQuery = true
  )
  long countProductsOutOfStock();

  @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
  long countTotalActiveProducts();

  // ─────────────────────────────────────────────────────────────
  // Stock-sorted queries (native SQL required for GROUP BY +
  // aggregate ORDER BY). The @SQLRestriction on the entity does
  // NOT apply to native queries, so is_active is explicit.
  //
  // Each filter is an independent WHERE clause so the planner
  // can pick the best index per filter (B-tree for sapCode/sku,
  // B-tree text_pattern_ops for name starts-with, GIN trigram
  // for partial searches).
  // ─────────────────────────────────────────────────────────────

  @Query(
    value = """
    SELECT p.* FROM products p
    LEFT JOIN stock_levels sl ON sl.product_id = p.id
    WHERE
      (CAST(:name AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent(:name || '%'))
      AND (CAST(:sapCode AS text) IS NULL OR p.sap_code = :sapCode)
      AND (CAST(:sku AS text) IS NULL OR p.sku = :sku)
      AND (CAST(:category AS bigint) IS NULL OR p.category_id = :category)
      AND (:hasStock = false OR EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0))
      AND p.is_active = true
    GROUP BY p.id
    ORDER BY COALESCE(SUM(sl.current_stock), 0) ASC, p.id DESC
    """,
    countQuery = """
    SELECT COUNT(*) FROM products p
    WHERE
      (CAST(:name AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent(:name || '%'))
      AND (CAST(:sapCode AS text) IS NULL OR p.sap_code = :sapCode)
      AND (CAST(:sku AS text) IS NULL OR p.sku = :sku)
      AND (CAST(:category AS bigint) IS NULL OR p.category_id = :category)
      AND (:hasStock = false OR EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0))
      AND p.is_active = true
    """,
    nativeQuery = true
  )
  Page<Product> findByFiltersOrderByStockAsc(
    Pageable pageable,
    @Param("name") String name,
    @Param("sapCode") String sapCode,
    @Param("sku") String sku,
    @Param("category") Long category,
    @Param("hasStock") Boolean hasStock
  );

  @Query(
    value = """
    SELECT p.* FROM products p
    LEFT JOIN stock_levels sl ON sl.product_id = p.id
    WHERE
      (CAST(:name AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent(:name || '%'))
      AND (CAST(:sapCode AS text) IS NULL OR p.sap_code = :sapCode)
      AND (CAST(:sku AS text) IS NULL OR p.sku = :sku)
      AND (CAST(:category AS bigint) IS NULL OR p.category_id = :category)
      AND (:hasStock = false OR EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0))
      AND p.is_active = true
    GROUP BY p.id
    ORDER BY COALESCE(SUM(sl.current_stock), 0) DESC, p.id DESC
    """,
    countQuery = """
    SELECT COUNT(*) FROM products p
    WHERE
      (CAST(:name AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent(:name || '%'))
      AND (CAST(:sapCode AS text) IS NULL OR p.sap_code = :sapCode)
      AND (CAST(:sku AS text) IS NULL OR p.sku = :sku)
      AND (CAST(:category AS bigint) IS NULL OR p.category_id = :category)
      AND (:hasStock = false OR EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0))
      AND p.is_active = true
    """,
    nativeQuery = true
  )
  Page<Product> findByFiltersOrderByStockDesc(
    Pageable pageable,
    @Param("name") String name,
    @Param("sapCode") String sapCode,
    @Param("sku") String sku,
    @Param("category") Long category,
    @Param("hasStock") Boolean hasStock
  );
}
