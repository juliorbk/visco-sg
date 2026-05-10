package com.visco.backend.controllers;

import java.util.List;

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
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.services.ProcurementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    // POST /api/v1/procurement/orders
    @PostMapping("/orders")
    public ResponseEntity<PurchaseOrderResponse> createOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(procurementService.createPurchaseOrder(request));
    }

    // GET /api/v1/procurement/orders
    @GetMapping("/orders")
    public ResponseEntity<List<PurchaseOrderResponse>> getAllOrders() {
        return ResponseEntity.ok(procurementService.getAllOrders());
    }

    // GET /api/v1/procurement/orders/{id}
    @GetMapping("/orders/{id}")
    public ResponseEntity<PurchaseOrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.getOrderById(id));
    }

    // PATCH /api/v1/procurement/orders/{id}/deliver
    @PatchMapping("/orders/{id}/deliver")
    public ResponseEntity<PurchaseOrderResponse> markDelivered(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.markAsDelivered(id));
    }

    // PATCH /api/v1/procurement/orders/{id}/cancel
    @PatchMapping("/orders/{id}/cancel")
    public ResponseEntity<PurchaseOrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.cancelOrder(id));
    }
}
