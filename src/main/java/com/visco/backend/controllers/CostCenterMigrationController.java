package com.visco.backend.controllers;

import com.visco.backend.services.CostCenterMigrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/migration/cost-centers")
public class CostCenterMigrationController {

    private final CostCenterMigrationService migrationService;

    public CostCenterMigrationController(CostCenterMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/import")
    public ResponseEntity<String> importCostCenters(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo está vacío");
        }

        try {
            CostCenterMigrationService.MigrationResult result =
                migrationService.importCostCentersFromCsv(file);
            return ResponseEntity.ok(
                String.format(
                    "Migración completada exitosamente. Centros insertados: %d, Ignorados/Duplicados: %d",
                    result.inserted(),
                    result.ignored()
                )
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                "Error durante la migración: " + e.getMessage()
            );
        }
    }
}
