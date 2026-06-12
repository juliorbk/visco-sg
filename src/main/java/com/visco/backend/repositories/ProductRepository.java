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

// Repository for Product entities with stock-aware queries and inventory projections.
public interface ProductRepository extends JpaRepository<Product, Long> {
  // Finds a product by its unique internal code.
  Optional<Product> findFirstByInternalCode(String internalCode);

  // Finds a product by its unique SKU.
  Optional<Product> findFirstBySku(String sku);

  // Finds products by category with supplier and category eagerly loaded.
  @Query(
    "SELECT p FROM Product p LEFT JOIN FETCH p.supplier LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId"
  )
  Page<Product> findByCategoryIdWithFetch(
    @Param("categoryId") Long categoryId,
    Pageable pageable
  );

  // Finds products that have stock in a specific warehouse.
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

  // Counts active products whose stock is at or below their reorder point.
  @Query(
    "SELECT COUNT(DISTINCT p) FROM Product p " +
      "JOIN StockLevel s ON s.product.id = p.id " +
      "WHERE s.currentStock <= p.reorderPoint AND p.active = true"
  )
  Long countProductsBelowReorderPoint();

  @Query("SELECT p FROM Product p "
      + "JOIN StockLevel s ON s.product.id = p.id "
      + "WHERE s.currentStock <= p.reorderPoint AND p.active = true "
      + "ORDER BY s.currentStock ASC")
  List<Product> findProductsBelowReorderPoint(Pageable pageable);

  // Returns the sum of all current stock across all products and warehouses.
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

  @Query(
    value = "SELECT nextval('product_internal_code_seq')",
    nativeQuery = true
  )
  Long getNextInternalCodeSequence();

  // ─────────────────────────────────────────────────────────────
  // Search bar queries (used by /api/products).
  //
  // nativeQuery = true is REQUIRED: Hibernate 7.2's HQL parser
  // refuses `FUNCTION('unaccent', …) ILIKE …` with
  // `Operand of 'like' is of type 'java.lang.Object' which is
  // not a string` because there is no reliable way to tell the
  // SqmFunctionRegistry the return type. Native SQL bypasses the
  // HQL validator entirely.
  //
  // Every native query must add `AND p.is_active = true` because
  // the entity's @SQLRestriction only applies to JPQL/HQL.
  //
  // The `unaccent(col) gin_trgm_ops` GIN indexes from V10 let
  // Postgres serve the ILIKE from the index.
  // ─────────────────────────────────────────────────────────────

  @Query(
    value = """
    SELECT p.* FROM products p
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
    ORDER BY p.id DESC
    """,
    countQuery = """
    SELECT COUNT(*) FROM products p
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
    """,
    nativeQuery = true
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
    SELECT p.* FROM products p
    LEFT JOIN stock_levels sl ON sl.product_id = p.id
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
    GROUP BY p.id
    ORDER BY COALESCE(SUM(sl.current_stock), 0) ASC
    """,
    countQuery = """
    SELECT COUNT(*) FROM products p
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
    """,
    nativeQuery = true
  )
  Page<Product> findBySearchAndCategoryOrderByStockAsc(
    Pageable pageable,
    @Param("search") String search,
    @Param("category") Long category
  );

  @Query(
    value = """
    SELECT p.* FROM products p
    LEFT JOIN stock_levels sl ON sl.product_id = p.id
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
    GROUP BY p.id
    ORDER BY COALESCE(SUM(sl.current_stock), 0) DESC
    """,
    countQuery = """
    SELECT COUNT(*) FROM products p
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
    """,
    nativeQuery = true
  )
  Page<Product> findBySearchAndCategoryOrderByStockDesc(
    Pageable pageable,
    @Param("search") String search,
    @Param("category") Long category
  );

  // Counts products with zero or negative total stock across all warehouses.
  @Query("SELECT COUNT(DISTINCT p.id) FROM Product p "
      + "LEFT JOIN StockLevel s ON s.product = p "
      + "GROUP BY p.id HAVING COALESCE(SUM(s.currentStock), 0) <= 0")
  long countProductsOutOfStock();

  interface CriticalProductProjection {
    Long getProductId();
    String getProductName();
    String getSku();
    BigDecimal getCurrentStock();
    BigDecimal getReorderPoint();
    BigDecimal getMaxStock();
  }

  // Returns the count of all active (non-archived) products.
  @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
  long countTotalActiveProducts();

  // ─────────────────────────────────────────────────────────────
  // hasStock filter: productos con stock > 0 en algún almacén
  // ─────────────────────────────────────────────────────────────

  @Query(
    value = """
    SELECT p.* FROM products p
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
      AND EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0)
    """,
    countQuery = """
    SELECT COUNT(*) FROM products p
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
      AND EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0)
    """,
    nativeQuery = true
  )
  Page<Product> findBySearchAndCategoryWithStock(
    Pageable pageable,
    @Param("search") String search,
    @Param("category") Long category
  );

  @Query(
    value = """
    SELECT p.* FROM products p
    LEFT JOIN stock_levels sl ON sl.product_id = p.id
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
      AND EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0)
    GROUP BY p.id
    ORDER BY COALESCE(SUM(sl.current_stock), 0) ASC
    """,
    countQuery = """
    SELECT COUNT(*) FROM products p
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
      AND EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0)
    """,
    nativeQuery = true
  )
  Page<Product> findBySearchAndCategoryWithStockOrderByStockAsc(
    Pageable pageable,
    @Param("search") String search,
    @Param("category") Long category
  );

  @Query(
    value = """
    SELECT p.* FROM products p
    LEFT JOIN stock_levels sl ON sl.product_id = p.id
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
      AND EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0)
    GROUP BY p.id
    ORDER BY COALESCE(SUM(sl.current_stock), 0) DESC
    """,
    countQuery = """
    SELECT COUNT(*) FROM products p
    WHERE
      (CAST(:search AS text) IS NULL
        OR unaccent(p.name) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.sku) ILIKE unaccent('%' || CAST(:search AS text) || '%')
        OR unaccent(p.internal_code) ILIKE unaccent('%' || CAST(:search AS text) || '%'))
      AND (CAST(:category AS text) IS NULL OR p.category_id = CAST(:category AS bigint))
      AND p.is_active = true
      AND EXISTS (SELECT 1 FROM stock_levels s WHERE s.product_id = p.id AND s.current_stock > 0)
    """,
    nativeQuery = true
  )
  Page<Product> findBySearchAndCategoryWithStockOrderByStockDesc(
    Pageable pageable,
    @Param("search") String search,
    @Param("category") Long category
  );
}
