package com.visco.backend.repositories;

import com.visco.backend.models.entities.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {
  Optional<Product> findByInternalCode(String internalCode);

  Optional<Product> findBySku(String sku);

  Optional<Product> findBySapCode(String sapCode);

  // Repository method automatically supports pagination
  Page<Product> findAll(Pageable pageable);

  @Query(
    "SELECT COUNT(DISTINCT p) FROM Product p " +
      "JOIN StockLevel s ON s.product.id = p.id " +
      "WHERE s.currentStock <= p.reorderPoint AND p.active = true"
  )
  Long countProductsBelowReorderPoint();

  // Productos bajo reorder point con su stock actual
  @Query(
    "SELECT p FROM Product p " +
      "JOIN StockLevel s ON s.product.id = p.id " +
      "WHERE s.currentStock <= p.reorderPoint AND p.active = true " +
      "ORDER BY s.currentStock ASC"
  )
  List<Product> findProductsBelowReorderPoint();

  // Total de unidades en inventario
  @Query("SELECT COALESCE(SUM(s.currentStock), 0) FROM StockLevel s")
  BigDecimal getTotalInventoryUnits();

  @Query(
    "SELECT p.id as productId, p.name as productName, p.sku as sku, " +
      "COALESCE(SUM(s.currentStock), 0) as currentStock, " +
      "p.reorderPoint as reorderPoint " +
      "FROM Product p JOIN StockLevel s ON s.product.id = p.id " +
      "WHERE s.currentStock <= p.reorderPoint AND p.active = true " +
      "GROUP BY p.id, p.name, p.sku, p.reorderPoint " +
      "ORDER BY currentStock ASC"
  )
  List<CriticalProductProjection> findCriticalInventory();

  @Query(
    value = "SELECT nextval('product_internal_code_seq')",
    nativeQuery = true
  )
  Long getNextInternalCodeSequence();

  interface CriticalProductProjection {
    Long getProductId();
    String getProductName();
    String getSku();
    BigDecimal getCurrentStock();
    BigDecimal getReorderPoint();
  }
}
