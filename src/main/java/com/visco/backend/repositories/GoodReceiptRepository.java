package com.visco.backend.repositories;

import com.visco.backend.models.entities.GoodReceipt;
import java.math.BigDecimal;
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
  // Busca todas las recepciones asociadas a una orden de compra
  // Útil para acumular cantidades recibidas y determinar si la orden está
  // completa
  List<GoodReceipt> findByPurchaseOrderId(Long purchaseOrderId);

  Page<GoodReceipt> findAll(Pageable pageable);

  @Query("SELECT gr FROM GoodReceipt gr JOIN FETCH gr.purchaseOrder po LEFT JOIN FETCH po.destinationWarehouse LEFT JOIN FETCH gr.receivedBy")
  Page<GoodReceipt> findAllWithFetch(Pageable pageable);

  @Query(
    value = """
    SELECT gr FROM GoodReceipt gr
    JOIN FETCH gr.purchaseOrder po
    LEFT JOIN FETCH po.destinationWarehouse
    LEFT JOIN FETCH gr.receivedBy
    WHERE (:search IS NULL
      OR LOWER(gr.receiptNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(gr.notes) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(po.orderNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """,
    countQuery = """
    SELECT COUNT(gr) FROM GoodReceipt gr
    JOIN gr.purchaseOrder po
    WHERE (:search IS NULL
      OR LOWER(gr.receiptNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(gr.notes) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(po.orderNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """
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

  interface ReceivedQuantityProjection {
    Long getProductId();
    BigDecimal getTotalReceived();
  }
}
