package com.visco.backend.models.dtos;

import jakarta.validation.constraints.Positive;

// Request payload for updating the location assigned to a receipt item.
public record UpdateReceiptItemLocationRequest(
  @Positive(message = "El ID de la ubicación debe ser positivo") Long locationId
) {}
