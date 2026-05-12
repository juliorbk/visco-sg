package com.visco.backend.controllers;

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
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.services.WarehouseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

	private final WarehouseService warehouseService;

	public WarehouseController(WarehouseService warehouseService) {
		this.warehouseService = warehouseService;
	}

	// Recibe mercancía contra una orden de compra
	// Crea una nota de recepción y ajusta el stock automáticamente
	// Si falta mercancía → PARTIALLY_DELIVERED, si está completo → DELIVERED
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

	@GetMapping("/receipts/{id}")
	public ResponseEntity<GoodReceiptResponse> getReceipt(@PathVariable Long id) {
		return ResponseEntity.ok(warehouseService.getReceiptById(id));
	}

}