package com.visco.backend.reports.controllers;

import com.visco.backend.reports.models.dtos.CreateReportRequest;
import com.visco.backend.reports.models.dtos.CreateScheduledReportRequest;
import com.visco.backend.reports.models.dtos.ReportAnalyticsDTO;
import com.visco.backend.reports.models.dtos.ReportDTO;
import com.visco.backend.reports.models.dtos.ScheduledReportDTO;
import com.visco.backend.reports.models.dtos.UpdateScheduledReportRequest;
import com.visco.backend.reports.models.entities.Report;
import com.visco.backend.reports.models.enums.ReportFormat;
import com.visco.backend.reports.models.enums.ReportStatus;
import com.visco.backend.reports.models.enums.ReportType;
import com.visco.backend.reports.repositories.ReportRepository;
import com.visco.backend.reports.services.ReportService;
import com.visco.backend.reports.utils.DateUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Generación y gestión de reportes del sistema")
public class ReportController {

    private final ReportService reportService;
    private final ReportRepository reportRepository;

    @GetMapping
    @Operation(summary = "Listar reportes generados", description = "Retorna una lista paginada de reportes")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de reportes obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Page<ReportDTO>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        Page<ReportDTO> reports = reportService.getReports(pageable, type, status);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener reporte por ID", description = "Retorna los detalles de un reporte específico")
    public ResponseEntity<ReportDTO> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generar nuevo reporte",
               description = "Genera el reporte en memoria y devuelve los metadatos (no el archivo). "
                           + "Use /{id}/download para obtener el archivo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reporte generado y listo para descargar"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<ReportDTO> generateReport(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "anonymous";
        ReportDTO report = reportService.generateReport(request, username);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Descargar archivo del reporte",
               description = "Regenera el archivo PDF/Excel y lo transmite como streaming. "
                           + "No se carga el contenido completo en memoria del servidor.")
    public ResponseEntity<StreamingResponseBody> downloadReport(@PathVariable Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reporte no encontrado: " + id));

        if (report.getStatus() != ReportStatus.COMPLETED) {
            throw new IllegalStateException("El reporte aún no está listo (estado: " + report.getStatus() + ")");
        }

        String safeName = report.getName() == null
                ? "reporte-" + report.getId()
                : report.getName().replaceAll("[^a-zA-Z0-9-_]+", "_");
        String extension = report.getFormat().name().toLowerCase(Locale.ROOT);
        String filename = "%s-%s.%s".formatted(
                safeName, DateUtils.formatDate(report.getGeneratedAt()), extension);

        MediaType mediaType = switch (report.getFormat()) {
            case PDF -> MediaType.APPLICATION_PDF;
            case EXCEL -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case JSON -> MediaType.APPLICATION_JSON;
        };

        StreamingResponseBody stream = outputStream ->
            reportService.streamReportBytes(report, outputStream);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(stream);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar reporte", description = "Elimina (soft delete) el reporte")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/templates")
    @Operation(summary = "Listar tipos de reportes disponibles", description = "Retorna los tipos de reporte que se pueden generar")
    public ResponseEntity<List<Map<String, String>>> getReportTemplates() {
        List<Map<String, String>> templates = Arrays.stream(ReportType.values())
                .map(t -> Map.of("type", t.name(), "displayName", t.getDisplayName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Analíticas de reportes", description = "Retorna métricas agregadas y tendencias de reportes generados")
    public ResponseEntity<ReportAnalyticsDTO> getReportAnalytics() {
        return ResponseEntity.ok(reportService.getReportAnalytics());
    }

    @GetMapping("/scheduled")
    @Operation(summary = "Listar reportes programados", description = "Retorna todos los reportes programados")
    public ResponseEntity<List<ScheduledReportDTO>> getScheduledReports() {
        return ResponseEntity.ok(reportService.getScheduledReports());
    }

    @PostMapping("/scheduled")
    @Operation(summary = "Crear reporte programado", description = "Crea una nueva configuración de reporte programado")
    public ResponseEntity<ScheduledReportDTO> createScheduledReport(
            @Valid @RequestBody CreateScheduledReportRequest request) {
        return ResponseEntity.ok(reportService.createScheduledReport(request));
    }

    @PutMapping("/scheduled/{id}")
    @Operation(summary = "Actualizar reporte programado", description = "Actualiza la configuración de un reporte programado existente")
    public ResponseEntity<ScheduledReportDTO> updateScheduledReport(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduledReportRequest request) {
        return ResponseEntity.ok(reportService.updateScheduledReport(id, request));
    }

    @DeleteMapping("/scheduled/{id}")
    @Operation(summary = "Eliminar reporte programado", description = "Elimina un reporte programado")
    public ResponseEntity<Void> deleteScheduledReport(@PathVariable Long id) {
        reportService.deleteScheduledReport(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/scheduled/{id}/execute")
    @Operation(summary = "Ejecutar reporte programado", description = "Ejecuta inmediatamente un reporte programado")
    public ResponseEntity<ReportDTO> executeScheduledReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.executeScheduledReport(id));
    }
}
