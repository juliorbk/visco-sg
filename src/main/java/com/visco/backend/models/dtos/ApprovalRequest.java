package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request payload containing approval notes for a purchase order.
public record ApprovalRequest(
  @Size(max = 500, message = "Las notas no pueden exceder los 500 caracteres")
  String notes
) {}
