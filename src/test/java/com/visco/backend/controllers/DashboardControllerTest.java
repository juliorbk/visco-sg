package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.models.dtos.RecentOrderDTO;
import com.visco.backend.models.dtos.SpendingStatsDTO;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.services.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void getKpis_Success() throws Exception {
        KpiStatsDTO kpis = KpiStatsDTO.builder()
                .totalOrders(100)
                .totalInventoryUnits(BigDecimal.valueOf(1000))
                .monthlySpend(BigDecimal.valueOf(50000))
                .fulfillmentRate(95.0)
                .build();

        when(statsService.getKpis()).thenReturn(kpis);

        mockMvc.perform(get("/api/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(100))
                .andExpect(jsonPath("$.totalInventoryUnits").value(1000))
                .andExpect(jsonPath("$.fulfillmentRate").value(95.0));

        verify(statsService).getKpis();
    }

    @Test
    void getRecentOrders_Success() throws Exception {
        RecentOrderDTO order = RecentOrderDTO.builder()
                .id(1L).orderNumber("PO-001").supplierName("Supplier")
                .status(PurchaseOrderStatus.DELIVERED)
                .createdAt(LocalDateTime.now())
                .build();

        when(statsService.getRecentOrders(anyInt())).thenReturn(List.of(order));

        mockMvc.perform(get("/api/dashboard/recent-orders").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("PO-001"));

        verify(statsService).getRecentOrders(5);
    }

    @Test
    void getRecentOrders_DefaultLimit() throws Exception {
        when(statsService.getRecentOrders(6)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/recent-orders"))
                .andExpect(status().isOk());

        verify(statsService).getRecentOrders(6);
    }

    @Test
    void getSpending_Success() throws Exception {
        SpendingStatsDTO spending = SpendingStatsDTO.builder()
                .totalMonthly(BigDecimal.valueOf(50000))
                .monthlyBreakdown(Collections.emptyList())
                .byCategory(Collections.emptyMap())
                .byCategoryPercent(Collections.emptyMap())
                .build();

        when(statsService.getSpendingStats()).thenReturn(spending);

        mockMvc.perform(get("/api/dashboard/spending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonthly").value(50000));

        verify(statsService).getSpendingStats();
    }

    @Test
    void getCriticalInventory_Success() throws Exception {
        CriticalInventoryItemDTO item = CriticalInventoryItemDTO.builder()
                .productId(1L).productName("Product").sku("SKU-001")
                .currentStock(BigDecimal.ZERO)
                .reorderPoint(BigDecimal.valueOf(10))
                .severity("CRITICAL")
                .build();

        when(statsService.getCriticalInventory()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/dashboard/critical-inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Product"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));

        verify(statsService).getCriticalInventory();
    }

    @Test
    void getCriticalInventory_EmptyList() throws Exception {
        when(statsService.getCriticalInventory()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/critical-inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
