package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Request payload for creating or updating a cost center.
public record CostCenterRequestDto(
  Boolean isActive,

  @NotBlank(message = "El código no puede estar vacío")
  @Size(max = 100)
  String code,

  @Size(max = 255) String divisionDescription,

  @NotBlank(message = "La descripción completa es obligatoria")
  @Size(max = 255)
  String fullDescription,

  @NotNull(message = "El ID de la gerencia es obligatorio") Long managementId
) {}
