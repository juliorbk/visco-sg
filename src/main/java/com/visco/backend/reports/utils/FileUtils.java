package com.visco.backend.reports.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
// Utility for report file path management, name generation, and cleanup.
public final class FileUtils {

    public static Path ensureReportsDir(String storagePath) {
        try {
            Path dir = Path.of(storagePath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                log.info("Created reports storage directory: {}", dir);
            }
            return dir;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de reportes: " + storagePath, e);
        }
    }

    public static String generateFileName(String baseName, String extension) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeName = baseName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        return safeName + "_" + dateStr + "." + extension;
    }

    public static void deleteOldFiles(Path storagePath, int daysOld) {
        try {
            LocalDate cutoff = LocalDate.now().minusDays(daysOld);
            try (var files = Files.list(storagePath)) {
                files.filter(path -> {
                    try {
                        return Files.isRegularFile(path)
                            && Files.getLastModifiedTime(path).toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate().isBefore(cutoff);
                    } catch (IOException e) {
                        return false;
                    }
                }).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        log.info("Deleted old report file: {}", path);
                    } catch (IOException e) {
                        log.warn("Could not delete old report file: {}", path, e);
                    }
                });
            }
        } catch (IOException e) {
            log.warn("Error cleaning up old report files", e);
        }
    }
}
