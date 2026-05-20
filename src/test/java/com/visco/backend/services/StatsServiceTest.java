package com.visco.backend.services;

import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.models.dtos.RecentOrderDTO;
import com.visco.backend.models.dtos.SpendingStatsDTO;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PurchaseOrderRepository orderRepository;

    @InjectMocks
    private StatsService statsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getKpis_ReturnsKpiStatsDTO() {
        when(orderRepository.count()).thenReturn(100L);
        when(orderRepository.countDeliveredOrders()).thenReturn(90L);
        when(productRepository.getTotalInventoryUnits()).thenReturn(BigDecimal.valueOf(1000));
        when(orderRepository.getMonthlySpending(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        KpiStatsDTO result = statsService.getKpis();

        assertThat(result).isNotNull();
        assertThat(result.getTotalOrders()).isEqualTo(100L);
        assertThat(result.getTotalInventoryUnits()).isEqualTo(BigDecimal.valueOf(1000));
    }

    @Test
    void getKpis_WithZeroOrders() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.countDeliveredOrders()).thenReturn(0L);
        when(productRepository.getTotalInventoryUnits()).thenReturn(BigDecimal.ZERO);
        when(orderRepository.getMonthlySpending(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        KpiStatsDTO result = statsService.getKpis();

        assertThat(result).isNotNull();
        assertThat(result.getFulfillmentRate()).isEqualTo(0.0);
    }

    @Test
    void getRecentOrders_ReturnsList() {
        when(orderRepository.findRecentOrders(any(PageRequest.class))).thenReturn(Collections.emptyList());

        List<RecentOrderDTO> result = statsService.getRecentOrders(5);

        assertThat(result).isNotNull();
        verify(orderRepository).findRecentOrders(any(PageRequest.class));
    }

    @Test
    void getSpendingStats_ReturnsSpendingStatsDTO() {
        when(orderRepository.getMonthlySpending(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(orderRepository.getSpendingByCategory(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        SpendingStatsDTO result = statsService.getSpendingStats();

        assertThat(result).isNotNull();
        assertThat(result.getMonthlyBreakdown()).isNotNull();
        assertThat(result.getByCategory()).isNotNull();
    }

    @Test
    void getCriticalInventory_ReturnsList() {
        when(productRepository.findCriticalInventory()).thenReturn(Collections.emptyList());

        List<CriticalInventoryItemDTO> result = statsService.getCriticalInventory();

        assertThat(result).isNotNull();
    }
}
