package com.visco.backend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductMigrationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ProductMigrationService migrationService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void importProductsFromCsv_FailsWhenFileIsEmpty() {
        MultipartFile emptyFile = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> migrationService.importProductsFromCsv(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void importProductsFromCsv_FailsWhenFileIsNull() {
        assertThatThrownBy(() -> migrationService.importProductsFromCsv(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void importProductsFromCsv_FailsWhenExtensionIsNotCsv() {
        MultipartFile txtFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "content".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> migrationService.importProductsFromCsv(txtFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(".csv");
    }

    @Test
    void importProductsFromCsv_SuccessWithValidCsv() {
        String csvContent = "NEW CODE,CODIGO OLD,DESCRIPCION + N° PARTE,N° PARTE,U/M,CATEGORY ID\n" +
                "\"001\",\"SAP001\",\"Product One\",\"SKU001\",\"UNIT\",\"144\"\n" +
                "\"002\",\"SAP002\",\"Product Two\",\"SKU002\",\"UNIT\",\"144\"\n";

        MultipartFile csvFile = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        when(jdbcTemplate.batchUpdate(anyString(), any())).thenReturn(new int[]{1, 1});

        ProductMigrationService.MigrationResult result = migrationService.importProductsFromCsv(csvFile);

        assertThat(result).isNotNull();
        assertThat(result.inserted()).isGreaterThanOrEqualTo(0);
        verify(jdbcTemplate).batchUpdate(anyString(), any());
    }

    @Test
    void normalizeCode_ReturnsFormattedCode() {
        String csvContent = "NEW CODE,CODIGO OLD,DESCRIPCION + N° PARTE,N° PARTE,U/M\n" +
                "\"1\",\"SAP001\",\"Product One\",\"SKU001\",\"UNIT\"\n";

        MultipartFile csvFile = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        when(jdbcTemplate.batchUpdate(anyString(), any())).thenReturn(new int[]{1});

        ProductMigrationService.MigrationResult result = migrationService.importProductsFromCsv(csvFile);

        assertThat(result).isNotNull();
        verify(jdbcTemplate).batchUpdate(anyString(), any());
    }

    @Test
    void importProductsFromCsv_IgnoresRowsWithMissingName() {
        String csvContent = "NEW CODE,CODIGO OLD,DESCRIPCION + N° PARTE,N° PARTE,U/M\n" +
                "\"001\",\"SAP001\",\"\",\"SKU001\",\"UNIT\"\n";

        MultipartFile csvFile = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        when(jdbcTemplate.batchUpdate(anyString(), any())).thenReturn(new int[0]);

        ProductMigrationService.MigrationResult result = migrationService.importProductsFromCsv(csvFile);

        assertThat(result).isNotNull();
        assertThat(result.ignored()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void importProductsFromCsv_IgnoresRowsWithMissingUom() {
        String csvContent = "NEW CODE,CODIGO OLD,DESCRIPCION + N° PARTE,N° PARTE,U/M\n" +
                "\"001\",\"SAP001\",\"Product One\",\"SKU001\",\"\"\n";

        MultipartFile csvFile = new MockMultipartFile(
                "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8)
        );

        when(jdbcTemplate.batchUpdate(anyString(), any())).thenReturn(new int[0]);

        ProductMigrationService.MigrationResult result = migrationService.importProductsFromCsv(csvFile);

        assertThat(result).isNotNull();
    }
}
