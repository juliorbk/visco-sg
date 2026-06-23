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

  // ─────────────────────────────────────────────────────────────
  // Multi-PO-per-requisition helpers
  // ─────────────────────────────────────────────────────────────

  /**
   * Returns the sum of quantities already awarded to a specific
   * RequisitionItem across every non-cancelled, non-rejected purchase
   * order. Returns 0 when no award exists yet.
   */
  @Query(
    "SELECT COALESCE(SUM(i.quantity), 0) " +
      "FROM PurchaseOrderItem i " +
      "WHERE i.requisitionItem.id = :requisitionItemId " +
      "AND i.purchaseOrder.status NOT IN " +
      "(com.visco.backend.models.entities.PurchaseOrderStatus.CANCELLED, " +
      " com.visco.backend.models.entities.PurchaseOrderStatus.REJECTED)"
  )
  BigDecimal sumAwardedQuantityByRequisitionItemId(
    @Param("requisitionItemId") Long requisitionItemId
  );

  /**
   * Returns the awarded quantity for every RequisitionItem that belongs
   * to the given requisition, in a single query. Items with no award yet
   * are omitted from the result (callers should treat missing keys as 0).
   */
  @Query(
    "SELECT i.requisitionItem.id AS requisitionItemId, " +
      "       COALESCE(SUM(i.quantity), 0) AS awardedQuantity " +
      "FROM PurchaseOrderItem i " +
      "WHERE i.requisitionItem.id IN " +
      "  (SELECT ri.id FROM RequisitionItem ri WHERE ri.requisition.id = :requisitionId) " +
      "AND i.purchaseOrder.status NOT IN " +
      "  (com.visco.backend.models.entities.PurchaseOrderStatus.CANCELLED, " +
      "   com.visco.backend.models.entities.PurchaseOrderStatus.REJECTED) " +
      "GROUP BY i.requisitionItem.id"
  )
  List<AwardedQuantityProjection> sumAwardedByRequisitionId(
    @Param("requisitionId") Long requisitionId
  );

  /**
   * Returns every PO linked to a given requisition (lightweight, no items).
   */
  @Query(
    "SELECT o FROM PurchaseOrder o " +
      "JOIN FETCH o.supplier " +
      "WHERE o.requisition.id = :requisitionId " +
      "ORDER BY o.createdAt DESC"
  )
  List<PurchaseOrder> findByRequisitionIdOrdered(
    @Param("requisitionId") Long requisitionId
  );

  interface AwardedQuantityProjection {
    Long getRequisitionItemId();
    BigDecimal getAwardedQuantity();
  }

  /**
   * Bulk award aggregation for a list of requisitions, used by the
   * "requisition fulfillment" report. Returns one row per (requisition,
   * item) pair.
   */
  @Query(
    "SELECT i.requisitionItem.requisition.id AS requisitionId, " +
      "       i.requisitionItem.id AS requisitionItemId, " +
      "       COALESCE(SUM(i.quantity), 0) AS awardedQuantity " +
      "FROM PurchaseOrderItem i " +
      "WHERE i.requisitionItem IS NOT NULL " +
      "AND i.requisitionItem.requisition.id IN :requisitionIds " +
      "AND i.purchaseOrder.status NOT IN " +
      "  (com.visco.backend.models.entities.PurchaseOrderStatus.CANCELLED, " +
      "   com.visco.backend.models.entities.PurchaseOrderStatus.REJECTED) " +
      "GROUP BY i.requisitionItem.requisition.id, i.requisitionItem.id"
  )
  List<BulkAwardedProjection> sumAwardedByRequisitionIds(
    @Param("requisitionIds") List<Long> requisitionIds
  );

  interface BulkAwardedProjection {
    Long getRequisitionId();
    Long getRequisitionItemId();
    BigDecimal getAwardedQuantity();
  }

  /**
   * Per-PO breakdown of awarded quantities for a single requisition.
   * One row per (PO, line) pair, with supplier + status info joined.
   */
  @Query(
    "SELECT i.purchaseOrder.id AS purchaseOrderId, " +
      "       i.purchaseOrder.orderNumber AS orderNumber, " +
      "       i.purchaseOrder.supplier.name AS supplierName, " +
      "       i.purchaseOrder.status AS status, " +
      "       i.requisitionItem.id AS requisitionItemId, " +
      "       i.quantity AS quantity, " +
      "       i.unitPrice AS unitPrice, " +
      "       i.purchaseOrder.createdAt AS createdAt " +
      "FROM PurchaseOrderItem i " +
      "WHERE i.requisitionItem IS NOT NULL " +
      "AND i.requisitionItem.requisition.id = :requisitionId " +
      "AND i.purchaseOrder.status NOT IN " +
      "  (com.visco.backend.models.entities.PurchaseOrderStatus.CANCELLED, " +
      "   com.visco.backend.models.entities.PurchaseOrderStatus.REJECTED) " +
      "ORDER BY i.purchaseOrder.createdAt DESC"
  )
  List<AwardedPoLineProjection> findAwardedLinesByRequisitionId(
    @Param("requisitionId") Long requisitionId
  );

  interface AwardedPoLineProjection {
    Long getPurchaseOrderId();
    String getOrderNumber();
    String getSupplierName();
    PurchaseOrderStatus getStatus();
    Long getRequisitionItemId();
    BigDecimal getQuantity();
    BigDecimal getUnitPrice();
    LocalDateTime getCreatedAt();
  }

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
