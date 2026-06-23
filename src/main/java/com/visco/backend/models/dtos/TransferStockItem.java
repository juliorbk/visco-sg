package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

// Single line in a batch transfer request.
public record TransferStockItem(
    @NotNull(message = "El producto es obligatorio") Long productId,

    @NotNull @Positive(message = "La cantidad debe ser positiva") BigDecimal quantity
) {}
