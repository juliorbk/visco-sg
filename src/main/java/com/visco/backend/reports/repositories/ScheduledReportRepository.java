package com.visco.backend.reports.repositories;

import com.visco.backend.reports.models.entities.ScheduledReport;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, Long> {

    List<ScheduledReport> findByEnabledAndNextExecutionAtLessThanEqual(Boolean enabled, LocalDateTime dateTime);

    Page<ScheduledReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
