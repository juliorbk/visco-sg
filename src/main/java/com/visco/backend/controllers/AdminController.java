package com.visco.backend.controllers;

import com.visco.backend.services.WeeklyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Operaciones administrativas")
public class AdminController {

    private final WeeklyReportService weeklyReportService;

    // POST /api/admin/reports/send
    // Dispara el reporte semanal manualmente (útil para pruebas o envío urgente).
    // Solo accesible para ADMIN.
    @PostMapping("/reports/send")
    @Operation(
        summary = "Enviar reporte semanal ahora",
        description = "Genera y envía el reporte semanal inmediatamente sin esperar al cron"
    )
    public ResponseEntity<String> sendReportNow() {
        log.info("📤 Envío manual de reporte solicitado por admin");
        weeklyReportService.sendReportNow();
        return ResponseEntity.ok("Reporte enviado. Revisa los logs para confirmar la entrega.");
    }
}
