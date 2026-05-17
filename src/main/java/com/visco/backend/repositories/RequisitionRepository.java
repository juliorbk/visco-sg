package com.visco.backend.repositories;

import com.visco.backend.models.entities.Requisition;
import com.visco.backend.models.entities.RequisitionStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequisitionRepository extends JpaRepository<Requisition, Long> {
    Page<Requisition> findByStatus(RequisitionStatus status, Pageable pageable);
    List<Requisition> findByStatus(RequisitionStatus status);
    Page<Requisition> findByRequestedById(java.util.UUID requestedById, Pageable pageable);
}
