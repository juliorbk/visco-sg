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

  // Tamaño del lote: cada 1000 registros hace un INSERT masivo a la BD
  // (reducido de 5000 porque TEXTO LARGO MATERIAL puede ser extenso)
  private static final int BATCH_SIZE = 1000;

  // Tamaño máximo permitido del archivo (50 MB)
  private static final long MAX_FILE_SIZE_BYTES = 75 * 1024 * 1024L;

  public ProductMigrationService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public MigrationResult importProductsFromCsv(MultipartFile file) {
    // ── Validaciones previas ──────────────────────────────────────────────
    validateFile(file);

    String sql = """
      INSERT INTO products
      (internal_code, sap_code, name, sku, uom, description, category_id, is_active, reorder_point)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT (internal_code) DO NOTHING
      """;

    List<Object[]> batch = new ArrayList<>();
    int totalInserted = 0;
    int totalIgnored = 0;

    // Set para deduplicar por SKU en memoria antes del INSERT
    // El mismo producto aparece en múltiples filas por estar en distintos almacenes;
    // solo nos interesa insertar uno.
    java.util.Set<String> seenSkus = new java.util.HashSet<>();

    try (
      // BOMInputStream stripea el BOM antes de que el parser lea los headers,
      // evitando que la primera columna quede como "﻿NEW CODE" en vez de "NEW CODE"
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
        // 1. Extraer el código limpiando comillas residuales del Excel
        // BOM ya fue removido por BOMInputStream
        String rawCode = csvRecord.get("NEW CODE").replace("\"", "").trim();

        // Saltar filas vacías (basura al final del archivo Excel exportado)
        if (rawCode.isEmpty()) {
          continue;
        }

        // Normalizar el código: Excel elimina ceros a la izquierda al exportar CSV
        // (000001 → 1). Los reponemos dejando siempre mínimo 6 dígitos (000001, 000002…)
        // Si el código ya tiene más de 6 dígitos, se respeta tal cual.
        String internalCode = normalizeCode(rawCode);

        // 2. Extraer y limpiar el Category ID
        String categoryIdStr = csvRecord
          .get("CATEGORY ID")
          .replace("\"", "")
          .trim();

        if (categoryIdStr.isEmpty()) {
          log.warn(
            "Fila {} ignorada: CATEGORY ID vacío -> Producto: {}",
            csvParser.getCurrentLineNumber(),
            internalCode
          );
          totalIgnored++;
          continue;
        }

        // 3. Parsear el Category ID de forma segura
        //    El split("\\.") cubre casos como "1234.0" que exporta Excel
        long categoryId;
        try {
          categoryId = Long.parseLong(categoryIdStr.split("\\.")[0]);
        } catch (NumberFormatException e) {
          log.warn(
            "Fila {} ignorada: CATEGORY ID inválido '{}' -> Producto: {}",
            csvParser.getCurrentLineNumber(),
            categoryIdStr,
            internalCode
          );
          totalIgnored++;
          continue;
        }

        // 4. Extraer columnas requeridas (NOT NULL en la tabla)
        String sapCode = csvRecord.get("CODIGO OLD").replace("\"", "").trim();
        String name = csvRecord
          .get("DESCRIPCION + N° PARTE")
          .replace("\"", "")
          .trim();
        String sku = csvRecord.get("N° PARTE").replace("\"", "").trim();
        String uom = csvRecord.get("U/M").replace("\"", "").trim();

        // Validar que ninguna columna NOT NULL venga vacía desde el CSV
        if (
          sapCode.isEmpty() || name.isEmpty() || sku.isEmpty() || uom.isEmpty()
        ) {
          log.warn(
            "Fila {} ignorada: columna obligatoria vacía (sap_code='{}', name='{}', sku='{}', uom='{}') -> Producto: {}",
            csvParser.getCurrentLineNumber(),
            sapCode,
            name,
            sku,
            uom,
            internalCode
          );
          totalIgnored++;
          continue;
        }

        // Deduplicar por SKU: el mismo producto puede aparecer N veces en el CSV
        // porque está en distintos almacenes. Solo insertamos la primera ocurrencia.
        if (!seenSkus.add(sku)) {
          log.debug(
            "Fila {} ignorada: SKU '{}' duplicado (producto: {})",
            csvParser.getCurrentLineNumber(),
            sku,
            internalCode
          );
          totalIgnored++;
          continue;
        }

        // 5. Mapear columnas al orden exacto del INSERT
        //    Las columnas FAMILIA, SUBFAMILIA, BUSCADOR, Centro y ALMACEN
        //    están disponibles en el CSV pero no se persisten en esta tabla.
        Object[] values = new Object[] {
          internalCode, // internal_code
          sapCode, // sap_code
          name, // name
          sku, // sku
          uom, // uom
          csvRecord.get("TEXTO LARGO MATERIAL"), // description (nullable)
          categoryId, // category_id
          true, // is_active
          0.00, // reorder_point
        };

        batch.add(values);

        // 5. Flush del lote cada BATCH_SIZE registros
        if (batch.size() >= BATCH_SIZE) {
          jdbcTemplate.batchUpdate(sql, batch);
          totalInserted += batch.size();
          log.info("Progreso: {} registros insertados...", totalInserted);
          batch.clear();
        }
      }

      // 6. Flush del último lote (registros restantes por debajo de BATCH_SIZE)
      if (!batch.isEmpty()) {
        jdbcTemplate.batchUpdate(sql, batch);
        totalInserted += batch.size();
        batch.clear();
      }

      log.info(
        "Migración completada. Insertados: {} | Ignorados: {}",
        totalInserted,
        totalIgnored
      );
      return new MigrationResult(totalInserted, totalIgnored);
    } catch (Exception e) {
      // Incluir número de línea en el mensaje para facilitar el debug
      throw new RuntimeException(
        "Error procesando el archivo CSV: " + e.getMessage(),
        e
      );
    }
  }

  // ── Normalización del código interno ─────────────────────────────────────

  /**
   * Garantiza que el código tenga siempre el formato de 6 dígitos con ceros a la izquierda.
   * Excel elimina los ceros al exportar CSV: "000001" → "1".
   * Este método revierte ese comportamiento: "1" → "000001", "12345" → "012345".
   * Si el valor ya supera los 6 dígitos (ej: "1234567"), se deja intacto.
   * Si el valor no es numérico (ej: código alfanumérico), se devuelve tal cual.
   */
  private String normalizeCode(String raw) {
    try {
      long numeric = Long.parseLong(raw);
      return String.format("%06d", numeric);
    } catch (NumberFormatException e) {
      // Código alfanumérico: no se puede repadear, se usa tal cual
      return raw;
    }
  }

  // ── Validación del archivo entrante ──────────────────────────────────────

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
        "El archivo supera el tamaño máximo permitido de 50 MB."
      );
    }
  }

  // ── DTO de resultado ─────────────────────────────────────────────────────

  /**
   * Resumen de la migración devuelto al llamador (controller).
   */
  public record MigrationResult(int inserted, int ignored) {}
}
