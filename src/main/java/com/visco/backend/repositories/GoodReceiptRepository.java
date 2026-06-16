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
    value = "SELECT gr FROM GoodReceipt gr JOIN FETCH gr.purchaseOrder po LEFT JOIN FETCH po.destinationWarehouse LEFT JOIN FETCH gr.receivedBy",
    countQuery = "SELECT COUNT(gr) FROM GoodReceipt gr"
  )
  Page<GoodReceipt> findAllWithFetch(Pageable pageable);

  @Query(
    value = """
    SELECT gr.* FROM good_receipts gr
    JOIN purchase_orders po ON po.id = gr.purchase_order_id
    WHERE CAST(:search AS text) IS NULL
      OR gr.receipt_number ILIKE '%' || :search || '%'
      OR gr.notes ILIKE '%' || :search || '%'
      OR po.order_number ILIKE '%' || :search || '%'
    """,
    countQuery = """
    SELECT COUNT(*) FROM good_receipts gr
    JOIN purchase_orders po ON po.id = gr.purchase_order_id
    WHERE CAST(:search AS text) IS NULL
      OR gr.receipt_number ILIKE '%' || :search || '%'
      OR gr.notes ILIKE '%' || :search || '%'
      OR po.order_number ILIKE '%' || :search || '%'
    """,
    nativeQuery = true
  )
  Page<GoodReceipt> findAllWithSearch(@Param("search") String search, Pageable pageable);

  @Query("SELECT gr FROM GoodReceipt gr JOIN FETCH gr.purchaseOrder po LEFT JOIN FETCH po.destinationWarehouse LEFT JOIN FETCH gr.receivedBy LEFT JOIN FETCH gr.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.location WHERE gr.purchaseOrder.id = :orderId")
  List<GoodReceipt> findByPurchaseOrderIdWithFetch(@Param("orderId") Long orderId);

  @Query("SELECT gr FROM GoodReceipt gr JOIN FETCH gr.purchaseOrder po LEFT JOIN FETCH po.destinationWarehouse LEFT JOIN FETCH gr.receivedBy LEFT JOIN FETCH gr.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH i.location WHERE gr.id = :id")
  Optional<GoodReceipt> findByIdDetailed(@Param("id") Long id);

  @Query("SELECT gri.product.id as productId, SUM(gri.receivedQuantity) as totalReceived "
      + "FROM GoodReceipt gr JOIN gr.items gri "
      + "WHERE gr.purchaseOrder.id = :orderId "
      + "GROUP BY gri.product.id")
  List<ReceivedQuantityProjection> getTotalReceivedByOrder(@Param("orderId") Long orderId);

  @Query(value = "SELECT nextval('receipt_seq')", nativeQuery = true)
  Long getNextReceiptSequence();

  @Query("""
    SELECT gr FROM GoodReceipt gr
      JOIN FETCH gr.purchaseOrder po
      JOIN FETCH po.supplier
      JOIN FETCH gr.destinationWarehouse
      LEFT JOIN FETCH gr.receivedBy
      LEFT JOIN FETCH gr.items i
      LEFT JOIN FETCH i.product
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
}
