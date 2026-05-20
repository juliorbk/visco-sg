package com.visco.backend.controllers;

import com.visco.backend.services.ProductMigrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductMigrationController.class)
class ProductMigrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductMigrationService migrationService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void importCatalog_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", "content".getBytes()
        );

        doNothing().when(migrationService).importProductsFromCsv(any());

        mockMvc.perform(multipart("/api/migration/products/import").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("Migración completada exitosamente."));

        verify(migrationService).importProductsFromCsv(any());
    }

    @Test
    void importCatalog_FailsWhenFileIsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", new byte[0]
        );

        mockMvc.perform(multipart("/api/migration/products/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El archivo está vacío"));

        verify(migrationService, never()).importProductsFromCsv(any());
    }

    @Test
    void importCatalog_FailsWhenErrorOccurs() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", "content".getBytes()
        );

        doThrow(new RuntimeException("Migration error")).when(migrationService).importProductsFromCsv(any());

        mockMvc.perform(multipart("/api/migration/products/import").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error durante la migración: Migration error"));
    }
}
