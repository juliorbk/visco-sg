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
    value = "SELECT dn FROM DispatchNote dn JOIN FETCH dn.warehouse JOIN FETCH dn.withdrawnBy e LEFT JOIN FETCH e.costCenter JOIN FETCH dn.createdBy",
    countQuery = "SELECT COUNT(dn) FROM DispatchNote dn"
  )
  Page<DispatchNote> findAllWithFetch(Pageable pageable);

  @Query(
    value = """
    SELECT dn.* FROM dispatch_notes dn
    WHERE CAST(:search AS text) IS NULL
      OR dn.dispatch_number ILIKE '%' || :search || '%'
      OR dn.notes ILIKE '%' || :search || '%'
    """,
    countQuery = """
    SELECT COUNT(*) FROM dispatch_notes dn
    WHERE CAST(:search AS text) IS NULL
      OR dn.dispatch_number ILIKE '%' || :search || '%'
      OR dn.notes ILIKE '%' || :search || '%'
    """,
    nativeQuery = true
  )
  Page<DispatchNote> findAllWithSearch(@Param("search") String search, Pageable pageable);

  @Query("SELECT dn FROM DispatchNote dn JOIN FETCH dn.warehouse JOIN FETCH dn.withdrawnBy e LEFT JOIN FETCH e.costCenter JOIN FETCH dn.createdBy LEFT JOIN FETCH dn.items i LEFT JOIN FETCH i.product WHERE dn.id = :id")
  Optional<DispatchNote> findByIdDetailed(@Param("id") Long id);

  @Query(value = "SELECT nextval('dispatch_seq')", nativeQuery = true)
  Long getNextDispatchSequence();
}
