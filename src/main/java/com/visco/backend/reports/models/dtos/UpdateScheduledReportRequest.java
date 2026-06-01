package com.visco.backend.reports.models.dtos;

import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportFrequency;
import com.visco.backend.reports.models.enums.ReportType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Data;

@Data
public class UpdateScheduledReportRequest {
    private String name;
    private ReportType reportType;
    private ReportFrequency frequency;
    private String recipientEmails;
    private String filterConfig;
    private ReportFormat format;
    private LocalTime scheduleTime;
    private DayOfWeek scheduleDayOfWeek;
    private Integer scheduleDay;
    private Boolean enabled;
}
