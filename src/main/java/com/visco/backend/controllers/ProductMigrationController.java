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
import java.util.Set;

@RestController
@RequestMapping("/api/migration/products")
@Tag(name = "Product Migration", description = "Product bulk import endpoints")
public class ProductMigrationController {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"text/csv",
		"application/csv",
		"text/plain",
		"application/octet-stream"
	);
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv");
	private static final long MAX_BYTES = 20L * 1024 * 1024; // 20 MB

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
		if (file.getSize() > MAX_BYTES) {
			return ResponseEntity.badRequest().body("El archivo excede el tamaño máximo (20 MB)");
		}
		String original = file.getOriginalFilename();
		String extension = original != null && original.contains(".")
			? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
			: "";
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			return ResponseEntity.badRequest()
				.body("Extensión no permitida. Se esperaba un archivo .csv");
		}
		String contentType = file.getContentType();
		if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			return ResponseEntity.badRequest()
				.body("Tipo de contenido no permitido: " + contentType);
		}

		try {
			migrationService.importProductsFromCsv(file);
			return ResponseEntity.ok("Migración completada exitosamente.");
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Error durante la migración: " + e.getMessage());
		}
	}
}