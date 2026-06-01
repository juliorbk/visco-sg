package com.visco.backend.reports.models.dtos;

import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportStatus;
import com.visco.backend.reports.models.enums.ReportType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportDTO {
    private Long id;
    private String name;
    private String description;
    private ReportType type;
    private ReportStatus status;
    private ReportFormat format;
    private LocalDateTime generatedAt;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer recordCount;
    private Long fileSize;
    private String filePath;
    private String createdBy;
    private LocalDateTime createdAt;
}
