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
// Repository for invoice persistence with fetch-join queries.
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    // Finds all invoices linked to a purchase order.
    List<Invoice> findByPurchaseOrderId(Long purchaseOrderId);
    // Finds invoices filtered by their current status.
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);
    // Finds overdue invoices for collections or alert purposes.
    List<Invoice> findByDueDateBeforeAndStatus(LocalDate date, InvoiceStatus status);

    // Finds all invoices with purchase order and supplier eagerly loaded.
    @Query(
        value = "SELECT i FROM Invoice i JOIN FETCH i.purchaseOrder JOIN FETCH i.supplier",
        countQuery = "SELECT COUNT(i) FROM Invoice i"
    )
    Page<Invoice> findAllWithFetch(Pageable pageable);

    // Finds invoices by status with purchase order and supplier eagerly loaded.
    @Query(
        value = "SELECT i FROM Invoice i JOIN FETCH i.purchaseOrder JOIN FETCH i.supplier WHERE i.status = :status",
        countQuery = "SELECT COUNT(i) FROM Invoice i WHERE i.status = :status"
    )
    Page<Invoice> findByStatusWithFetch(@Param("status") InvoiceStatus status, Pageable pageable);

    // Finds a single invoice with all details including items and products.
    @Query("SELECT DISTINCT i FROM Invoice i JOIN FETCH i.purchaseOrder JOIN FETCH i.supplier LEFT JOIN FETCH i.items it LEFT JOIN FETCH it.product WHERE i.id = :id")
    Optional<Invoice> findByIdDetailed(@Param("id") Long id);

    // Finds all invoices for a purchase order with items and products eagerly loaded.
    @Query("SELECT i FROM Invoice i JOIN FETCH i.purchaseOrder JOIN FETCH i.supplier LEFT JOIN FETCH i.items it LEFT JOIN FETCH it.product WHERE i.purchaseOrder.id = :orderId")
    List<Invoice> findByPurchaseOrderIdWithFetch(@Param("orderId") Long orderId);

    @Query(value = "SELECT nextval('invoice_seq')", nativeQuery = true)
    Long getNextInvoiceSequence();
}
