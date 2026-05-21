package com.visco.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.visco.backend.services.ProductMigrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/migration/products")
@Tag(name = "Product Migration", description = "Product bulk import endpoints")
public class ProductMigrationController {

	private final ProductMigrationService migrationService;

	public ProductMigrationController(ProductMigrationService migrationService) {
		this.migrationService = migrationService;
	}

	@PostMapping("/import")
	@Operation(summary = "Import products from CSV", description = "Bulk imports products from a CSV file")
	public ResponseEntity<String> importCatalog(@RequestParam("file") MultipartFile file) {
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body("El archivo está vacío");
		}

		try {
			migrationService.importProductsFromCsv(file);
			return ResponseEntity.ok("Migración completada exitosamente.");
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Error durante la migración: " + e.getMessage());
		}
	}
}