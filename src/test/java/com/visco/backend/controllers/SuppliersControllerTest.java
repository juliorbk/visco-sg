package com.visco.backend.controllers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.visco.backend.models.dtos.CreateSupplierRequest;
import com.visco.backend.models.dtos.SupplierDTO;
import com.visco.backend.models.dtos.SupplierPerformanceMonthlyDTO;
import com.visco.backend.models.dtos.UpdateSupplierRequest;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.services.JwtService;
import com.visco.backend.services.SupplierService;

import jakarta.persistence.EntityNotFoundException;

@WebMvcTest(SuppliersController.class)
@AutoConfigureMockMvc(addFilters = false)
class SuppliersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupplierService supplierService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private SupplierDTO createDTO(Long id) {
        return SupplierDTO.builder()
                .id(id).name("Supplier " + id)
                .contactEmail("supplier" + id + "@test.com").active(true)
                .build();
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void createSupplier_shouldReturn200() throws Exception {
        when(supplierService.createSupplier(any(CreateSupplierRequest.class))).thenReturn(createDTO(1L));

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Supplier\",\"email\":\"s@t.com\",\"address\":\"addr\",\"currency\":\"USD\",\"description\":\"desc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Supplier 1"));
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void getAllSuppliers_shouldReturn200() throws Exception {
        when(supplierService.getAllSuppliers(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(createDTO(1L))));

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Supplier 1"));
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void getActiveSuppliers_shouldReturn200() throws Exception {
        when(supplierService.getActiveSuppliers(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(createDTO(1L))));
        mockMvc.perform(get("/api/suppliers/active")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void getInactiveSuppliers_shouldReturn200() throws Exception {
        SupplierDTO dto = createDTO(2L);
        dto.setActive(false);
        when(supplierService.getInactiveSuppliers(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));
        mockMvc.perform(get("/api/suppliers/inactive")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void getSupplierById_shouldReturn200() throws Exception {
        when(supplierService.getSupplierById(1L)).thenReturn(createDTO(1L));
        mockMvc.perform(get("/api/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Supplier 1"));
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void getSupplierById_shouldReturn404() throws Exception {
        when(supplierService.getSupplierById(99L)).thenThrow(new EntityNotFoundException("Not found"));
        mockMvc.perform(get("/api/suppliers/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void updateSupplier_shouldReturn200() throws Exception {
        when(supplierService.updateSupplier(eq(1L), any(UpdateSupplierRequest.class))).thenReturn(createDTO(1L));
        mockMvc.perform(put("/api/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"email\":\"u@t.com\",\"address\":\"a\",\"currency\":\"USD\",\"description\":\"d\",\"phoneNumbers\":[\"123\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void deactivateSupplier_shouldReturn204() throws Exception {
        doNothing().when(supplierService).deactivateSupplier(1L);
        mockMvc.perform(delete("/api/suppliers/1")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void activateSupplier_shouldReturn204() throws Exception {
        doNothing().when(supplierService).activateSupplier(1L);
        mockMvc.perform(patch("/api/suppliers/1/activate")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "PROCUREMENT")
    void getPerformance_shouldReturn200() throws Exception {
        when(supplierService.getSupplierPerformanceChart(6))
                .thenReturn(List.of(SupplierPerformanceMonthlyDTO.builder()
                        .month("2026-05").a(95.0).b(80.0).build()));
        mockMvc.perform(get("/api/suppliers/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2026-05"));
    }

}
