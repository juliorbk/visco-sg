package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CostCenterDTO;
import com.visco.backend.services.CostCenterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CostCenterController.class)
class CostCenterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CostCenterService costCenterService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void getAllCostCenters_ReturnsPage() throws Exception {
        CostCenterDTO costCenter = CostCenterDTO.builder()
                .id(1L).fullDescription("Test Cost Center").build();

        Page<CostCenterDTO> page = new PageImpl<>(List.of(costCenter));
        when(costCenterService.getCostCenters(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/cost-centers").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullDescription").value("Test Cost Center"));
    }

    @Test
    void getAllUnpaged_ReturnsList() throws Exception {
        CostCenterDTO costCenter = CostCenterDTO.builder()
                .id(1L).fullDescription("Test Cost Center").build();

        when(costCenterService.getAllCostCenters()).thenReturn(List.of(costCenter));

        mockMvc.perform(get("/api/cost-centers/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullDescription").value("Test Cost Center"));
    }

    @Test
    void getById_Success() throws Exception {
        CostCenterDTO costCenter = CostCenterDTO.builder()
                .id(1L).fullDescription("Test Cost Center").build();

        when(costCenterService.getCostCenterById(1L)).thenReturn(costCenter);

        mockMvc.perform(get("/api/cost-centers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullDescription").value("Test Cost Center"));
    }
}
