package com.visco.backend.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateRequisitionRequest(
    @NotBlank(message = "Requisition number is required") String requisitionNumber,
    @NotBlank(message = "Description is required") String description,
    @NotNull(message = "Requested by is required") UUID requestedById,
    @NotNull(message = "Area ID is required") Long areaId,
    @NotEmpty(message = "At least one item is required") @Valid List<RequisitionItemRequest> items
) {}
