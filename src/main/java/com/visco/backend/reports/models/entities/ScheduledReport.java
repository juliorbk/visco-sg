package com.visco.backend.reports.models.entities;

import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportFrequency;
import com.visco.backend.reports.models.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "scheduled_reports", indexes = {
    @Index(name = "idx_scheduled_enabled", columnList = "enabled"),
    @Index(name = "idx_scheduled_next_execution", columnList = "next_execution_at"),
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ScheduledReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportFrequency frequency;

    @Column(name = "recipient_emails", columnDefinition = "TEXT")
    private String recipientEmails;

    @Column(name = "filter_config", columnDefinition = "jsonb")
    private String filterConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportFormat format;

    @Column(name = "schedule_time")
    private LocalTime scheduleTime;

    @Column(name = "schedule_day_of_week")
    private DayOfWeek scheduleDayOfWeek;

    @Column(name = "schedule_day")
    private Integer scheduleDay;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "next_execution_at")
    private LocalDateTime nextExecutionAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
