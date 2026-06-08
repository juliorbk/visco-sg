package com.visco.backend.reports.models.dtos;

import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportFrequency;
import com.visco.backend.reports.models.enums.ReportType;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
// Response DTO with scheduled report configuration and execution state.
public class ScheduledReportDTO {
    private Long id;
    private String name;
    private ReportType reportType;
    private ReportFrequency frequency;
    private String recipientEmails;
    private String filterConfig;
    private ReportFormat format;
    private LocalTime scheduleTime;
    private DayOfWeek scheduleDayOfWeek;
    private Integer scheduleDay;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime nextExecutionAt;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
