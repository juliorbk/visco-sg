package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Request payload for creating a warehouse location.
public record CreateLocationRequest(
  @NotBlank(message = "El código es obligatorio") String code,
  String zone,
  String aisle,
  String rack,
  String level,
  Integer positionX,
  Integer positionY,
  String description,
  @NotNull(message = "El almacén es obligatorio") Long warehouseId
) {}