package com.visco.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visco.backend.models.entities.GoodReceipt;

public interface GoodReceiptRepository extends JpaRepository<GoodReceipt, Long> {
  // Busca todas las recepciones asociadas a una orden de compra
  // Útil para acumular cantidades recibidas y determinar si la orden está completa
  List<GoodReceipt> findByPurchaseOrderId(Long purchaseOrderId);
}
