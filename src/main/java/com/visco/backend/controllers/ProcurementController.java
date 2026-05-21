package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.services.ProcurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Procurement", description = "Purchase order management endpoints")
public class ProcurementController {

  private final ProcurementService procurementService;

  @PostMapping("/orders")
  @Operation(summary = "Create purchase order", description = "Creates a new purchase order")
  public ResponseEntity<PurchaseOrderResponse> createOrder(
    @Valid @RequestBody CreatePurchaseOrderRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      procurementService.createPurchaseOrder(request)
    );
  }

  @GetMapping("/orders")
  @Operation(summary = "List all purchase orders", description = "Returns a paginated list of all purchase orders")
  public ResponseEntity<Page<PurchaseOrderResponse>> getAllOrders(
    Pageable pageable
  ) {
    return ResponseEntity.ok(procurementService.getAllOrders(pageable));
  }

  @GetMapping("/orders/{id}")
  @Operation(summary = "Get purchase order by ID", description = "Returns a specific purchase order")
  public ResponseEntity<PurchaseOrderResponse> getOrder(@PathVariable Long id) {
    return ResponseEntity.ok(procurementService.getOrderById(id));
  }

  @PatchMapping("/orders/{id}/submit-for-approval")
  @Operation(summary = "Submit order for approval", description = "Submits a purchase order for approval")
  public ResponseEntity<PurchaseOrderResponse> submitForApproval(
    @PathVariable Long id
  ) {
    return ResponseEntity.ok(procurementService.submitOrderForApproval(id));
  }

  public record ApproveOrderRequest(String notes) {}

  @PatchMapping("/orders/{id}/approve")
  @Operation(summary = "Approve purchase order", description = "Approves a purchase order")
  public ResponseEntity<PurchaseOrderResponse> markApproved(
    @PathVariable Long id,
    @AuthenticationPrincipal UserDetails currentUser,
    @RequestBody(required = false) ApproveOrderRequest request
  ) {
    String approverEmail = currentUser.getUsername();
    String notes = request != null ? request.notes() : null;

    return ResponseEntity.ok(
      procurementService.markAsApprovedByEmail(id, approverEmail, notes)
    );
  }

  @PatchMapping("/orders/{id}/reject")
  @Operation(summary = "Reject purchase order", description = "Rejects a purchase order with a reason")
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
  @Operation(summary = "Send order to supplier", description = "Marks a purchase order as sent to supplier")
  public ResponseEntity<PurchaseOrderResponse> sendToSupplier(
    @PathVariable Long id
  ) {
    return ResponseEntity.ok(procurementService.markAsSentToSupplier(id));
  }

  @PatchMapping("/orders/{id}/cancel")
  @Operation(summary = "Cancel purchase order", description = "Cancels a purchase order")
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
