package com.visco.backend.reports.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.reports.models.dtos.CreateReportRequest;
import com.visco.backend.reports.models.dtos.CreateScheduledReportRequest;
import com.visco.backend.reports.models.dtos.ReportAnalyticsDTO;
import com.visco.backend.reports.models.dtos.ReportDTO;
import com.visco.backend.reports.models.dtos.ScheduledReportDTO;
import com.visco.backend.reports.models.dtos.UpdateScheduledReportRequest;
import com.visco.backend.reports.models.entities.Report;
import com.visco.backend.reports.models.entities.ScheduledReport;
import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportStatus;
import com.visco.backend.reports.models.enums.ReportType;
import com.visco.backend.reports.repositories.ReportRepository;
import com.visco.backend.reports.repositories.ScheduledReportRepository;
import com.visco.backend.reports.utils.DateUtils;
import com.visco.backend.reports.utils.FileUtils;
import jakarta.persistence.EntityNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Orchestrates report generation, scheduling, and retrieval.
 */
public class ReportService {

    private final ReportRepository reportRepository;
    private final ScheduledReportRepository scheduledReportRepository;
    private final ReportGeneratorService reportGeneratorService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;
    private final ObjectMapper objectMapper;

    @Value("${app.reports.storage-path:/var/app/reports}")
    private String storagePath;

    @Value("${app.reports.max-records-per-export:50000}")
    private int maxRecords;

    /**
     * Returns a paginated list of reports, optionally filtered by type and/or status.
     */
    public Page<ReportDTO> getReports(Pageable pageable, ReportType type, ReportStatus status) {
        if (type != null && status != null) {
            return reportRepository.findByTypeAndStatus(type, status, pageable).map(this::toDTO);
        }
        if (type != null) {
            return reportRepository.findByType(type, pageable).map(this::toDTO);
        }
        if (status != null) {
            return reportRepository.findByStatus(status, pageable).map(this::toDTO);
        }
        return reportRepository.findAll(pageable).map(this::toDTO);
    }

    /**
     * Returns a paginated list of all reports.
     */
    public Page<ReportDTO> getReports(Pageable pageable) {
        return reportRepository.findAll(pageable).map(this::toDTO);
    }

    /**
     * Returns a single report by its ID, or throws EntityNotFoundException.
     */
    public ReportDTO getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reporte no encontrado: " + id));
        return toDTO(report);
    }

    /**
     * Asynchronously generates a report (fetches data, exports to file, persists metadata).
     */
    @Async
    @Transactional
    public ReportDTO generateReport(CreateReportRequest request, String username) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        Report report = Report.builder()
                .name(request.getName())
                .type(request.getType())
                .status(ReportStatus.PENDING)
                .format(request.getFormat())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdBy(username)
                .active(true)
                .build();

        if (request.getAdditionalFilters() != null && !request.getAdditionalFilters().isEmpty()) {
            try {
                report.setFilters(objectMapper.writeValueAsString(request.getAdditionalFilters()));
            } catch (JsonProcessingException e) {
                log.warn("Could not serialize filters", e);
            }
        }

        report = reportRepository.save(report);

        try {
            report.setStatus(ReportStatus.PROCESSING);
            reportRepository.save(report);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("Generado por", username);
            metadata.put("Tipo", request.getType().getDisplayName());
            metadata.put("Período", DateUtils.formatDate(request.getStartDate()) + " - "
                    + DateUtils.formatDate(request.getEndDate()));

            Path reportsDir = FileUtils.ensureReportsDir(storagePath);
            Path filePath = null;

            switch (request.getType()) {
                case STOCK_INVENTORY -> {
                    var data = reportGeneratorService.generateStockReport(
                            request.getStartDate(), request.getEndDate(),
                            request.getCategoryId(), request.getWarehouseId(), request.getSearch());
                    if (data.size() > maxRecords) {
                        data = data.subList(0, maxRecords);
                    }
                    report.setRecordCount(data.size());
                    String ext = request.getFormat() == ReportFormat.PDF ? "pdf" : "xlsx";
                    filePath = reportsDir.resolve(FileUtils.generateFileName(request.getName(), ext));
                    try (var fos = new FileOutputStream(filePath.toFile())) {
                        if (request.getFormat() == ReportFormat.PDF) {
                            pdfExportService.exportStockReportToPdf(data, request.getName(), metadata, fos);
                        } else {
                            excelExportService.exportStockReportToExcel(data, request.getName(), metadata, fos);
                        }
                    }
                    report.setFilePath(filePath.toString());
                    report.setFileSize(Files.size(filePath));
                }
                case STOCK_MOVEMENTS -> {
                    var data = reportGeneratorService.generateMovementReport(
                            request.getStartDate(), request.getEndDate(),
                            null, request.getCategoryId(), request.getWarehouseId(), request.getSearch());
                    if (data.size() > maxRecords) data = data.subList(0, maxRecords);
                    report.setRecordCount(data.size());
                    String ext = request.getFormat() == ReportFormat.PDF ? "pdf" : "xlsx";
                    filePath = reportsDir.resolve(FileUtils.generateFileName(request.getName(), ext));
                    try (var fos = new FileOutputStream(filePath.toFile())) {
                        if (request.getFormat() == ReportFormat.PDF) {
                            pdfExportService.exportMovementReportToPdf(data, request.getName(), metadata, fos);
                        } else {
                            excelExportService.exportMovementReportToExcel(data, request.getName(), metadata, fos);
                        }
                    }
                    report.setFilePath(filePath.toString());
                    report.setFileSize(Files.size(filePath));
                }
                case CRITICAL_ALERTS -> {
                    var data = reportGeneratorService.generateAlertReport(null, null, request.getWarehouseId());
                    report.setRecordCount(data.size());
                    String ext = request.getFormat() == ReportFormat.PDF ? "pdf" : "xlsx";
                    filePath = reportsDir.resolve(FileUtils.generateFileName(request.getName(), ext));
                    try (var fos = new FileOutputStream(filePath.toFile())) {
                        if (request.getFormat() == ReportFormat.PDF) {
                            pdfExportService.exportAlertReportToPdf(data, request.getName(), metadata, fos);
                        } else {
                            excelExportService.exportAlertReportToExcel(data, request.getName(), metadata, fos);
                        }
                    }
                    report.setFilePath(filePath.toString());
                    report.setFileSize(Files.size(filePath));
                }
                case WAREHOUSE_ANALYSIS -> {
                    var data = reportGeneratorService.generateWarehouseAnalysis(request.getWarehouseId());
                    report.setRecordCount(data.size());
                    String ext = request.getFormat() == ReportFormat.PDF ? "pdf" : "xlsx";
                    filePath = reportsDir.resolve(FileUtils.generateFileName(request.getName(), ext));
                    try (var fos = new FileOutputStream(filePath.toFile())) {
                        if (request.getFormat() == ReportFormat.PDF) {
                            pdfExportService.exportWarehouseAnalysisToPdf(data, request.getName(), metadata, fos);
                        } else {
                            excelExportService.exportWarehouseAnalysisToExcel(data, request.getName(), metadata, fos);
                        }
                    }
                    report.setFilePath(filePath.toString());
                    report.setFileSize(Files.size(filePath));
                }
                default -> throw new IllegalArgumentException("Tipo de reporte no soportado: " + request.getType());
            }

            report.setStatus(ReportStatus.COMPLETED);
            report.setGeneratedAt(LocalDateTime.now());
            report = reportRepository.save(report);

            log.info("Report generated successfully: {} ({} bytes, {} records)",
                    filePath.getFileName(), Files.size(filePath), report.getRecordCount());

        } catch (Exception e) {
            log.error("Error generating report: {}", e.getMessage(), e);
            report.setStatus(ReportStatus.FAILED);
            reportRepository.save(report);
            throw new RuntimeException("Error generando reporte: " + e.getMessage(), e);
        }

        return toDTO(report);
    }

    /**
     * Soft-deletes a report and removes its file from disk.
     */
    @Transactional
    public void deleteReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reporte no encontrado: " + id));
        report.setActive(false);
        reportRepository.save(report);

        if (report.getFilePath() != null) {
            try {
                Files.deleteIfExists(Path.of(report.getFilePath()));
            } catch (IOException e) {
                log.warn("Could not delete report file: {}", report.getFilePath(), e);
            }
        }
    }

    /**
     * Returns the report file as a downloadable Resource.
     */
    public Resource downloadReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reporte no encontrado: " + id));

        if (report.getFilePath() == null || !Files.exists(Path.of(report.getFilePath()))) {
            throw new EntityNotFoundException("Archivo del reporte no encontrado");
        }

        return new FileSystemResource(Path.of(report.getFilePath()));
    }

    /**
     * Returns all scheduled reports ordered by creation date.
     */
    public List<ScheduledReportDTO> getScheduledReports() {
        return scheduledReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toScheduledDTO)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new scheduled report with calculated next execution date.
     */
    @Transactional
    public ScheduledReportDTO createScheduledReport(CreateScheduledReportRequest request) {
        LocalDateTime nextExecution = DateUtils.calculateNextExecution(
                request.getScheduleTime(), request.getScheduleDayOfWeek(), request.getScheduleDay());

        ScheduledReport sr = ScheduledReport.builder()
                .name(request.getName())
                .reportType(request.getReportType())
                .frequency(request.getFrequency())
                .recipientEmails(request.getRecipientEmails())
                .filterConfig(request.getFilterConfig())
                .format(request.getFormat())
                .scheduleTime(request.getScheduleTime())
                .scheduleDayOfWeek(request.getScheduleDayOfWeek())
                .scheduleDay(request.getScheduleDay())
                .nextExecutionAt(nextExecution)
                .enabled(true)
                .build();

        sr = scheduledReportRepository.save(sr);
        return toScheduledDTO(sr);
    }

    /**
     * Updates an existing scheduled report and recalculates its next execution date.
     */
    @Transactional
    public ScheduledReportDTO updateScheduledReport(Long id, UpdateScheduledReportRequest request) {
        ScheduledReport sr = scheduledReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reporte programado no encontrado: " + id));

        if (request.getName() != null) sr.setName(request.getName());
        if (request.getReportType() != null) sr.setReportType(request.getReportType());
        if (request.getFrequency() != null) sr.setFrequency(request.getFrequency());
        if (request.getRecipientEmails() != null) sr.setRecipientEmails(request.getRecipientEmails());
        if (request.getFilterConfig() != null) sr.setFilterConfig(request.getFilterConfig());
        if (request.getFormat() != null) sr.setFormat(request.getFormat());
        if (request.getScheduleTime() != null) sr.setScheduleTime(request.getScheduleTime());
        if (request.getScheduleDayOfWeek() != null) sr.setScheduleDayOfWeek(request.getScheduleDayOfWeek());
        if (request.getScheduleDay() != null) sr.setScheduleDay(request.getScheduleDay());
        if (request.getEnabled() != null) sr.setEnabled(request.getEnabled());

        LocalDateTime nextExec = DateUtils.calculateNextExecution(
                sr.getScheduleTime(), sr.getScheduleDayOfWeek(), sr.getScheduleDay());
        sr.setNextExecutionAt(nextExec);

        sr = scheduledReportRepository.save(sr);
        return toScheduledDTO(sr);
    }

    /**
     * Permanently deletes a scheduled report.
     */
    @Transactional
    public void deleteScheduledReport(Long id) {
        ScheduledReport sr = scheduledReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reporte programado no encontrado: " + id));
        scheduledReportRepository.delete(sr);
    }

    /**
     * Manually triggers a scheduled report, persisting and returning the generated report.
     */
    @Transactional
    public ReportDTO executeScheduledReport(Long id) {
        ScheduledReport sr = scheduledReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reporte programado no encontrado: " + id));

        CreateReportRequest request = new CreateReportRequest();
        request.setName(sr.getName());
        request.setType(sr.getReportType());
        request.setFormat(sr.getFormat());
        request.setStartDate(LocalDateTime.now().minusDays(30));
        request.setEndDate(LocalDateTime.now());

        var filters = sr.getFilterConfig();
        if (filters != null) {
            try {
                Map<String, Object> additional = objectMapper.readValue(filters, Map.class);
                request.setAdditionalFilters(additional);
            } catch (Exception e) {
                log.warn("Could not parse filter config", e);
            }
        }

        ReportDTO result = generateReport(request, "scheduler");

        sr.setLastExecutedAt(LocalDateTime.now());
        LocalDateTime nextExec = DateUtils.calculateNextExecution(
                sr.getScheduleTime(), sr.getScheduleDayOfWeek(), sr.getScheduleDay());
        sr.setNextExecutionAt(nextExec);
        scheduledReportRepository.save(sr);

        return result;
    }

    /**
     * Returns aggregated analytics: counts by status/type, monthly trend, and totals.
     */
    @Transactional(readOnly = true)
    public ReportAnalyticsDTO getReportAnalytics() {
        long totalReports = reportRepository.countByActiveTrue();
        long totalScheduled = scheduledReportRepository.count();
        long completedReports = reportRepository.countByStatusAndActiveTrue(ReportStatus.COMPLETED);
        long failedReports = reportRepository.countByStatusAndActiveTrue(ReportStatus.FAILED);
        long pendingReports = reportRepository.countByStatusAndActiveTrue(ReportStatus.PENDING);
        long totalRecordsExported = reportRepository.sumCompletedRecordCount();

        Map<String, Long> reportsByType = reportRepository.countActiveByType().stream()
                .collect(Collectors.toMap(
                        row -> ((ReportType) row[0]).name(),
                        row -> (Long) row[1]
                ));

        Map<String, Long> reportsByStatus = reportRepository.countActiveByStatus().stream()
                .collect(Collectors.toMap(
                        row -> ((ReportStatus) row[0]).name(),
                        row -> (Long) row[1]
                ));

        LocalDateTime sixMonthsAgo = LocalDateTime.now()
                .minusMonths(6)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        List<ReportAnalyticsDTO.MonthlyCount> monthlyTrend = reportRepository
                .countByMonthSince(sixMonthsAgo)
                .stream()
                .map(row -> {
                    java.sql.Timestamp ts = (java.sql.Timestamp) row[0];
                    long count = (Long) row[1];
                    return ReportAnalyticsDTO.MonthlyCount.builder()
                            .month(ts.toString().substring(0, 7))
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());

        return ReportAnalyticsDTO.builder()
                .totalReports(totalReports)
                .totalScheduledReports(totalScheduled)
                .completedReports(completedReports)
                .failedReports(failedReports)
                .pendingReports(pendingReports)
                .totalRecordsExported(totalRecordsExported)
                .reportsByType(reportsByType)
                .reportsByStatus(reportsByStatus)
                .monthlyTrend(monthlyTrend)
                .build();
    }

    private ReportDTO toDTO(Report report) {
        return ReportDTO.builder()
                .id(report.getId())
                .name(report.getName())
                .description(report.getDescription())
                .type(report.getType())
                .status(report.getStatus())
                .format(report.getFormat())
                .generatedAt(report.getGeneratedAt())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .recordCount(report.getRecordCount())
                .fileSize(report.getFileSize())
                .filePath(report.getFilePath())
                .createdBy(report.getCreatedBy())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private ScheduledReportDTO toScheduledDTO(ScheduledReport sr) {
        return ScheduledReportDTO.builder()
                .id(sr.getId())
                .name(sr.getName())
                .reportType(sr.getReportType())
                .frequency(sr.getFrequency())
                .recipientEmails(sr.getRecipientEmails())
                .filterConfig(sr.getFilterConfig())
                .format(sr.getFormat())
                .scheduleTime(sr.getScheduleTime())
                .scheduleDayOfWeek(sr.getScheduleDayOfWeek())
                .scheduleDay(sr.getScheduleDay())
                .lastExecutedAt(sr.getLastExecutedAt())
                .nextExecutionAt(sr.getNextExecutionAt())
                .enabled(sr.getEnabled())
                .createdAt(sr.getCreatedAt())
                .build();
    }
}
