package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CreateRequisitionRequest;
import com.visco.backend.models.dtos.RequisitionResponse;
import com.visco.backend.models.entities.RequisitionStatus;
import com.visco.backend.services.RequisitionService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requisitions")
@RequiredArgsConstructor
public class RequisitionController {

    private final RequisitionService requisitionService;

    @PostMapping
    public ResponseEntity<RequisitionResponse> createRequisition(
        @Valid @RequestBody CreateRequisitionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            requisitionService.createRequisition(request)
        );
    }

    @GetMapping
    public ResponseEntity<Page<RequisitionResponse>> getAllRequisitions(
        @RequestParam(required = false) RequisitionStatus status,
        Pageable pageable
    ) {
        if (status != null) {
            return ResponseEntity.ok(requisitionService.getRequisitionsByStatus(status, pageable));
        }
        return ResponseEntity.ok(requisitionService.getAllRequisitions(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequisitionResponse> getRequisition(@PathVariable Long id) {
        return ResponseEntity.ok(requisitionService.getRequisitionById(id));
    }

    @PatchMapping("/{id}/submit")
    public ResponseEntity<RequisitionResponse> submitForApproval(@PathVariable Long id) {
        return ResponseEntity.ok(requisitionService.submitForApproval(id));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<RequisitionResponse> approveRequisition(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        UUID approverId =
            body.get("userId") != null ? UUID.fromString(body.get("userId").toString()) : null;
        String notes = body.get("notes") != null ? body.get("notes").toString() : null;
        return ResponseEntity.ok(requisitionService.approveRequisition(id, approverId, notes));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<RequisitionResponse> rejectRequisition(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        UUID rejecterId =
            body.get("userId") != null ? UUID.fromString(body.get("userId").toString()) : null;
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return ResponseEntity.ok(requisitionService.rejectRequisition(id, rejecterId, reason));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RequisitionResponse> cancelRequisition(@PathVariable Long id) {
        return ResponseEntity.ok(requisitionService.cancelRequisition(id));
    }

    @PatchMapping("/{id}/convert")
    public ResponseEntity<RequisitionResponse> markAsConverted(@PathVariable Long id) {
        return ResponseEntity.ok(requisitionService.markAsConverted(id));
    }
}
