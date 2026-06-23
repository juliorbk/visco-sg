package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

// Single line in a batch stock-adjustment request.
public record AdjustStockItem(
    @NotNull(message = "El producto es obligatorio") Long productId,

    @NotNull @PositiveOrZero(message = "El stock no puede ser negativo") BigDecimal newStock,

    BigDecimal unitCost
) {}
