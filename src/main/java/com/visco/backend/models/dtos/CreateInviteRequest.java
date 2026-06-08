package com.visco.backend.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
// Request payload for creating a user invitation token.
public class CreateInviteRequest {

  @NotBlank(message = "El email es obligatorio")
  @Email(message = "Formato de email inválido")
  private String email;

  @NotNull(message = "El rol es obligatorio")
  private String role;

  private Long costCenterId;

  private LocalDateTime expiresAt;
}
