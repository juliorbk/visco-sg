package com.visco.backend.controllers;

import com.visco.backend.models.dtos.AdjustStockRequest;
import com.visco.backend.models.dtos.CreateWarehouseRequest;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.InventoryMovementResponse;
import com.visco.backend.models.dtos.ProductStockBreakdown;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.TransferStockRequest;
import com.visco.backend.models.dtos.WarehouseDTO;
import com.visco.backend.models.dtos.WarehouseResponse;
import com.visco.backend.models.dtos.WarehouseStockSummary;
import com.visco.backend.models.entities.MovementType;
import com.visco.backend.services.WarehouseService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

  private final WarehouseService warehouseService;

  // ─── Warehouses ─────────────────────────────────────────────────

  @GetMapping
  public ResponseEntity<List<WarehouseResponse>> getAllWarehouses() {
    return ResponseEntity.ok(warehouseService.getAllWarehouses());
  }

  @PostMapping
  public ResponseEntity<WarehouseDTO> createWarehouse(
    @Valid @RequestBody CreateWarehouseRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      warehouseService.createWarehouse(request)
    );
  }

  // ─── Stock breakdown by warehouse for a product ─────────────────

  @GetMapping("/products/{productId}/stock-breakdown")
  public ResponseEntity<ProductStockBreakdown> getStockBreakdownByProduct(
    @PathVariable Long productId
  ) {
    return ResponseEntity.ok(
      warehouseService.getStockBreakdownByProduct(productId)
    );
  }

  // ─── Global stock summary per warehouse ─────────────────────────

  @GetMapping("/stock-summary")
  public ResponseEntity<List<WarehouseStockSummary>> getGlobalStockSummary() {
    return ResponseEntity.ok(warehouseService.getGlobalStockSummary());
  }

  // ─── Inventory transfers ────────────────────────────────────────

  @PostMapping("/stock/transfer")
  public ResponseEntity<Void> transferStock(
    @Valid @RequestBody TransferStockRequest request
  ) {
    warehouseService.transferStock(request);
    return ResponseEntity.ok().build();
  }

  // ─── Stock adjustment ───────────────────────────────────────────

  @PostMapping("/stock/adjust")
  public ResponseEntity<Void> adjustStock(
    @Valid @RequestBody AdjustStockRequest request
  ) {
    warehouseService.adjustStock(request);
    return ResponseEntity.ok().build();
  }

  // ─── Goods receiving ────────────────────────────────────────────

  @PostMapping("/orders/{id}/receive")
  public ResponseEntity<GoodReceiptResponse> receiveGoods(
    @PathVariable Long id,
    @Valid @RequestBody ReceiveGoodsRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      warehouseService.receiveGoods(id, request)
    );
  }

  @GetMapping("/receipts")
  public ResponseEntity<Page<GoodReceiptResponse>> getAllReceipts(
    Pageable pageable
  ) {
    return ResponseEntity.ok(warehouseService.getAllOrders(pageable));
  }

  @GetMapping("/orders/{orderId}/receipts")
  public ResponseEntity<List<GoodReceiptResponse>> getReceiptsByOrderId(
    @PathVariable Long orderId
  ) {
    return ResponseEntity.ok(warehouseService.getReceiptsByOrderId(orderId));
  }

  @GetMapping("/receipts/{id}")
  public ResponseEntity<GoodReceiptResponse> getReceipt(@PathVariable Long id) {
    return ResponseEntity.ok(warehouseService.getReceiptById(id));
  }

  // ─── Kardex / Inventory Movements ───────────────────────────────

  @GetMapping("/movements")
  public ResponseEntity<List<InventoryMovementResponse>> getMovements(
    @RequestParam(required = false) Long productId,
    @RequestParam(required = false) Long locationId,
    @RequestParam(required = false) MovementType type,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime startDate,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime endDate
  ) {
    return ResponseEntity.ok(
      warehouseService.getMovements(
        productId,
        locationId,
        type,
        startDate,
        endDate
      )
    );
  }
}
