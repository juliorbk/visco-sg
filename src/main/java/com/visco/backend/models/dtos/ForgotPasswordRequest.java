package com.visco.backend.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Request payload for initiating a password reset flow.
public record ForgotPasswordRequest(
  @NotBlank @Email String email
) {}
