package com.visco.backend.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductMigrationService {

  private static final Logger log = LoggerFactory.getLogger(
    ProductMigrationService.class
  );

  private final JdbcTemplate jdbcTemplate;

  private static final int BATCH_SIZE = 1000;

  // LÍMITE AUMENTADO A 150 MB reales
  private static final long MAX_FILE_SIZE_BYTES = 150 * 1024 * 1024L;

  public ProductMigrationService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public MigrationResult importProductsFromCsv(MultipartFile file) {
    validateFile(file);

    String sql =
      "INSERT INTO products (internal_code, sap_code, name, sku, uom, description, category_id, is_active, reorder_point, max_stock, created_at, updated_at) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
      "ON CONFLICT (sap_code) DO UPDATE SET " +
      "reorder_point = EXCLUDED.reorder_point, " +
      "max_stock = EXCLUDED.max_stock, " +
      "sku = COALESCE(products.sku, EXCLUDED.sku), " +
      "updated_at = NOW()";

    List<Object[]> batch = new ArrayList<>();
    int totalInserted = 0;
    int totalIgnored = 0;

    java.util.Set<String> seenSkus = new java.util.HashSet<>();

    try (
      BOMInputStream bomStream = BOMInputStream.builder()
        .setInputStream(file.getInputStream())
        .setByteOrderMarks(ByteOrderMark.UTF_8)
        .setInclude(false)
        .get();
      BufferedReader reader = new BufferedReader(
        new InputStreamReader(bomStream, StandardCharsets.UTF_8)
      );
      CSVParser csvParser = CSVFormat.DEFAULT.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setIgnoreHeaderCase(true)
        .setTrim(true)
        .setIgnoreSurroundingSpaces(true)
        .setAllowMissingColumnNames(true)
        .build()
        .parse(reader)
    ) {
      for (CSVRecord csvRecord : csvParser) {
        String rawCode = csvRecord.get("NEW CODE").replace("\"", "").trim();
        if (rawCode.isEmpty()) continue;

        String internalCode = normalizeCode(rawCode);

        // --- CORRECCIÓN: Categoría tolerante a fallos (Comodín) ---
        Long categoryId = 144L; // <-- ID POR DEFECTO: "POR CLASIFICAR"

        try {
          if (
            csvRecord.isMapped("CATEGORY ID") &&
            !csvRecord.get("CATEGORY ID").trim().isEmpty()
          ) {
            String categoryIdStr = csvRecord
              .get("CATEGORY ID")
              .replace("\"", "")
              .trim();
            categoryId = Long.parseLong(categoryIdStr.split("\\.")[0]);
          }
        } catch (Exception e) {
          log.debug(
            "Fila {}: Sin categoría válida, se enviará a POR CLASIFICAR. Producto: {}",
            csvParser.getCurrentLineNumber(),
            internalCode
          );
        }
        // ----------------------------------------------------------

        String sapCode = csvRecord.get("CODIGO OLD").replace("\"", "").trim();
        String name = csvRecord
          .get("DESCRIPCION + N° PARTE")
          .replace("\"", "")
          .trim();
        String sku = csvRecord.get("N° PARTE").replace("\"", "").trim();
        String uom = csvRecord.get("U/M").replace("\"", "").trim();

        // VALIDACIÓN RELAJADA: Solo exigimos NOMBRE y UNIDAD DE MEDIDA
        if (name.isEmpty() || uom.isEmpty()) {
          log.warn(
            "Fila {} ignorada: Falta nombre o unidad de medida -> Producto: {}",
            csvParser.getCurrentLineNumber(),
            internalCode
          );
          totalIgnored++;
          continue;
        }

        // Si hay SKU, verificamos que no esté duplicado en esta misma carga
        if (!sku.isEmpty() && !seenSkus.add(sku)) {
          totalIgnored++;
          continue;
        }

        // LECTURA DE LOS NUEVOS CAMPOS (Punto de reorden y stock máximo)
        double reorderPoint = 0.0;
        double maxStock = 0.0;
        try {
          if (
            csvRecord.isMapped("REORDER_POINT") &&
            !csvRecord.get("REORDER_POINT").isEmpty()
          ) {
            reorderPoint = Double.parseDouble(
              csvRecord.get("REORDER_POINT").trim()
            );
          }
          if (
            csvRecord.isMapped("MAX_STOCK") &&
            !csvRecord.get("MAX_STOCK").isEmpty()
          ) {
            maxStock = Double.parseDouble(csvRecord.get("MAX_STOCK").trim());
          }
        } catch (Exception e) {
          log.debug(
            "No se pudieron parsear los límites de stock para {}. Se usarán 0.0",
            internalCode
          );
        }

        // ARRAY CON LOS 10 CAMPOS EXACTOS
        Object[] values = new Object[] {
          internalCode,
          sapCode.isEmpty() ? null : sapCode,
          name,
          sku.isEmpty() ? null : sku,
          uom,
          csvRecord.isMapped("TEXTO LARGO MATERIAL")
            ? csvRecord.get("TEXTO LARGO MATERIAL")
            : null,
          categoryId,
          true,
          reorderPoint,
          maxStock,
        };

        batch.add(values);

        if (batch.size() >= BATCH_SIZE) {
          jdbcTemplate.batchUpdate(sql, batch);
          totalInserted += batch.size();
          log.info("Progreso: {} registros procesados...", totalInserted);
          batch.clear();
        }
      }

      if (!batch.isEmpty()) {
        jdbcTemplate.batchUpdate(sql, batch);
        totalInserted += batch.size();
        batch.clear();
      }

      log.info(
        "Migración completada. Procesados: {} | Ignorados: {}",
        totalInserted,
        totalIgnored
      );
      return new MigrationResult(totalInserted, totalIgnored);
    } catch (Exception e) {
      throw new RuntimeException(
        "Error procesando el archivo CSV: " + e.getMessage(),
        e
      );
    }
  }

  private String normalizeCode(String raw) {
    try {
      long numeric = Long.parseLong(raw);
      return String.format("%06d", numeric);
    } catch (NumberFormatException e) {
      return raw;
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException(
        "El archivo está vacío o no fue enviado."
      );
    }
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
      throw new IllegalArgumentException(
        "Solo se aceptan archivos con extensión .csv."
      );
    }
    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new IllegalArgumentException(
        "El archivo supera el tamaño máximo permitido de 150 MB."
      );
    }
  }

  public record MigrationResult(int inserted, int ignored) {}
}
