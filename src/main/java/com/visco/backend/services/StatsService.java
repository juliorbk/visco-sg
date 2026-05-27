package com.visco.backend.services;

import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.models.dtos.MonthlySpendingDTO;
import com.visco.backend.models.dtos.RecentOrderDTO;
import com.visco.backend.models.dtos.SpendingStatsDTO;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatsService {

  private final ProductRepository productRepository;
  private final PurchaseOrderRepository orderRepository;

  private static final DateTimeFormatter MONTH_FMT =
    DateTimeFormatter.ofPattern("yyyy-MM");
  private static final BigDecimal PROJECTION_FACTOR = new BigDecimal("1.10");

  // ── KPIs ──────────────────────────────────────────────────────────────────

  @Cacheable(value = "dashboard", key = "'kpis'")
  @Transactional(readOnly = true)
  public KpiStatsDTO getKpis() {
    long totalOrders = orderRepository.count();
    long delivered = orderRepository.countDeliveredOrders(); // Order Status: DELIVERED
    double fulfillmentRate =
      totalOrders == 0
        ? 0.0
        : BigDecimal.valueOf((delivered * 100.0) / totalOrders) // Success Rate
            .setScale(1, RoundingMode.HALF_UP)
            .doubleValue();

    BigDecimal totalUnits = productRepository.getTotalInventoryUnits(); // Total stock in the whole system

    LocalDateTime firstOfMonth = LocalDateTime.now()
      .withDayOfMonth(1)
      .withHour(0)
      .withMinute(0)
      .withSecond(0);
    BigDecimal monthlySpend = orderRepository.getTotalSpendingSince(
      firstOfMonth
    );

    return KpiStatsDTO.builder()
      .totalOrders(totalOrders)
      .totalInventoryUnits(totalUnits)
      .monthlySpend(monthlySpend)
      .fulfillmentRate(fulfillmentRate)
      .build();
  }

  // ── Pedidos recientes ─────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<RecentOrderDTO> getRecentOrders(int limit) {
    return orderRepository
      .findRecentOrders(PageRequest.of(0, limit))
      .stream()
      .map(o ->
        RecentOrderDTO.builder()
          .id(o.getId())
          .orderNumber(o.getOrderNumber())
          .createdAt(o.getCreatedAt())
          .supplierName(
            o.getSupplier() != null ? o.getSupplier().getName() : "—"
          )
          .status(o.getStatus())
          .build()
      )
      .toList();
  }

  // ── Gastos ────────────────────────────────────────────────────────────────

  @Cacheable(value = "dashboard", key = "'spending'")
  @Transactional(readOnly = true)
  public SpendingStatsDTO getSpendingStats() {
    LocalDateTime sixMonthsAgo = LocalDateTime.now()
      .minusMonths(6)
      .withDayOfMonth(1)
      .withHour(0)
      .withMinute(0)
      .withSecond(0);

    // Breakdown mensual
    List<MonthlySpendingDTO> monthly = orderRepository
      .getMonthlySpending(sixMonthsAgo)
      .stream()
      .map(p -> {
        BigDecimal actual =
          p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO;
        return MonthlySpendingDTO.builder()
          .month(p.getMonth().toString().substring(0, 7)) // "2025-07"
          .actual(actual)
          .projected(
            actual.multiply(PROJECTION_FACTOR).setScale(2, RoundingMode.HALF_UP)
          )
          .build();
      })
      .toList();

    // Total del mes actual
    BigDecimal totalMonthly = monthly.isEmpty()
      ? BigDecimal.ZERO
      : monthly.get(monthly.size() - 1).getActual();

    // Por categoría
    Map<String, BigDecimal> byCategory = orderRepository
      .getSpendingByCategory(sixMonthsAgo)
      .stream()
      .collect(
        Collectors.toMap(
          p ->
            p.getCategoryName() != null ? p.getCategoryName() : "Sin categoría",
          p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO
        )
      );

    BigDecimal grandTotal = orderRepository.getTotalSpendingSince(
      sixMonthsAgo
    );

    Map<String, Double> byCategoryPercent = byCategory
      .entrySet()
      .stream()
      .collect(
        Collectors.toMap(Map.Entry::getKey, e ->
          grandTotal.compareTo(BigDecimal.ZERO) == 0
            ? 0.0
            : e
                .getValue()
                .multiply(BigDecimal.valueOf(100))
                .divide(grandTotal, 1, RoundingMode.HALF_UP)
                .doubleValue()
        )
      );

    return SpendingStatsDTO.builder()
      .totalMonthly(totalMonthly)
      .monthlyBreakdown(monthly)
      .byCategory(byCategory)
      .byCategoryPercent(byCategoryPercent)
      .build();
  }

  // ── Inventario crítico ────────────────────────────────────────────────────

  @Cacheable(value = "dashboard", key = "'critical'")
  @Transactional(readOnly = true)
  public List<CriticalInventoryItemDTO> getCriticalInventory() {
    return productRepository
      .findCriticalInventory()
      .stream()
      .map(p -> {
        String severity =
          p.getCurrentStock().compareTo(BigDecimal.ZERO) == 0
            ? "CRITICAL"
            : "WARNING";
        return CriticalInventoryItemDTO.builder()
          .productId(p.getProductId())
          .productName(p.getProductName())
          .sku(p.getSku())
          .currentStock(p.getCurrentStock())
          .reorderPoint(p.getReorderPoint())
          .severity(severity)
          .build();
      })
      .toList();
  }
}
