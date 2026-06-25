package com.visco.backend.repositories;

import com.visco.backend.models.entities.GoodReceipt;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodReceiptRepository
  extends JpaRepository<GoodReceipt, Long>
{
  List<GoodReceipt> findByPurchaseOrderId(Long purchaseOrderId);

  Page<GoodReceipt> findAll(Pageable pageable);

  @Query(
    value = "SELECT DISTINCT gr FROM GoodReceipt gr JOIN FETCH gr.purchaseOrder po LEFT JOIN FETCH po.destinationWarehouse dw LEFT JOIN FETCH dw.responsibleUser LEFT JOIN FETCH po.supplier LEFT JOIN FETCH gr.receivedBy LEFT JOIN FETCH gr.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.location",
    countQuery = "SELECT COUNT(gr) FROM GoodReceipt gr"
  )
  Page<GoodReceipt> findAllWithFetch(Pageable pageable);

  @Query(
    value = "SELECT DISTINCT gr FROM GoodReceipt gr " +
      "JOIN FETCH gr.purchaseOrder po " +
      "LEFT JOIN FETCH po.destinationWarehouse dw " +
      "LEFT JOIN FETCH dw.responsibleUser " +
      "LEFT JOIN FETCH po.supplier " +
      "LEFT JOIN FETCH gr.receivedBy " +
      "LEFT JOIN FETCH gr.items i " +
      "LEFT JOIN FETCH i.product " +
      "LEFT JOIN FETCH i.location " +
      "WHERE CAST(:search AS string) IS NULL " +
      "OR LOWER(gr.receiptNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
      "OR LOWER(gr.notes) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
      "OR LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))",
    countQuery = "SELECT COUNT(gr) FROM GoodReceipt gr " +
      "LEFT JOIN gr.purchaseOrder po " +
      "WHERE CAST(:search AS string) IS NULL " +
      "OR LOWER(gr.receiptNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
      "OR LOWER(gr.notes) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
      "OR LOWER(po.orderNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
  )
  Page<GoodReceipt> findAllWithSearch(@Param("search") String search, Pageable pageable);

  @Query("SELECT gr FROM GoodReceipt gr JOIN FETCH gr.purchaseOrder po LEFT JOIN FETCH po.destinationWarehouse dw LEFT JOIN FETCH dw.responsibleUser LEFT JOIN FETCH po.supplier LEFT JOIN FETCH gr.receivedBy LEFT JOIN FETCH gr.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.location WHERE gr.purchaseOrder.id = :orderId")
  List<GoodReceipt> findByPurchaseOrderIdWithFetch(@Param("orderId") Long orderId);

  @Query("SELECT gr FROM GoodReceipt gr JOIN FETCH gr.purchaseOrder po LEFT JOIN FETCH po.destinationWarehouse dw LEFT JOIN FETCH dw.responsibleUser LEFT JOIN FETCH po.supplier LEFT JOIN FETCH gr.receivedBy LEFT JOIN FETCH gr.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.location WHERE gr.id = :id")
  Optional<GoodReceipt> findByIdDetailed(@Param("id") Long id);

  @Query("SELECT gri.product.id as productId, SUM(gri.receivedQuantity) as totalReceived "
      + "FROM GoodReceipt gr JOIN gr.items gri "
      + "WHERE gr.purchaseOrder.id = :orderId "
      + "GROUP BY gri.product.id")
  List<ReceivedQuantityProjection> getTotalReceivedByOrder(@Param("orderId") Long orderId);

  /**
   * Batch counterpart of {@link #getTotalReceivedByOrder(Long)}: returns the
   * cumulative received quantity per (PO, product) pair for the given set of
   * purchase orders in a single round-trip. Used by the daily receptions
   * report to compute PO-level completeness when a PO was received in
   * multiple partial deliveries.
   */
  @Query("SELECT gri.goodReceipt.purchaseOrder.id as orderId, "
      + "       gri.product.id as productId, "
      + "       SUM(gri.receivedQuantity) as totalReceived "
      + "FROM GoodReceiptItem gri "
      + "WHERE gri.goodReceipt.purchaseOrder.id IN :orderIds "
      + "GROUP BY gri.goodReceipt.purchaseOrder.id, gri.product.id")
  List<OrderProductReceivedProjection> getCumulativeReceivedForOrders(
    @Param("orderIds") List<Long> orderIds
  );

  @Query(value = "SELECT nextval('receipt_seq')", nativeQuery = true)
  Long getNextReceiptSequence();

  @Query("""
    SELECT DISTINCT gr FROM GoodReceipt gr
      JOIN FETCH gr.purchaseOrder po
      JOIN FETCH po.supplier
      JOIN FETCH gr.destinationWarehouse
      LEFT JOIN FETCH gr.receivedBy
      LEFT JOIN FETCH gr.items i
      LEFT JOIN FETCH i.product
      LEFT JOIN FETCH po.items
    WHERE gr.destinationWarehouse.id = :warehouseId
      AND gr.receivedAt >= :start
      AND gr.receivedAt <  :end
    ORDER BY gr.receivedAt ASC
  """)
  List<GoodReceipt> findForDailyReport(
    @Param("warehouseId") Long warehouseId,
    @Param("start") LocalDateTime start,
    @Param("end") LocalDateTime end);

  interface ReceivedQuantityProjection {
    Long getProductId();
    BigDecimal getTotalReceived();
  }

  interface OrderProductReceivedProjection {
    Long getOrderId();
    Long getProductId();
    BigDecimal getTotalReceived();
  }
}
