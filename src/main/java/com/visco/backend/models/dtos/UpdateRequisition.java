package com.visco.backend.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;

public record UpdateRequisition(
  @NotBlank String description,
  @NonNull Long costCenterId,
  @NonNull UUID requestedById,
  @Valid @NotEmpty List<RequisitionItemRequest> items
) {}
