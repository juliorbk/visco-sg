package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.RequisitionStatus;
import java.time.LocalDateTime;
import java.util.List;

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
