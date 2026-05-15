package com.visco.backend.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.models.dtos.MonthlySpendingDTO;
import com.visco.backend.models.dtos.RecentOrderDTO;
import com.visco.backend.models.dtos.SpendingStatsDTO;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.services.JwtService;
import com.visco.backend.services.StatsService;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(authorities = "MANAGER")
    void getKpis_shouldReturn200() throws Exception {
        when(statsService.getKpis()).thenReturn(KpiStatsDTO.builder()
                .totalOrders(100L)
                .totalInventoryUnits(BigDecimal.valueOf(5000))
                .monthlySpend(BigDecimal.valueOf(25000))
                .fulfillmentRate(85.0)
                .build());

        mockMvc.perform(get("/api/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(100))
                .andExpect(jsonPath("$.fulfillmentRate").value(85.0));
    }

    @Test
    @WithMockUser(authorities = "MANAGER")
    void getRecentOrders_shouldReturn200() throws Exception {
        when(statsService.getRecentOrders(6)).thenReturn(List.of(
                RecentOrderDTO.builder()
                        .id(1L)
                        .orderNumber("PO-001")
                        .createdAt(LocalDateTime.now())
                        .supplierName("Supplier A")
                        .status(PurchaseOrderStatus.PENDING)
                        .build()));

        mockMvc.perform(get("/api/dashboard/recent-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("PO-001"));
    }

    @Test
    @WithMockUser(authorities = "MANAGER")
    void getSpending_shouldReturn200() throws Exception {
        when(statsService.getSpendingStats()).thenReturn(SpendingStatsDTO.builder()
                .totalMonthly(BigDecimal.valueOf(15000))
                .monthlyBreakdown(List.of(MonthlySpendingDTO.builder()
                        .month("2026-05")
                        .actual(BigDecimal.valueOf(15000))
                        .projected(BigDecimal.valueOf(16500))
                        .build()))
                .byCategory(Map.of("Category A", BigDecimal.valueOf(15000)))
                .byCategoryPercent(Map.of("Category A", 100.0))
                .build());

        mockMvc.perform(get("/api/dashboard/spending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonthly").value(15000));
    }

    @Test
    @WithMockUser(authorities = "MANAGER")
    void getCriticalInventory_shouldReturn200() throws Exception {
        when(statsService.getCriticalInventory()).thenReturn(List.of(
                CriticalInventoryItemDTO.builder()
                        .productId(1L)
                        .productName("Product A")
                        .sku("SKU-001")
                        .currentStock(BigDecimal.valueOf(3))
                        .reorderPoint(BigDecimal.TEN)
                        .severity("WARNING")
                        .build()));

        mockMvc.perform(get("/api/dashboard/critical-inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].severity").value("WARNING"));
    }

    @Test
    void shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/dashboard/kpis"))
                .andExpect(status().isUnauthorized());
    }
}


