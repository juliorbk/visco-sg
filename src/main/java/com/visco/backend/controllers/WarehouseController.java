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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Warehouse", description = "Warehouse and stock management endpoints")
public class WarehouseController {

  private final WarehouseService warehouseService;

  @GetMapping
  @Operation(summary = "List all warehouses", description = "Returns a paginated list of all warehouses")
  public ResponseEntity<Page<WarehouseDTO>> getAllWarehouses(
    Pageable pageable
  ) {
    return ResponseEntity.ok(warehouseService.getAllWarehouses(pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get warehouse by ID", description = "Returns a warehouse by its ID")
  public ResponseEntity<WarehouseDTO> getWarehouse(@PathVariable Long id) {
    return ResponseEntity.ok(warehouseService.getWarehouse(id));
  }

  @PostMapping
  @Operation(summary = "Create warehouse", description = "Creates a new warehouse")
  public ResponseEntity<WarehouseDTO> createWarehouse(
    @Valid @RequestBody CreateWarehouseRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      warehouseService.createWarehouse(request)
    );
  }

  @GetMapping("/products/{productId}/stock-breakdown")
  @Operation(summary = "Get stock breakdown by product", description = "Returns stock breakdown across warehouses for a specific product")
  public ResponseEntity<ProductStockBreakdown> getStockBreakdownByProduct(
    @PathVariable Long productId
  ) {
    return ResponseEntity.ok(
      warehouseService.getStockBreakdownByProduct(productId)
    );
  }

  @GetMapping("/stock-summary")
  @Operation(summary = "Get global stock summary", description = "Returns stock summary per warehouse")
  public ResponseEntity<List<WarehouseStockSummary>> getGlobalStockSummary() {
    return ResponseEntity.ok(warehouseService.getGlobalStockSummary());
  }

  @PostMapping("/stock/transfer")
  @Operation(summary = "Transfer stock", description = "Transfers stock between warehouses")
  public ResponseEntity<Void> transferStock(
    @Valid @RequestBody TransferStockRequest request
  ) {
    warehouseService.transferStock(request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/stock/adjust")
  @Operation(summary = "Adjust stock", description = "Adjusts stock quantity in a warehouse")
  public ResponseEntity<Void> adjustStock(
    @Valid @RequestBody AdjustStockRequest request
  ) {
    warehouseService.adjustStock(request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/orders/{id}/receive")
  @Operation(summary = "Receive goods", description = "Receives goods against a purchase order")
  public ResponseEntity<GoodReceiptResponse> receiveGoods(
    @PathVariable Long id,
    @Valid @RequestBody ReceiveGoodsRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      warehouseService.receiveGoods(id, request)
    );
  }

  @GetMapping("/receipts")
  @Operation(summary = "List all receipts", description = "Returns a paginated list of all goods receipts")
  public ResponseEntity<Page<GoodReceiptResponse>> getAllReceipts(
    Pageable pageable
  ) {
    return ResponseEntity.ok(warehouseService.getAllOrders(pageable));
  }

  @GetMapping("/orders/{orderId}/receipts")
  @Operation(summary = "Get receipts by order", description = "Returns all receipts for a specific purchase order")
  public ResponseEntity<List<GoodReceiptResponse>> getReceiptsByOrderId(
    @PathVariable Long orderId
  ) {
    return ResponseEntity.ok(warehouseService.getReceiptsByOrderId(orderId));
  }

  @GetMapping("/receipts/{id}")
  @Operation(summary = "Get receipt by ID", description = "Returns a specific goods receipt")
  public ResponseEntity<GoodReceiptResponse> getReceipt(@PathVariable Long id) {
    return ResponseEntity.ok(warehouseService.getReceiptById(id));
  }

  @GetMapping("/movements")
  @Operation(summary = "List inventory movements", description = "Returns paginated inventory movements (kardex) with optional filters")
  public ResponseEntity<Page<InventoryMovementResponse>> getMovements(
    @RequestParam(required = false) Long productId,
    @RequestParam(required = false) Long warehouseId,
    @RequestParam(required = false) MovementType type,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime startDate,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime endDate,
    Pageable pageable
  ) {
    return ResponseEntity.ok(
      warehouseService.getMovements(
        productId,
        warehouseId,
        type,
        startDate,
        endDate,
        pageable
      )
    );
  }
}
