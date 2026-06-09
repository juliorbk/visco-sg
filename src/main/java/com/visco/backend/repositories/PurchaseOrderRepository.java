package com.visco.backend.repositories;

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

// Repository for purchase orders with analytics and projections.
public interface PurchaseOrderRepository
  extends JpaRepository<PurchaseOrder, Long>
{
  // Finds all purchase orders with related entities eagerly loaded.
  @Query(
    "SELECT o FROM PurchaseOrder o JOIN FETCH o.supplier JOIN FETCH o.createdBy LEFT JOIN FETCH o.approvedBy LEFT JOIN FETCH o.destinationWarehouse LEFT JOIN FETCH o.requisition"
  )
  Page<PurchaseOrder> findAllWithFetch(Pageable pageable);

  // Finds a purchase order with all details including items and products.
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

  // Finds a purchase order with its line items and products (no outer relations).
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

  // Correcto:
  @Query(
    "SELECT o FROM PurchaseOrder o " +
      "JOIN FETCH o.supplier " +
      "JOIN FETCH o.createdBy " +
      "ORDER BY o.createdAt DESC"
  )
  List<PurchaseOrder> findRecentOrders(Pageable pageable);

  // Gastos por mes (últimos 6 meses)
  @Query(
    "SELECT FUNCTION('DATE_TRUNC', 'month', o.createdAt) as month, " +
      "SUM(i.unitPrice * i.quantity) as total " +
      "FROM PurchaseOrder o JOIN o.items i " +
      "WHERE o.createdAt >= :from " +
      "GROUP BY FUNCTION('DATE_TRUNC', 'month', o.createdAt) " +
      "ORDER BY month ASC"
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
    "SELECT s.id as supplierId, s.name as supplierName, " +
      "FUNCTION('DATE_TRUNC', 'month', o.createdAt) as month, " +
      "COUNT(o) as totalOrders, " +
      "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) as deliveredOrders, " +
      "SUM(i.unitPrice * i.quantity) as totalSpend " +
      "FROM PurchaseOrder o JOIN o.supplier s JOIN o.items i " +
      "WHERE o.createdAt >= :from " +
      "GROUP BY s.id, s.name, FUNCTION('DATE_TRUNC', 'month', o.createdAt) " +
      "ORDER BY month ASC, s.name ASC"
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

  // Gastos por categoría
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

  // Tasa de cumplimiento
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
    "SELECT FUNCTION('DATE_TRUNC', 'month', o.createdAt) as month, " +
      "s.id as supplierId, " +
      "COUNT(o) as totalOrders, " +
      "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) as deliveredOrders " +
      "FROM PurchaseOrder o JOIN o.supplier s " +
      "WHERE o.createdAt >= :from " +
      "GROUP BY FUNCTION('DATE_TRUNC', 'month', o.createdAt), s.id " +
      "ORDER BY month ASC"
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

  @Query(value = "SELECT nextval('order_seq')", nativeQuery = true)
  Long getNextOrderSequence();
}
