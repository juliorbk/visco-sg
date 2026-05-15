package com.visco.backend.models.dtos;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record CreateWarehouseRequest(

		@NotBlank(message = "El nombre es obligatorio") String name,

		@NotBlank(message = "La dirección es obligatoria") String physicalAddress,

		@NotBlank(message = "La descripción es obligatoria") @NotBlank(message = "El email es obligatorio") String description,

		@NotBlank UUID responsibleUserId,

		@NotBlank(message = "La descripción es obligatoria") String sapCenterCode,

		Set<Long> locationIds) {

}
