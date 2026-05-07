package com.visco.backend.repositories;

import com.visco.backend.models.entities.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository
  extends JpaRepository<PurchaseOrder, Long> {}
