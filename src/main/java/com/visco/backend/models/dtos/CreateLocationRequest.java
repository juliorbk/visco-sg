package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLocationRequest(
  @NotBlank(message = "El código es obligatorio") String code,
  String aisle,
  String shelf,
  String bin,
  String description,
  @NotNull(message = "El almacén es obligatorio") Long warehouseId
) {}
