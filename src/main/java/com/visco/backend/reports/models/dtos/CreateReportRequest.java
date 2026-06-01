package com.visco.backend.reports.models.dtos;

import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotBlank(message = "El nombre del reporte es requerido")
    private String name;

    @NotNull(message = "El tipo de reporte es requerido")
    private ReportType type;

    @NotNull(message = "El formato es requerido")
    private ReportFormat format;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDateTime startDate;

    @NotNull(message = "La fecha de fin es requerida")
    private LocalDateTime endDate;

    private Long categoryId;
    private Long warehouseId;
    private String search;
    private Map<String, Object> additionalFilters;
}
