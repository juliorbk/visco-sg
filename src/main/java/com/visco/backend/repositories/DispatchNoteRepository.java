package com.visco.backend.repositories;

import com.visco.backend.models.entities.DispatchNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DispatchNoteRepository extends JpaRepository<DispatchNote, Long> {
  Page<DispatchNote> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query("SELECT dn FROM DispatchNote dn JOIN FETCH dn.warehouse JOIN FETCH dn.withdrawnBy e LEFT JOIN FETCH e.costCenter JOIN FETCH dn.createdBy")
  Page<DispatchNote> findAllWithFetch(Pageable pageable);
}
