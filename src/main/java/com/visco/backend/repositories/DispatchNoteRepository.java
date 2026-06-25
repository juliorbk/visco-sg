package com.visco.backend.repositories;

import com.visco.backend.models.entities.DispatchNote;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DispatchNoteRepository extends JpaRepository<DispatchNote, Long> {
  Page<DispatchNote> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query(
    value = "SELECT DISTINCT dn FROM DispatchNote dn " +
      "JOIN FETCH dn.warehouse w " +
      "LEFT JOIN FETCH w.responsibleUser " +
      "JOIN FETCH dn.withdrawnBy " +
      "JOIN FETCH dn.costCenter " +
      "JOIN FETCH dn.createdBy " +
      "LEFT JOIN FETCH dn.items i " +
      "LEFT JOIN FETCH i.product",
    countQuery = "SELECT COUNT(dn) FROM DispatchNote dn"
  )
  Page<DispatchNote> findAllWithFetch(Pageable pageable);

  @Query(
    value = "SELECT DISTINCT dn FROM DispatchNote dn " +
      "JOIN FETCH dn.warehouse w " +
      "LEFT JOIN FETCH w.responsibleUser " +
      "JOIN FETCH dn.withdrawnBy " +
      "JOIN FETCH dn.costCenter " +
      "JOIN FETCH dn.createdBy " +
      "LEFT JOIN FETCH dn.items i " +
      "LEFT JOIN FETCH i.product " +
      "WHERE CAST(:search AS string) IS NULL " +
      "OR LOWER(dn.dispatchNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
      "OR LOWER(dn.notes) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))",
    countQuery = "SELECT COUNT(dn) FROM DispatchNote dn " +
      "WHERE CAST(:search AS string) IS NULL " +
      "OR LOWER(dn.dispatchNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
      "OR LOWER(dn.notes) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
  )
  Page<DispatchNote> findAllWithSearch(@Param("search") String search, Pageable pageable);

  @Query("SELECT dn FROM DispatchNote dn JOIN FETCH dn.warehouse w LEFT JOIN FETCH w.responsibleUser JOIN FETCH dn.withdrawnBy JOIN FETCH dn.costCenter JOIN FETCH dn.createdBy LEFT JOIN FETCH dn.items i LEFT JOIN FETCH i.product WHERE dn.id = :id")
  Optional<DispatchNote> findByIdDetailed(@Param("id") Long id);

  @Query(value = "SELECT nextval('dispatch_seq')", nativeQuery = true)
  Long getNextDispatchSequence();
}
