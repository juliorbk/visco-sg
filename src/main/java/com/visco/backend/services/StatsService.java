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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for dashboard statistics and KPIs.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StatsService {

  private final ProductRepository productRepository;
  private final PurchaseOrderRepository orderRepository;

  @Value("${app.reports.max-records-per-export:2000}")
  private int maxRecords;

  private static final DateTimeFormatter MONTH_FMT =
    DateTimeFormatter.ofPattern("yyyy-MM");
  private static final BigDecimal PROJECTION_FACTOR = new BigDecimal("1.10");

  // ── KPIs ──────────────────────────────────────────────────────────────────

  /**
   * Retrieves key performance indicators for the dashboard.
   *
   * @return KPI statistics DTO
   */
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

  /**
   * Retrieves the most recent purchase orders.
   *
   * @param limit maximum number of orders to return
   * @return list of recent order DTOs
   */
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
          .amount(o.getTotalAmount())
          .build()
      )
      .toList();
  }

  // ── Gastos ────────────────────────────────────────────────────────────────

  /**
   * Retrieves spending statistics over the last six months.
   *
   * @return spending statistics DTO
   */
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

    BigDecimal grandTotal = orderRepository.getTotalSpendingSince(sixMonthsAgo);

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

  /**
   * Retrieves products with critically low stock (cached default limit).
   *
   * @return list of critical inventory item DTOs
   */
  @Cacheable(value = "dashboard", key = "'critical'")
  @Transactional(readOnly = true)
  public List<CriticalInventoryItemDTO> getCriticalInventory() {
    return getCriticalInventory(maxRecords);
  }

  /**
   * Retrieves products with stock below reorder point.
   *
   * @param limit maximum number of items to return
   * @return list of critical inventory item DTOs
   */
  @Transactional(readOnly = true)
  public List<CriticalInventoryItemDTO> getCriticalInventory(int limit) {
    return productRepository
      .findCriticalInventory(PageRequest.of(0, limit))
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
          .maxStock(p.getMaxStock())
          .severity(severity)
          .build();
      })
      .toList();
  }

  /**
   * Retrieves products exceeding their maximum stock level (cached).
   *
   * @return list of overstock inventory item DTOs
   */
  @Cacheable(value = "dashboard", key = "'overstock'")
  @Transactional(readOnly = true)
  public List<CriticalInventoryItemDTO> getOverstockInventory() {
    return getOverstockInventory(maxRecords);
  }

  /**
   * Retrieves products exceeding their maximum stock level.
   *
   * @param limit maximum number of items to return
   * @return list of overstock inventory item DTOs
   */
  @Transactional(readOnly = true)
  public List<CriticalInventoryItemDTO> getOverstockInventory(int limit) {
    return productRepository
      .findOverstockInventory(PageRequest.of(0, limit))
      .stream()
      .map(p ->
        CriticalInventoryItemDTO.builder()
          .productId(p.getProductId())
          .productName(p.getProductName())
          .sku(p.getSku())
          .currentStock(p.getCurrentStock())
          .reorderPoint(p.getReorderPoint())
          .maxStock(p.getMaxStock())
          .severity("OVERSTOCK")
          .build()
      )
      .toList();
  }
}
