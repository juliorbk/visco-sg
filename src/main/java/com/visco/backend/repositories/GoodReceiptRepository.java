package com.visco.backend.repositories;

import com.visco.backend.models.entities.GoodReceipt;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

  @Query(value = "SELECT nextval('receipt_seq')", nativeQuery = true)
  Long getNextReceiptSequence();
}
