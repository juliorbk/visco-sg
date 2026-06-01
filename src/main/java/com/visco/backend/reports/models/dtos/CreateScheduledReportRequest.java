package com.visco.backend.reports.models.dtos;

import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportFrequency;
import com.visco.backend.reports.models.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Data;

@Data
public class CreateScheduledReportRequest {
    @NotBlank(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "El tipo de reporte es requerido")
    private ReportType reportType;

    @NotNull(message = "La frecuencia es requerida")
    private ReportFrequency frequency;

    private String recipientEmails;

    private String filterConfig;

    @NotNull(message = "El formato es requerido")
    private ReportFormat format;

    @NotNull(message = "La hora es requerida")
    private LocalTime scheduleTime;

    private DayOfWeek scheduleDayOfWeek;

    private Integer scheduleDay;
}
