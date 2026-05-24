package com.visco.backend.controllers;

import com.visco.backend.config.UserPrincipal; // <-- Importante: Tu clase recién creada
import com.visco.backend.models.dtos.ApprovalRequest;
import com.visco.backend.models.dtos.CreateRequisitionRequest;
import com.visco.backend.models.dtos.RejectRequest;
import com.visco.backend.models.dtos.RequisitionResponse;
import com.visco.backend.models.dtos.UpdateRequisition;
import com.visco.backend.models.entities.RequisitionStatus;
import com.visco.backend.services.RequisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requisitions")
@RequiredArgsConstructor
@Tag(
  name = "Requisitions",
  description = "Purchase requisition management endpoints"
)
public class RequisitionController {

  private final RequisitionService requisitionService;

  @PostMapping
  @Operation(
    summary = "Create requisition",
    description = "Creates a new purchase requisition"
  )
  public ResponseEntity<RequisitionResponse> createRequisition(
    @Valid @RequestBody CreateRequisitionRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      requisitionService.createRequisition(request)
    );
  }

  @GetMapping
  @Operation(
    summary = "List all requisitions",
    description = "Returns a paginated list of requisitions with optional status filter"
  )
  public ResponseEntity<Page<RequisitionResponse>> getAllRequisitions(
    @RequestParam(required = false) RequisitionStatus status,
    Pageable pageable
  ) {
    if (status != null) {
      return ResponseEntity.ok(
        requisitionService.getRequisitionsByStatus(status, pageable)
      );
    }
    return ResponseEntity.ok(requisitionService.getAllRequisitions(pageable));
  }

  @GetMapping("/{id}")
  @Operation(
    summary = "Get requisition by ID",
    description = "Returns a specific requisition"
  )
  public ResponseEntity<RequisitionResponse> getRequisition(
    @PathVariable Long id
  ) {
    return ResponseEntity.ok(requisitionService.getRequisitionById(id));
  }

  @PatchMapping("/{id}/submit")
  @Operation(
    summary = "Submit requisition for approval",
    description = "Submits a requisition for approval workflow"
  )
  public ResponseEntity<RequisitionResponse> submitForApproval(
    @PathVariable Long id
  ) {
    return ResponseEntity.ok(requisitionService.submitForApproval(id));
  }

  @PatchMapping("/{id}")
  @Operation(
    summary = "Edit requisition",
    description = "Updates a DRAFT requisition"
  )
  public ResponseEntity<RequisitionResponse> updateRequisition(
    @PathVariable Long id,
    @Valid @RequestBody UpdateRequisition request
  ) {
    return ResponseEntity.ok(
      requisitionService.updateRequisition(id, request.requestedById(), request)
    );
  }

  @PatchMapping("/{id}/approve")
  @Operation(
    summary = "Approve requisition",
    description = "Approves a requisition"
  )
  public ResponseEntity<RequisitionResponse> approveRequisition(
    @PathVariable Long id,
    @Valid @RequestBody ApprovalRequest request,
    @AuthenticationPrincipal UserPrincipal currentUser // <-- Inyectado automáticamente por Spring
  ) {
    return ResponseEntity.ok(
      requisitionService.approveRequisition(
        id,
        currentUser.getId(),
        request.notes()
      )
    );
  }

  @PatchMapping("/{id}/reject")
  @Operation(
    summary = "Reject requisition",
    description = "Rejects a requisition with a reason"
  )
  public ResponseEntity<RequisitionResponse> rejectRequisition(
    @PathVariable Long id,
    @Valid @RequestBody RejectRequest request,
    @AuthenticationPrincipal UserPrincipal currentUser // <-- Inyectado automáticamente por Spring
  ) {
    return ResponseEntity.ok(
      requisitionService.rejectRequisition(
        id,
        currentUser.getId(),
        request.reason()
      )
    );
  }

  @PatchMapping("/{id}/cancel")
  @Operation(
    summary = "Cancel requisition",
    description = "Cancels a requisition"
  )
  public ResponseEntity<RequisitionResponse> cancelRequisition(
    @PathVariable Long id
  ) {
    return ResponseEntity.ok(requisitionService.cancelRequisition(id));
  }

  @PatchMapping("/{id}/convert")
  @Operation(
    summary = "Convert requisition to purchase order",
    description = "Marks a requisition as converted to a purchase order"
  )
  public ResponseEntity<RequisitionResponse> markAsConverted(
    @PathVariable Long id
  ) {
    return ResponseEntity.ok(requisitionService.markAsConverted(id));
  }
}
