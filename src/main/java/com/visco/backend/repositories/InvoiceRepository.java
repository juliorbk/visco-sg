package com.visco.backend.repositories;

import com.visco.backend.models.entities.Invoice;
import com.visco.backend.models.entities.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPurchaseOrderId(Long purchaseOrderId);
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);
    List<Invoice> findByDueDateBeforeAndStatus(LocalDate date, InvoiceStatus status);
}
