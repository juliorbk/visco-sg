package com.visco.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.models.dtos.*;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.services.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SuppliersController.class)
class SuppliersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SupplierService supplierService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void createSupplier_Success() throws Exception {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "Test Supplier", "Address", "supplier@test.com", List.of("123456"),
                "Description", Currency.USD, "SAP001", null
        );

        SupplierDTO response = SupplierDTO.builder()
                .id(1L).name("Test Supplier").email("supplier@test.com").active(true).build();

        when(supplierService.createSupplier(any(CreateSupplierRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Supplier"));
    }

    @Test
    void getAllSuppliers_ReturnsPage() throws Exception {
        SupplierDTO supplier = SupplierDTO.builder()
                .id(1L).name("Test Supplier").email("supplier@test.com").active(true).build();

        Page<SupplierDTO> page = new PageImpl<>(List.of(supplier));
        when(supplierService.getAllSuppliers(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/suppliers").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Supplier"));
    }

    @Test
    void getActiveSuppliers_ReturnsPage() throws Exception {
        Page<SupplierDTO> page = new PageImpl<>(Collections.emptyList());
        when(supplierService.getActiveSuppliers(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/suppliers/active").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getInactiveSuppliers_ReturnsPage() throws Exception {
        Page<SupplierDTO> page = new PageImpl<>(Collections.emptyList());
        when(supplierService.getInactiveSuppliers(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/suppliers/inactive").param("page", "0").param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getSupplierById_Success() throws Exception {
        SupplierDTO response = SupplierDTO.builder()
                .id(1L).name("Test Supplier").email("supplier@test.com").active(true).build();

        when(supplierService.getSupplierById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Supplier"));
    }

    @Test
    void updateSupplier_Success() throws Exception {
        UpdateSupplierRequest request = new UpdateSupplierRequest(
                "Updated Supplier", "updated@test.com", List.of("654321"),
                "Updated Description", "Updated Address", Currency.EUR, "SAP002", null
        );

        SupplierDTO response = SupplierDTO.builder()
                .id(1L).name("Updated Supplier").email("updated@test.com").active(true).build();

        when(supplierService.updateSupplier(eq(1L), any(UpdateSupplierRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Supplier"));
    }

    @Test
    void deactivateSupplier_Success() throws Exception {
        doNothing().when(supplierService).deactivateSupplier(1L);

        mockMvc.perform(delete("/api/suppliers/1"))
                .andExpect(status().isNoContent());

        verify(supplierService).deactivateSupplier(1L);
    }

    @Test
    void activateSupplier_Success() throws Exception {
        doNothing().when(supplierService).activateSupplier(1L);

        mockMvc.perform(patch("/api/suppliers/1/activate"))
                .andExpect(status().isNoContent());

        verify(supplierService).activateSupplier(1L);
    }

    @Test
    void getPerformance_ReturnsList() throws Exception {
        when(supplierService.getSupplierPerformanceChart(6)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/suppliers/performance").param("months", "6"))
                .andExpect(status().isOk());
    }
}
