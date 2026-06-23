package com.visco.backend.repositories;

import com.visco.backend.models.dtos.ProductPurchaseOrderSummary;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository
  extends JpaRepository<PurchaseOrder, Long>
{
  @Query(
    "SELECT o FROM PurchaseOrder o JOIN FETCH o.supplier JOIN FETCH o.createdBy LEFT JOIN FETCH o.approvedBy LEFT JOIN FETCH o.destinationWarehouse LEFT JOIN FETCH o.requisition"
  )
  Page<PurchaseOrder> findAllWithFetch(Pageable pageable);

  @Query(
    value = """
    SELECT new com.visco.backend.models.dtos.ProductPurchaseOrderSummary(
      o.id, o.orderNumber, o.supplier.name, o.createdAt, i.quantity, i.unitPrice
    )
    FROM PurchaseOrder o
    JOIN o.items i
    WHERE i.product.id = :productId
    ORDER BY o.createdAt DESC
    """,
    countQuery = """
    SELECT COUNT(o)
    FROM PurchaseOrder o
    JOIN o.items i
    WHERE i.product.id = :productId
    """
  )
  Page<ProductPurchaseOrderSummary> findProductPurchaseOrders(
    @Param("productId") Long productId,
    Pageable pageable
  );

  @Query(
    "SELECT o FROM PurchaseOrder o " +
      "JOIN FETCH o.supplier " +
      "JOIN FETCH o.createdBy " +
      "LEFT JOIN FETCH o.approvedBy " +
      "LEFT JOIN FETCH o.destinationWarehouse " +
      "LEFT JOIN FETCH o.requisition " +
      "LEFT JOIN FETCH o.items i " +
      "LEFT JOIN FETCH i.product " +
      "WHERE o.id = :id"
  )
  Optional<PurchaseOrder> findByIdDetailed(@Param("id") Long id);

  @Query(
    "SELECT o FROM PurchaseOrder o " +
      "LEFT JOIN FETCH o.items i " +
      "LEFT JOIN FETCH i.product " +
      "WHERE o.id = :id"
  )
  Optional<PurchaseOrder> findByIdWithItems(@Param("id") Long id);

  @Query(
    "SELECT o.status as status, COUNT(o) as count FROM PurchaseOrder o GROUP BY o.status"
  )
  List<OrderStatusCountProjection> countByStatus();

  interface OrderStatusCountProjection {
    PurchaseOrderStatus getStatus();
    Long getCount();
  }

  @Query(
    "SELECT o FROM PurchaseOrder o " +
      "JOIN FETCH o.supplier " +
      "JOIN FETCH o.createdBy " +
      "ORDER BY o.createdAt DESC"
  )
  List<PurchaseOrder> findRecentOrders(Pageable pageable);

  @Query(
    value = """
    SELECT
      DATE_TRUNC('month', o.created_at) AS month,
      SUM(i.unit_price * i.quantity) AS total
    FROM purchase_orders o
    JOIN purchase_order_items i ON i.purchase_order_id = o.id
    WHERE o.created_at >= :from
    GROUP BY DATE_TRUNC('month', o.created_at)
    ORDER BY month ASC
    """,
    nativeQuery = true
  )
  List<MonthlySpendingProjection> getMonthlySpending(
    @Param("from") LocalDateTime from
  );

  @Query(
    "SELECT COALESCE(SUM(i.unitPrice * i.quantity), 0) " +
      "FROM PurchaseOrder o JOIN o.items i " +
      "WHERE o.createdAt >= :from"
  )
  BigDecimal getTotalSpendingSince(@Param("from") LocalDateTime from);

  @Query(
    value = """
    SELECT
      s.id AS supplierId,
      s.name AS supplierName,
      DATE_TRUNC('month', o.created_at) AS month,
      COUNT(o.id) AS totalOrders,
      SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) AS deliveredOrders,
      SUM(i.unit_price * i.quantity) AS totalSpend
    FROM purchase_orders o
    JOIN suppliers s ON s.id = o.supplier_id
    JOIN purchase_order_items i ON i.purchase_order_id = o.id
    WHERE o.created_at >= :from
    GROUP BY s.id, s.name, DATE_TRUNC('month', o.created_at)
    ORDER BY month ASC, s.name ASC
    """,
    nativeQuery = true
  )
  List<SupplierPerformanceProjection> getSupplierPerformance(
    @Param("from") LocalDateTime from
  );

  interface SupplierPerformanceProjection {
    Long getSupplierId();
    String getSupplierName();
    Object getMonth();
    Long getTotalOrders();
    Long getDeliveredOrders();
    BigDecimal getTotalSpend();
  }

  @Query(
    "SELECT p.category.name as categoryName, " +
      "SUM(i.unitPrice * i.quantity) as total " +
      "FROM PurchaseOrder o JOIN o.items i JOIN i.product p " +
      "WHERE o.createdAt >= :from " +
      "GROUP BY p.category.name"
  )
  List<CategorySpendingProjection> getSpendingByCategory(
    @Param("from") LocalDateTime from
  );

  @Query("SELECT COUNT(o) FROM PurchaseOrder o WHERE o.status = 'DELIVERED'")
  Long countDeliveredOrders();

  interface MonthlySpendingProjection {
    Object getMonth();
    BigDecimal getTotal();
  }

  interface CategorySpendingProjection {
    String getCategoryName();
    BigDecimal getTotal();
  }

  @Query(
    value = """
    SELECT
      DATE_TRUNC('month', o.created_at) AS month,
      s.id AS supplierId,
      COUNT(o.id) AS totalOrders,
      SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) AS deliveredOrders
    FROM purchase_orders o
    JOIN suppliers s ON s.id = o.supplier_id
    WHERE o.created_at >= :from
    GROUP BY DATE_TRUNC('month', o.created_at), s.id
    ORDER BY month ASC
    """,
    nativeQuery = true
  )
  List<MonthlySupplierStatsProjection> getMonthlySupplierStats(
    @Param("from") LocalDateTime from
  );

  interface MonthlySupplierStatsProjection {
    Object getMonth();
    Long getSupplierId();
    Long getTotalOrders();
    Long getDeliveredOrders();
  }
}
