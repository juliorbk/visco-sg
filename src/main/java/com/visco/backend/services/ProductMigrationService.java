package com.visco.backend.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductMigrationService {

    private final JdbcTemplate jdbcTemplate;

    // Tamaño del lote: Cada 5000 registros hace un INSERT masivo a la BD
    private static final int BATCH_SIZE = 5000;

    public ProductMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void importProductsFromCsv(MultipartFile file) {
        String sql = """
                INSERT INTO products
                (internal_code, sap_code, name, sku, uom, description, category_id, is_active, reorder_point)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (internal_code) DO NOTHING
                """;

        List<Object[]> batch = new ArrayList<>();
        int totalInserted = 0;
        int totalIgnored = 0;

        try (
            // BOM-safe: leemos con UTF-8 y BufferedReader para que Apache CSV maneje bien las comillas
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
            );
            CSVParser csvParser = CSVFormat.DEFAULT
                .builder()
                .setHeader()                  // Primera fila como cabecera
                .setSkipHeaderRecord(true)    // No procesar la cabecera como dato
                .setIgnoreHeaderCase(true)    // Case-insensitive en nombres de columna
                .setTrim(true)               // Trim automático en todos los campos
                .setIgnoreSurroundingSpaces(true)
                .setAllowMissingColumnNames(true)
                .build()
                .parse(reader)
        ) {
            for (CSVRecord csvRecord : csvParser) {

                // 1. Extraer el código limpiando BOM y comillas residuales
                String internalCode = csvRecord.get(0)
                        .replace("\uFEFF", "") // BOM
                        .replace("\"", "")
                        .trim();

                // Si la fila está vacía (basura al final del Excel), la saltamos
                if (internalCode.isEmpty()) {
                    continue;
                }

                // 2. Extraer y limpiar el Category ID
                String categoryIdStr = csvRecord.get("CATEGORY ID")
                        .replace("\"", "")
                        .trim();

                if (categoryIdStr.isEmpty()) {
                    System.out.println("Fila ignorada por CATEGORY ID vacío -> Producto: " + internalCode);
                    totalIgnored++;
                    continue;
                }

                // 3. Parsear el Category ID de forma segura
                //    El split(".") cubre casos como "1234.0" que exporta Excel
                long categoryId;
                try {
                    categoryId = Long.parseLong(categoryIdStr.split("\\.")[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Fila ignorada por CATEGORY ID inválido: '"
                            + categoryIdStr + "' -> Producto: " + internalCode);
                    totalIgnored++;
                    continue;
                }

                // 4. Mapear columnas al orden exacto del SQL
                Object[] values = new Object[]{
                        internalCode,
                        csvRecord.get("CODIGO OLD"),
                        csvRecord.get("DESCRIPCION + N° PARTE"),
                        csvRecord.get("N° PARTE"),
                        csvRecord.get("U/M"),
                        csvRecord.get("TEXTO LARGO MATERIAL"),
                        categoryId,
                        true,
                        0.00
                };

                batch.add(values);

                // 5. Flush del lote cada BATCH_SIZE registros
                if (batch.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    totalInserted += batch.size();
                    System.out.println("Insertados hasta ahora: " + totalInserted);
                    batch.clear();
                }
            }

            // 6. Flush del último lote (el que queda debajo de BATCH_SIZE)
            //    Sin esto, los últimos registros nunca se insertaban
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batch);
                totalInserted += batch.size();
                batch.clear();
            }

            System.out.println("Migración completada. Insertados: " + totalInserted + " | Ignorados: " + totalIgnored);

        } catch (Exception e) {
            throw new RuntimeException("Error al procesar el archivo CSV: " + e.getMessage(), e);
        }
    }
}