package com.visco.backend.models.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWarehouseRequest(

		@NotBlank(message = "El nombre es obligatorio") String name,

		@NotBlank(message = "La dirección es obligatoria") String physicalAddress,

		@NotBlank(message = "La descripción es obligatoria") String description,

		@NotNull(message = "El usuario responsable es obligatorio") UUID responsibleUserId,

		String sapCenterCode) {
}
