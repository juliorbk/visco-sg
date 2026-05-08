package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {

  @NotBlank(message = "El nombre es obligatorio")
  private String name;

  @Email(message = "Formato de email inválido")
  @NotBlank(message = "El email es obligatorio")
  private String email;

  @NotBlank(message = "La contraseña es obligatoria")
  @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
  private String password;

  @NotNull(message = "El rol es obligatorio")
  private UserRole role;

  private Long areaId;
}
