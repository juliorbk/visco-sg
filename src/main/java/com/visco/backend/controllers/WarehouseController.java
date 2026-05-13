package com.visco.backend.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.ProductStockBreakdown;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.WarehouseResponse;
import com.visco.backend.models.dtos.WarehouseStockSummary;
import com.visco.backend.services.WarehouseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

	private final WarehouseService warehouseService;

	public WarehouseController(WarehouseService warehouseService) {
		this.warehouseService = warehouseService;
	}

	// ─── Warehouses ─────────────────────────────────────────────────

	@GetMapping
	public ResponseEntity<List<WarehouseResponse>> getAllWarehouses() {
		return ResponseEntity.ok(warehouseService.getAllWarehouses());
	}

	// ─── Stock breakdown by warehouse for a product ─────────────────

	@GetMapping("/products/{productId}/stock-breakdown")
	public ResponseEntity<ProductStockBreakdown> getStockBreakdownByProduct(
			@PathVariable Long productId) {
		return ResponseEntity.ok(warehouseService.getStockBreakdownByProduct(productId));
	}

	// ─── Global stock summary per warehouse ─────────────────────────

	@GetMapping("/stock-summary")
	public ResponseEntity<List<WarehouseStockSummary>> getGlobalStockSummary() {
		return ResponseEntity.ok(warehouseService.getGlobalStockSummary());
	}

	// ─── Goods receiving ────────────────────────────────────────────

	@PostMapping("/orders/{id}/receive")
	public ResponseEntity<GoodReceiptResponse> receiveGoods(
			@PathVariable Long id,
			@Valid @RequestBody ReceiveGoodsRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(warehouseService.receiveGoods(id, request));
	}

	@GetMapping("/receipts")
	public ResponseEntity<Page<GoodReceiptResponse>> getAllReceipts(Pageable pageable) {
		return ResponseEntity.ok(warehouseService.getAllOrders(pageable));
	}

	@GetMapping("/orders/{orderId}/receipts")
	public ResponseEntity<List<GoodReceiptResponse>> getReceiptsByOrderId(@PathVariable Long orderId) {
		return ResponseEntity.ok(warehouseService.getReceiptsByOrderId(orderId));
	}

	@GetMapping("/receipts/{id}")
	public ResponseEntity<GoodReceiptResponse> getReceipt(@PathVariable Long id) {
		return ResponseEntity.ok(warehouseService.getReceiptById(id));
	}

}
