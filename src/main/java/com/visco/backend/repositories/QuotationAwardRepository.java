package com.visco.backend.repositories;

import com.visco.backend.models.entities.QuotationAward;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuotationAwardRepository extends JpaRepository<QuotationAward, Long> {

    Optional<QuotationAward> findByRequisitionItemId(Long requisitionItemId);

    List<QuotationAward> findByRequisitionId(Long requisitionId);

    // Sum of awarded subtotals for one supplier within one requisition.
    // Used by the comparison summary block to show how much of the award
    // went to each supplier.
    @Query("""
        SELECT COALESCE(SUM(qa.awardedSubtotal), 0)
        FROM QuotationAward qa
        WHERE qa.requisition.id = :requisitionId
          AND qa.awardedSupplier.id = :supplierId
        """)
    java.math.BigDecimal sumAwardedSubtotalByRequisitionAndSupplier(
        @Param("requisitionId") Long requisitionId,
        @Param("supplierId")    Long supplierId
    );

    void deleteByRequisitionId(Long requisitionId);
}
