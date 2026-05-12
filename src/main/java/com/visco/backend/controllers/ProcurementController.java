package com.visco.backend.controllers;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.services.ProcurementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    // Crea una nueva orden de compra
    @PostMapping("/orders")
    public ResponseEntity<PurchaseOrderResponse> createOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(procurementService.createPurchaseOrder(request));
    }

    // Lista todas las órdenes de compra
    @GetMapping("/orders")
    public ResponseEntity<List<PurchaseOrderResponse>> getAllOrders() {
        return ResponseEntity.ok(procurementService.getAllOrders());
    }

    // Obtiene una orden de compra por ID
    @GetMapping("/orders/{id}")
    public ResponseEntity<PurchaseOrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.getOrderById(id));
    }

    // Aprueba una orden (solo si está PENDING)
    @PatchMapping("/orders/{id}/approve")
    public ResponseEntity<PurchaseOrderResponse> markApproved(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.markAsApproved(id));
    }

    // Cancela una orden (solo si está PENDING o IN_TRANSIT)
    @PatchMapping("/orders/{id}/cancel")
    public ResponseEntity<PurchaseOrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.cancelOrder(id));
    }

}
