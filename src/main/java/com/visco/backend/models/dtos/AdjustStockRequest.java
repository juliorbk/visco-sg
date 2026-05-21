package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AdjustStockRequest(

		@NotNull(message = "El producto es obligatorio") Long productId,

		@NotNull(message = "El almacén es obligatorio") Long warehouseId,

		@NotNull @PositiveOrZero(message = "El stock no puede ser negativo") BigDecimal newStock,

		String reason,

		@NotNull(message = "El usuario que realiza el ajuste es obligatorio") UUID createdById,

		BigDecimal unitCost) {
}
