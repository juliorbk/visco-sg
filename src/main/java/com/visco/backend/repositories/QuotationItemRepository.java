package com.visco.backend.repositories;

import com.visco.backend.models.entities.QuotationItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    List<QuotationItem> findByQuotationId(Long quotationId);

    // Used by the comparison service: collect all items from all quotations
    // of a requisition, eager-loading the parent quotation for currency lookup.
    @Query("""
        SELECT qi FROM QuotationItem qi
        JOIN FETCH qi.quotation q
        WHERE q.requisition.id = :requisitionId
        """)
    List<QuotationItem> findAllByRequisitionId(@Param("requisitionId") Long requisitionId);

    void deleteByQuotationId(Long quotationId);
}
