package com.visco.backend.repositories;

import com.visco.backend.models.entities.Quotation;
import com.visco.backend.models.entities.QuotationStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    boolean existsByQuotationNumber(String quotationNumber);

    // Used by the comparison flow: only SUBMITTED and UNDER_REVIEW count
    // as quotable for the price comparison (DRAFT is still being filled,
    // AWARDED/REJECTED/CANCELLED are terminal).
    @Query("""
        SELECT q FROM Quotation q
        WHERE q.requisition.id = :requisitionId
          AND q.status IN (com.visco.backend.models.entities.QuotationStatus.SUBMITTED,
                           com.visco.backend.models.entities.QuotationStatus.UNDER_REVIEW,
                           com.visco.backend.models.entities.QuotationStatus.AWARDED,
                           com.visco.backend.models.entities.QuotationStatus.PARTIALLY_AWARDED,
                           com.visco.backend.models.entities.QuotationStatus.REJECTED)
        ORDER BY q.createdAt DESC
        """)
    List<Quotation> findByRequisitionIdInComparison(@Param("requisitionId") Long requisitionId);

    @Query("""
        SELECT q FROM Quotation q
        WHERE q.requisition.id = :requisitionId
        ORDER BY q.createdAt DESC
        """)
    List<Quotation> findByRequisitionIdOrderByCreatedAtDesc(@Param("requisitionId") Long requisitionId);

    @Query("""
        SELECT q FROM Quotation q
        WHERE (:requisitionId IS NULL OR q.requisition.id = :requisitionId)
          AND (:supplierId   IS NULL OR q.supplier.id   = :supplierId)
          AND (:status       IS NULL OR q.status        = :status)
          AND (:search       IS NULL OR LOWER(q.quotationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(q.notes)            LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(q.shippingConditions) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(q.paymentConditions)  LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(q.warrantyTerms)      LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Quotation> searchQuotations(
        @Param("requisitionId") Long requisitionId,
        @Param("supplierId")    Long supplierId,
        @Param("status")        QuotationStatus status,
        @Param("search")        String search,
        Pageable pageable
    );
}
