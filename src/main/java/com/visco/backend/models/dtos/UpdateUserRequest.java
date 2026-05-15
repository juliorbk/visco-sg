package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.UserRole;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(

		@NotNull(message = "El rol es obligatorio") UserRole role,

		Long areaId) {
}
