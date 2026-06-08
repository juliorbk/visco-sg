package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.RequisitionStatus;
import java.time.LocalDateTime;
import java.util.List;

// Response DTO for a complete requisition with items and approval status.
public record RequisitionResponse(
    Long id,
    String requisitionNumber,
    String description,
    String requestedBy,
    String areaName,
    RequisitionStatus status,
    String rejectionReason,
    String approvalNotes,
    String approvedBy,
    LocalDateTime approvedAt,
    LocalDateTime createdAt,
    List<RequisitionItemResponse> items
) {}
