package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CostCenterRequestDto(
  Boolean isActive, // Puede ser null en la creación si tu BD ya tiene un default

  @NotBlank(message = "El código no puede estar vacío")
  @Size(max = 100)
  String code,

  @Size(max = 255) String divisionDescription,

  @NotBlank(message = "La descripción completa es obligatoria")
  @Size(max = 255)
  String fullDescription,

  @Size(max = 50) String internalCc,

  @NotNull(message = "El ID de la gerencia es obligatorio") Long managementId
) {}
