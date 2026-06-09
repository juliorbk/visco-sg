package com.visco.backend.repositories;

import com.visco.backend.models.entities.DispatchNote;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repository for dispatch note persistence with search and eager-fetch support.
public interface DispatchNoteRepository extends JpaRepository<DispatchNote, Long> {
  Page<DispatchNote> findAllByOrderByCreatedAtDesc(Pageable pageable);

  // Finds all dispatch notes with related warehouse, employee, and creator eagerly loaded.
  @Query("SELECT dn FROM DispatchNote dn JOIN FETCH dn.warehouse JOIN FETCH dn.withdrawnBy e LEFT JOIN FETCH e.costCenter JOIN FETCH dn.createdBy")
  Page<DispatchNote> findAllWithFetch(Pageable pageable);

  @Query(
    value = """
    SELECT dn FROM DispatchNote dn
    JOIN FETCH dn.warehouse
    JOIN FETCH dn.withdrawnBy e
    LEFT JOIN FETCH e.costCenter
    JOIN FETCH dn.createdBy
    WHERE (:search IS NULL
      OR LOWER(dn.dispatchNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(dn.notes) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """,
    countQuery = """
    SELECT COUNT(dn) FROM DispatchNote dn
    WHERE (:search IS NULL
      OR LOWER(dn.dispatchNumber) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
      OR LOWER(dn.notes) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """
  )
  Page<DispatchNote> findAllWithSearch(@Param("search") String search, Pageable pageable);

  // Finds a single dispatch note by ID with all details including items and products.
  @Query("SELECT dn FROM DispatchNote dn JOIN FETCH dn.warehouse JOIN FETCH dn.withdrawnBy e LEFT JOIN FETCH e.costCenter JOIN FETCH dn.createdBy LEFT JOIN FETCH dn.items i LEFT JOIN FETCH i.product WHERE dn.id = :id")
  Optional<DispatchNote> findByIdDetailed(@Param("id") Long id);

  @Query(value = "SELECT nextval('dispatch_seq')", nativeQuery = true)
  Long getNextDispatchSequence();
}
