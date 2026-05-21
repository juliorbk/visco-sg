package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.services.ProcurementService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementController {

  private final ProcurementService procurementService;

  @PostMapping("/orders")
  public ResponseEntity<PurchaseOrderResponse> createOrder(
    @Valid @RequestBody CreatePurchaseOrderRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      procurementService.createPurchaseOrder(request)
    );
  }

  @GetMapping("/orders")
  public ResponseEntity<Page<PurchaseOrderResponse>> getAllOrders(
    Pageable pageable
  ) {
    return ResponseEntity.ok(procurementService.getAllOrders(pageable));
  }

  @GetMapping("/orders/{id}")
  public ResponseEntity<PurchaseOrderResponse> getOrder(@PathVariable Long id) {
    return ResponseEntity.ok(procurementService.getOrderById(id));
  }

  @PatchMapping("/orders/{id}/submit-for-approval")
  public ResponseEntity<PurchaseOrderResponse> submitForApproval(
    @PathVariable Long id
  ) {
    return ResponseEntity.ok(procurementService.submitOrderForApproval(id));
  }

  public record ApproveOrderRequest(String notes) {}

  @PatchMapping("/orders/{id}/approve")
  public ResponseEntity<PurchaseOrderResponse> markApproved(
    @PathVariable Long id,
    @AuthenticationPrincipal UserDetails currentUser,
    @RequestBody(required = false) ApproveOrderRequest request
  ) {
    // UserDetails.getUsername() returns the email (see User.getUsername()).
    // We delegate UUID resolution to the service so the controller stays thin.
    String approverEmail = currentUser.getUsername();
    String notes = request != null ? request.notes() : null;

    return ResponseEntity.ok(
      procurementService.markAsApprovedByEmail(id, approverEmail, notes)
    );
  }

  @PatchMapping("/orders/{id}/reject")
  public ResponseEntity<PurchaseOrderResponse> rejectOrder(
    @PathVariable Long id,
    @RequestBody Map<String, Object> body
  ) {
    UUID rejecterId =
      body.get("userId") != null
        ? UUID.fromString(body.get("userId").toString())
        : null;
    String reason =
      body.get("reason") != null ? body.get("reason").toString() : null;
    return ResponseEntity.ok(
      procurementService.rejectPurchaseOrder(id, rejecterId, reason)
    );
  }

  @PatchMapping("/orders/{id}/send-to-supplier")
  public ResponseEntity<PurchaseOrderResponse> sendToSupplier(
    @PathVariable Long id
  ) {
    return ResponseEntity.ok(procurementService.markAsSentToSupplier(id));
  }

  @PatchMapping("/orders/{id}/cancel")
  public ResponseEntity<PurchaseOrderResponse> cancelOrder(
    @PathVariable Long id,
    @RequestBody(required = false) Map<String, Object> body
  ) {
    String reason =
      body != null && body.get("reason") != null
        ? body.get("reason").toString()
        : null;
    return ResponseEntity.ok(procurementService.cancelOrder(id, reason));
  }
}
