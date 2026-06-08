package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request payload containing the reason for rejecting a purchase order.
public record RejectRequest(
  @NotBlank(message = "El motivo de rechazo es obligatorio")
  @Size(max = 500, message = "El motivo no puede exceder los 500 caracteres")
  String reason
) {}
