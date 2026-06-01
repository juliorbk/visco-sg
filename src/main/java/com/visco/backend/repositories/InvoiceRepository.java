package com.visco.backend.repositories;

import com.visco.backend.models.entities.Invoice;
import com.visco.backend.models.entities.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPurchaseOrderId(Long purchaseOrderId);
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);
    List<Invoice> findByDueDateBeforeAndStatus(LocalDate date, InvoiceStatus status);

    @Query("SELECT i FROM Invoice i JOIN FETCH i.purchaseOrder JOIN FETCH i.supplier")
    Page<Invoice> findAllWithFetch(Pageable pageable);

    @Query("SELECT i FROM Invoice i JOIN FETCH i.purchaseOrder JOIN FETCH i.supplier WHERE i.status = :status")
    Page<Invoice> findByStatusWithFetch(@Param("status") InvoiceStatus status, Pageable pageable);

    @Query("SELECT i FROM Invoice i JOIN FETCH i.purchaseOrder JOIN FETCH i.supplier LEFT JOIN FETCH i.items it LEFT JOIN FETCH it.product WHERE i.id = :id")
    Optional<Invoice> findByIdDetailed(@Param("id") Long id);

    @Query("SELECT i FROM Invoice i JOIN FETCH i.purchaseOrder JOIN FETCH i.supplier LEFT JOIN FETCH i.items it LEFT JOIN FETCH it.product WHERE i.purchaseOrder.id = :orderId")
    List<Invoice> findByPurchaseOrderIdWithFetch(@Param("orderId") Long orderId);
}
