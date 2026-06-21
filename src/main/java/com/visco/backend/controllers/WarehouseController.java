package com.visco.backend.controllers;

import com.visco.backend.models.dtos.AdjustStockBatchRequest;
import com.visco.backend.models.dtos.AdjustStockRequest;
import com.visco.backend.models.dtos.CreateWarehouseRequest;
import com.visco.backend.models.dtos.DispatchRequest;
import com.visco.backend.models.dtos.DispatchResponse;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.InventoryMovementResponse;
import com.visco.backend.models.dtos.ProductOnStock;
import com.visco.backend.models.dtos.ProductStockBreakdown;
import com.visco.backend.models.dtos.PurchaseOrderReceiptSummary;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.TransferStockBatchRequest;
import com.visco.backend.models.dtos.TransferStockRequest;
import com.visco.backend.models.dtos.UpdateReceiptItemLocationRequest;
import com.visco.backend.models.dtos.WarehouseDTO;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    @Operation(
        summary = "List all warehouses",
        description = "Returns a paginated list of all warehouses"
    )
    public ResponseEntity<Page<WarehouseDTO>> getAllWarehouses(Pageable pageable) {
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
    @Operation(
        summary = "Get stock breakdown by product",
        description = "Returns stock breakdown across warehouses for a specific product"
    )
    public ResponseEntity<ProductStockBreakdown> getStockBreakdownByProduct(
        @PathVariable Long productId
    ) {
        return ResponseEntity.ok(warehouseService.getStockBreakdownByProduct(productId));
    }

    @GetMapping("/stock-summary")
    @Operation(
        summary = "Get global stock summary",
        description = "Returns stock summary per warehouse"
    )
    public ResponseEntity<List<WarehouseStockSummary>> getGlobalStockSummary() {
        return ResponseEntity.ok(warehouseService.getGlobalStockSummary());
    }

    @PostMapping("/stock/transfer")
    @Operation(summary = "Transfer stock", description = "Transfers stock between warehouses")
    public ResponseEntity<Void> transferStock(@Valid @RequestBody TransferStockRequest request) {
        warehouseService.transferStock(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stock/transfer-batch")
    @Operation(
        summary = "Transfer multiple products between warehouses",
        description = "Transfers stock of N products between the same pair of warehouses in a single transaction. Returns one movement per product."
    )
    public ResponseEntity<List<InventoryMovementResponse>> transferStockBatch(
        @Valid @RequestBody TransferStockBatchRequest request
    ) {
        return ResponseEntity.ok(warehouseService.transferStockBatch(request));
    }

    @PostMapping("/stock/adjust")
    @Operation(summary = "Adjust stock", description = "Adjusts stock quantity in a warehouse")
    public ResponseEntity<Void> adjustStock(@Valid @RequestBody AdjustStockRequest request) {
        warehouseService.adjustStock(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stock/adjust-batch")
    @Operation(
        summary = "Adjust stock of multiple products in a warehouse",
        description = "Sets the stock of N products in the same warehouse to an absolute value, in a single transaction. Returns one movement per product."
    )
    public ResponseEntity<List<InventoryMovementResponse>> adjustStockBatch(
        @Valid @RequestBody AdjustStockBatchRequest request
    ) {
        return ResponseEntity.ok(warehouseService.adjustStockBatch(request));
    }

    @GetMapping("/{id}/products")
    @Operation(
        summary = "Get all products in a warehouse",
        description = "Returns all products in the specified warehouse including those with zero stock, with optional field-specific search (name, sapCode, sku)"
    )
    public ResponseEntity<Page<ProductOnStock>> getAllProductsInWarehouse(
        @PathVariable Long id,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String sapCode,
        @RequestParam(required = false) String sku,
        Pageable pageable
    ) {
        if (name != null && name.trim().isEmpty()) name = null;
        if (sapCode != null && sapCode.trim().isEmpty()) sapCode = null;
        if (sku != null && sku.trim().isEmpty()) sku = null;
        return ResponseEntity.ok(
            warehouseService.getAllProductsInWarehouse(id, name, sapCode, sku, pageable)
        );
    }

    @GetMapping("/stock/on-stock")
    @Operation(
        summary = "Get products with stock in a warehouse",
        description = "Returns paginated products that have stock (currentStock > 0) in the specified warehouse, with optional field-specific search (name, sapCode, sku)"
    )
    public ResponseEntity<Page<ProductOnStock>> getProductsOnStock(
        @RequestParam Long warehouseId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String sapCode,
        @RequestParam(required = false) String sku,
        Pageable pageable
    ) {
        if (name != null && name.trim().isEmpty()) name = null;
        if (sapCode != null && sapCode.trim().isEmpty()) sapCode = null;
        if (sku != null && sku.trim().isEmpty()) sku = null;
        return ResponseEntity.ok(
            warehouseService.getProductsOnStock(warehouseId, name, sapCode, sku, pageable)
        );
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
    @Operation(
        summary = "List all receipts",
        description = "Returns a paginated list of all goods receipts with optional search"
    )
    public ResponseEntity<Page<GoodReceiptResponse>> getAllReceipts(
        @RequestParam(required = false) String search,
        Pageable pageable
    ) {
        return ResponseEntity.ok(warehouseService.getAllOrders(search, pageable));
    }

    @GetMapping("/orders/{orderId}/receipts")
    @Operation(
        summary = "Get receipts by order",
        description = "Returns all receipts for a specific purchase order"
    )
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

    @GetMapping("/orders/{orderId}/receipt-summary")
    @Operation(
        summary = "Get receipt summary for a purchase order",
        description = "Returns consolidated received vs pending quantities for all items in a PO"
    )
    public ResponseEntity<PurchaseOrderReceiptSummary> getReceiptSummary(
        @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(warehouseService.getReceiptSummaryByOrder(orderId));
    }

    @PatchMapping("/receipts/{receiptId}/items/{itemId}/location")
    @Operation(
        summary = "Update receipt item location",
        description = "Updates the storage location assigned to a single receipt item. Pass a null locationId to clear it."
    )
    public ResponseEntity<GoodReceiptResponse> updateReceiptItemLocation(
        @PathVariable Long receiptId,
        @PathVariable Long itemId,
        @Valid @RequestBody(required = false) UpdateReceiptItemLocationRequest request
    ) {
        return ResponseEntity.ok(
            warehouseService.updateReceiptItemLocation(
                receiptId,
                itemId,
                request == null ? new UpdateReceiptItemLocationRequest(null) : request
            )
        );
    }

    @PostMapping("/dispatch")
    @Operation(
        summary = "Create dispatch",
        description = "Creates a dispatch note (output) removing products from warehouse stock"
    )
    public ResponseEntity<DispatchResponse> createDispatch(
        @Valid @RequestBody DispatchRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            warehouseService.outputStock(request, authentication.getName()) // name -> email
        );
    }

    @GetMapping("/dispatches")
    @Operation(
        summary = "List all dispatches",
        description = "Returns a paginated list of all dispatch notes with optional search"
    )
    public ResponseEntity<Page<DispatchResponse>> getAllDispatches(
        @RequestParam(required = false) String search,
        Pageable pageable
    ) {
        return ResponseEntity.ok(warehouseService.getAllDispatches(search, pageable));
    }

    @GetMapping("/dispatches/{id}")
    @Operation(summary = "Get dispatch by ID", description = "Returns a specific dispatch note")
    public ResponseEntity<DispatchResponse> getDispatchById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getDispatchById(id));
    }

    @GetMapping("/movements")
    @Operation(
        summary = "List inventory movements",
        description = "Returns paginated inventory movements (kardex) with optional filters"
    )
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
