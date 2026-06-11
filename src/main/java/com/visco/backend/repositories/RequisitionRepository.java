package com.visco.backend.repositories;

import com.visco.backend.models.entities.Requisition;
import com.visco.backend.models.entities.RequisitionItem;
import com.visco.backend.models.entities.RequisitionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
// Repository for requisitions with eager-fetch queries and search support.
public interface RequisitionRepository extends JpaRepository<Requisition, Long> {
    Page<Requisition> findByStatus(RequisitionStatus status, Pageable pageable);
    List<Requisition> findByStatus(RequisitionStatus status);
    Page<Requisition> findByRequestedById(java.util.UUID requestedById, Pageable pageable);

    // Finds all requisitions with requester, cost center, and approver eagerly loaded.
    @Query("SELECT r FROM Requisition r JOIN FETCH r.requestedBy JOIN FETCH r.costCenter LEFT JOIN FETCH r.approvedBy")
    Page<Requisition> findAllWithFetch(Pageable pageable);

    // Finds requisitions by status with related entities eagerly loaded.
    @Query("SELECT r FROM Requisition r JOIN FETCH r.requestedBy JOIN FETCH r.costCenter LEFT JOIN FETCH r.approvedBy WHERE r.status = :status")
    Page<Requisition> findByStatusWithFetch(@Param("status") RequisitionStatus status, Pageable pageable);

    @Query(
        value = """
        SELECT r FROM Requisition r
        JOIN FETCH r.requestedBy
        JOIN FETCH r.costCenter
        LEFT JOIN FETCH r.approvedBy
        WHERE (CAST(:search AS text) IS NULL
            OR LOWER(r.requisitionNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
            OR LOWER(r.description) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
        """,
        countQuery = """
        SELECT COUNT(r) FROM Requisition r
        WHERE (CAST(:search AS text) IS NULL
            OR LOWER(r.requisitionNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
            OR LOWER(r.description) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
        """
    )
    Page<Requisition> findAllWithSearch(@Param("search") String search, Pageable pageable);

    @Query(
        value = """
        SELECT r FROM Requisition r
        JOIN FETCH r.requestedBy
        JOIN FETCH r.costCenter
        LEFT JOIN FETCH r.approvedBy
        WHERE r.status = :status
          AND (CAST(:search AS text) IS NULL
            OR LOWER(r.requisitionNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
            OR LOWER(r.description) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
        """,
        countQuery = """
        SELECT COUNT(r) FROM Requisition r
        WHERE r.status = :status
          AND (CAST(:search AS text) IS NULL
            OR LOWER(r.requisitionNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
            OR LOWER(r.description) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
        """
    )
    Page<Requisition> findByStatusWithSearch(
        @Param("status") RequisitionStatus status,
        @Param("search") String search,
        Pageable pageable
    );

    // Finds requisitions requested by a specific user with related entities eagerly loaded.
    @Query("SELECT r FROM Requisition r JOIN FETCH r.requestedBy JOIN FETCH r.costCenter LEFT JOIN FETCH r.approvedBy WHERE r.requestedBy.id = :requestedById")
    Page<Requisition> findByRequestedByIdWithFetch(@Param("requestedById") java.util.UUID requestedById, Pageable pageable);

    // Finds a single requisition with all details including items and products.
    @Query("""
            SELECT r FROM Requisition r
            JOIN FETCH r.requestedBy
            JOIN FETCH r.costCenter
            LEFT JOIN FETCH r.approvedBy
            LEFT JOIN FETCH r.items i
            LEFT JOIN FETCH i.product
            WHERE r.id = :id
            """)
    Optional<Requisition> findByIdDetailed(@Param("id") Long id);

    // Deletes all line items for a requisition (used before re-adding items on update).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RequisitionItem ri WHERE ri.requisition.id = :requisitionId")
    void deleteItemsByRequisitionId(@Param("requisitionId") Long requisitionId);

}
