package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request payload for resetting a password using a reset token.
public record ResetPasswordRequest(
  @NotBlank String token,
  @NotBlank @Size(min = 8, max = 100) String newPassword
) {}
