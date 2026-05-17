package com.visco.backend.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class CostCenterMigrationService {

    private static final Logger log = LoggerFactory.getLogger(CostCenterMigrationService.class);

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024L;

    public CostCenterMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public MigrationResult importCostCentersFromCsv(MultipartFile file) {
        validateFile(file);

        String sql = """
            INSERT INTO cost_centers
            (internal_cc, code, full_description, division_description, management_code, management_description, general_management_code, general_management_description, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (code) DO NOTHING
            """;

        List<Object[]> batch = new ArrayList<>();
        List<String> currentBatchCodes = new ArrayList<>(); // Para rastrear los códigos del lote actual
        List<String> errorDetails = new ArrayList<>(); // Para guardar el registro de los que fallaron

        int totalInserted = 0;
        int totalIgnored = 0;

        Set<String> seenCodes = new HashSet<>();

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
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .build()
                .parse(reader)
        ) {
            for (CSVRecord csvRecord : csvParser) {
                long lineNumber = csvParser.getCurrentLineNumber();

                if (csvRecord.size() < 8) {
                    String error = String.format(
                        "Línea %d: Formato incompleto, no tiene suficientes columnas.",
                        lineNumber
                    );
                    log.warn(error);
                    errorDetails.add(error);
                    totalIgnored++;
                    continue;
                }

                String internalCc = csvRecord.get(0).replace("\"", "").trim();
                String code = csvRecord.get(1).replace("\"", "").trim();
                String fullDescription = csvRecord.get(2).replace("\"", "").trim();
                String divisionDescription = csvRecord.get(3).replace("\"", "").trim();
                String managementCode = csvRecord.get(4).replace("\"", "").trim();
                String managementDescription = csvRecord.get(5).replace("\"", "").trim();
                String generalManagementCode = csvRecord.get(6).replace("\"", "").trim();
                String generalManagementDescription = csvRecord.get(7).replace("\"", "").trim();

                if (code.isEmpty() || fullDescription.isEmpty()) {
                    String error = String.format(
                        "Línea %d: Código de Centro de Costos o Descripción están vacíos.",
                        lineNumber
                    );
                    log.warn(error);
                    errorDetails.add(error);
                    totalIgnored++;
                    continue;
                }

                if (!seenCodes.add(code)) {
                    String error = String.format(
                        "Línea %d: Código duplicado en el archivo CSV (%s).",
                        lineNumber,
                        code
                    );
                    log.warn(error);
                    errorDetails.add(error);
                    totalIgnored++;
                    continue;
                }

                batch.add(
                    new Object[] {
                        internalCc,
                        code,
                        fullDescription,
                        divisionDescription,
                        managementCode,
                        managementDescription,
                        generalManagementCode,
                        generalManagementDescription,
                        true
                    }
                );
                currentBatchCodes.add(code);

                if (batch.size() >= BATCH_SIZE) {
                    totalInserted += processBatch(batch, currentBatchCodes, errorDetails, sql);
                    totalIgnored += currentBatchCodes.size(); // Los que fallaron en BD
                    log.info(
                        "Progreso: {} centros de costo evaluados...",
                        totalInserted + totalIgnored
                    );
                }
            }

            if (!batch.isEmpty()) {
                totalInserted += processBatch(batch, currentBatchCodes, errorDetails, sql);
                totalIgnored += currentBatchCodes.size();
            }

            log.info(
                "Migración completada. Insertados: {} | Ignorados: {}",
                totalInserted,
                totalIgnored
            );
            return new MigrationResult(totalInserted, totalIgnored, errorDetails);
        } catch (Exception e) {
            throw new RuntimeException(
                "Error procesando el archivo CSV de centros de costo: " + e.getMessage(),
                e
            );
        }
    }

    private int processBatch(
        List<Object[]> batch,
        List<String> currentBatchCodes,
        List<String> errorDetails,
        String sql
    ) {
        int insertedInBatch = 0;
        int[] results = jdbcTemplate.batchUpdate(sql, batch);

        for (int i = 0; i < results.length; i++) {
            if (results[i] == 0) {
                // ON CONFLICT DO NOTHING devolvió 0 filas afectadas
                String code = currentBatchCodes.get(i);
                String error = String.format(
                    "BD: El código %s ya existía en el sistema y fue ignorado.",
                    code
                );
                log.warn(error);
                errorDetails.add(error);
            } else {
                insertedInBatch++;
            }
        }

        batch.clear();
        currentBatchCodes.clear();
        return insertedInBatch;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o no fue enviado.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Solo se aceptan archivos con extensión .csv.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "El archivo supera el tamaño máximo permitido de 50 MB."
            );
        }
    }

    public record MigrationResult(int inserted, int ignored, List<String> errorDetails) {}
}
