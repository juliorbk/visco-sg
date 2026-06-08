package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
// Request payload for user registration via invite token.
public class UserRegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @Email(message = "Formato de email inválido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El token de invitación es obligatorio")
    private String inviteToken;
}
