package com.visco.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private PurchaseOrderRepository orderRepository;

  @InjectMocks
  private StatsService statsService;

  // ── Helpers ──────────────────────────────────────────────────────

  private ProductRepository.CriticalProductProjection buildProjection(
    Long id,
    String name,
    String sku,
    BigDecimal currentStock,
    BigDecimal reorderPoint
  ) {
    return new ProductRepository.CriticalProductProjection() {
      @Override
      public Long getProductId() {
        return id;
      }

      @Override
      public String getProductName() {
        return name;
      }

      @Override
      public String getSku() {
        return sku;
      }

      @Override
      public BigDecimal getCurrentStock() {
        return currentStock;
      }

      @Override
      public BigDecimal getReorderPoint() {
        return reorderPoint;
      }
    };
  }

  // ── getKpis ─────────────────────────────────────────────────────

  @Test
  void shouldReturnKpis_whenDataExists() {
    when(orderRepository.count()).thenReturn(100L);
    when(orderRepository.countDeliveredOrders()).thenReturn(80L);
    when(productRepository.getTotalInventoryUnits()).thenReturn(
      new BigDecimal("5000")
    );
    when(
      orderRepository.getTotalSpendingSince(org.mockito.ArgumentMatchers.any())
    ).thenReturn(new BigDecimal("15000.00"));

    KpiStatsDTO kpis = statsService.getKpis();

    assertThat(kpis.getTotalOrders()).isEqualTo(100L);
    assertThat(kpis.getFulfillmentRate()).isEqualTo(80.0);
    assertThat(kpis.getTotalInventoryUnits()).isEqualByComparingTo("5000");
    assertThat(kpis.getMonthlySpend()).isEqualByComparingTo("15000.00");
  }

  @Test
  void shouldReturnKpisWithZeroValues_whenNoData() {
    when(orderRepository.count()).thenReturn(0L);
    when(orderRepository.countDeliveredOrders()).thenReturn(0L);
    when(productRepository.getTotalInventoryUnits()).thenReturn(
      BigDecimal.ZERO
    );
    when(
      orderRepository.getTotalSpendingSince(org.mockito.ArgumentMatchers.any())
    ).thenReturn(BigDecimal.ZERO);

    KpiStatsDTO kpis = statsService.getKpis();

    assertThat(kpis.getTotalOrders()).isZero();
    assertThat(kpis.getFulfillmentRate()).isEqualTo(0.0);
    assertThat(kpis.getTotalInventoryUnits()).isEqualByComparingTo("0");
    assertThat(kpis.getMonthlySpend()).isEqualByComparingTo("0");
  }

  @Test
  void shouldReturnKpisWithZeroFulfillment_whenNoOrders() {
    when(orderRepository.count()).thenReturn(0L);
    when(orderRepository.countDeliveredOrders()).thenReturn(0L);
    when(productRepository.getTotalInventoryUnits()).thenReturn(
      BigDecimal.ZERO
    );
    when(
      orderRepository.getTotalSpendingSince(org.mockito.ArgumentMatchers.any())
    ).thenReturn(BigDecimal.ZERO);

    KpiStatsDTO kpis = statsService.getKpis();

    assertThat(kpis.getFulfillmentRate()).isEqualTo(0.0);
  }

  @Test
  void shouldReturnKpisWithNullMonthlySpend_whenSpendingIsNull() {
    when(orderRepository.count()).thenReturn(5L);
    when(orderRepository.countDeliveredOrders()).thenReturn(3L);
    when(productRepository.getTotalInventoryUnits()).thenReturn(
      new BigDecimal("100")
    );
    when(
      orderRepository.getTotalSpendingSince(org.mockito.ArgumentMatchers.any())
    ).thenReturn(BigDecimal.ZERO);

    KpiStatsDTO kpis = statsService.getKpis();

    assertThat(kpis.getMonthlySpend()).isEqualByComparingTo("0");
  }

  // ── getCriticalInventory ────────────────────────────────────────

  @Test
  void shouldReturnCriticalInventoryItems_withWarningSeverity() {
    when(productRepository.findCriticalInventory()).thenReturn(
      List.of(
        buildProjection(
          1L,
          "Product A",
          "SKU-A",
          BigDecimal.valueOf(3),
          BigDecimal.valueOf(5)
        )
      )
    );

    List<CriticalInventoryItemDTO> items = statsService.getCriticalInventory();

    assertThat(items).hasSize(1);
    CriticalInventoryItemDTO item = items.get(0);
    assertThat(item.getProductId()).isEqualTo(1L);
    assertThat(item.getProductName()).isEqualTo("Product A");
    assertThat(item.getSku()).isEqualTo("SKU-A");
    assertThat(item.getCurrentStock()).isEqualByComparingTo("3");
    assertThat(item.getReorderPoint()).isEqualByComparingTo("5");
    assertThat(item.getSeverity()).isEqualTo("WARNING");
  }

  @Test
  void shouldReturnCriticalInventoryItems_withCriticalSeverity_whenStockIsZero() {
    when(productRepository.findCriticalInventory()).thenReturn(
      List.of(
        buildProjection(
          2L,
          "Product B",
          "SKU-B",
          BigDecimal.ZERO,
          BigDecimal.valueOf(10)
        )
      )
    );

    List<CriticalInventoryItemDTO> items = statsService.getCriticalInventory();

    assertThat(items).hasSize(1);
    CriticalInventoryItemDTO item = items.get(0);
    assertThat(item.getSeverity()).isEqualTo("CRITICAL");
  }

  @Test
  void shouldReturnEmptyList_whenNoCriticalInventory() {
    when(productRepository.findCriticalInventory()).thenReturn(List.of());

    List<CriticalInventoryItemDTO> items = statsService.getCriticalInventory();

    assertThat(items).isEmpty();
  }

  @Test
  void shouldReturnMultipleItemsWithMixedSeverity() {
    when(productRepository.findCriticalInventory()).thenReturn(
      List.of(
        buildProjection(
          1L,
          "Product A",
          "SKU-A",
          BigDecimal.ZERO,
          BigDecimal.valueOf(10)
        ),
        buildProjection(
          2L,
          "Product B",
          "SKU-B",
          BigDecimal.valueOf(3),
          BigDecimal.valueOf(5)
        ),
        buildProjection(
          3L,
          "Product C",
          "SKU-C",
          BigDecimal.valueOf(0),
          BigDecimal.valueOf(1)
        )
      )
    );

    List<CriticalInventoryItemDTO> items = statsService.getCriticalInventory();

    assertThat(items).hasSize(3);
    assertThat(items.get(0).getSeverity()).isEqualTo("CRITICAL");
    assertThat(items.get(1).getSeverity()).isEqualTo("WARNING");
    assertThat(items.get(2).getSeverity()).isEqualTo("CRITICAL");
  }
}
