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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByActiveTrue();

    long countByStatusAndActiveTrue(ReportStatus status);

    @Query("SELECT r.type, COUNT(r) FROM Report r WHERE r.active = true GROUP BY r.type")
    List<Object[]> countActiveByType();

    @Query("SELECT r.status, COUNT(r) FROM Report r WHERE r.active = true GROUP BY r.status")
    List<Object[]> countActiveByStatus();

    @Query("SELECT FUNCTION('DATE_TRUNC', 'month', r.createdAt), COUNT(r) "
            + "FROM Report r WHERE r.active = true AND r.createdAt >= :since "
            + "GROUP BY FUNCTION('DATE_TRUNC', 'month', r.createdAt) "
            + "ORDER BY FUNCTION('DATE_TRUNC', 'month', r.createdAt)")
    List<Object[]> countByMonthSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(r.recordCount), 0) FROM Report r WHERE r.status = 'COMPLETED' AND r.active = true")
    long sumCompletedRecordCount();
}
