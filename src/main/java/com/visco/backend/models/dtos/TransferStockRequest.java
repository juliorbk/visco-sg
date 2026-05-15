package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferStockRequest(

		@NotNull(message = "El producto es obligatorio") Long productId,

		@NotNull(message = "La ubicación origen es obligatoria") Long fromLocationId,

		@NotNull(message = "La ubicación destino es obligatoria") Long toLocationId,

		@NotNull @Positive(message = "La cantidad debe ser positiva") BigDecimal quantity,

		@NotNull(message = "El usuario que realiza la transferencia es obligatorio") UUID createdById) {
}
