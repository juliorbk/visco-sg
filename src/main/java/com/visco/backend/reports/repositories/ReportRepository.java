package com.visco.backend.reports.repositories;

import com.visco.backend.reports.models.entities.Report;
import com.visco.backend.reports.models.enums.ReportStatus;
import com.visco.backend.reports.models.enums.ReportType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findAll(Pageable pageable);

    Page<Report> findByType(ReportType type, Pageable pageable);

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    Page<Report> findByTypeAndStatus(ReportType type, ReportStatus status, Pageable pageable);

    List<Report> findByStatusAndCreatedAtBefore(ReportStatus status, LocalDateTime date);

    Optional<Report> findByIdAndCreatedBy(Long id, String createdBy);

    List<Report> findByCreatedAtBeforeAndActiveTrue(LocalDateTime date);
}
