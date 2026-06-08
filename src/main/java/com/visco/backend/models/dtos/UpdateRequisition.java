package com.visco.backend.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

// Request payload for updating an existing requisition.
public record UpdateRequisition(
  @NotBlank String description,
  @NotNull Long costCenterId,
  @NotNull UUID requestedById,
  @Valid @NotEmpty List<RequisitionItemRequest> items
) {}
