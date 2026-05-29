package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.models.dtos.RecentOrderDTO;
import com.visco.backend.models.dtos.SpendingStatsDTO;
import com.visco.backend.services.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics")
public class DashboardController {

  private final StatsService statsService;

  @GetMapping("/kpis")
  @Operation(
    summary = "KPIs principales: pedidos, inventario, gastos, cumplimiento"
  )
  public ResponseEntity<KpiStatsDTO> getKpis() {
    return ResponseEntity.ok(statsService.getKpis());
  }

  @GetMapping("/recent-orders")
  @Operation(summary = "Últimas órdenes de compra")
  public ResponseEntity<List<RecentOrderDTO>> getRecentOrders(
    @RequestParam(defaultValue = "6") int limit
  ) {
    return ResponseEntity.ok(statsService.getRecentOrders(limit));
  }

  @GetMapping("/spending")
  @Operation(summary = "Gastos mensuales y desglose por categoría")
  public ResponseEntity<SpendingStatsDTO> getSpending() {
    return ResponseEntity.ok(statsService.getSpendingStats());
  }

  @GetMapping("/critical-inventory")
  @Operation(
    summary = "Productos bajo su punto de reorden"
  )
  public ResponseEntity<List<CriticalInventoryItemDTO>> getCriticalInventory() {
    return ResponseEntity.ok(statsService.getCriticalInventory());
  }

  @GetMapping("/overstock-inventory")
  @Operation(
    summary = "Productos en o por encima de su stock máximo"
  )
  public ResponseEntity<List<CriticalInventoryItemDTO>> getOverstockInventory() {
    return ResponseEntity.ok(statsService.getOverstockInventory());
  }
}
