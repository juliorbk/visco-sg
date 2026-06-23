package com.visco.backend.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

// Request payload for setting the stock of multiple products in a
// single warehouse to an absolute value, in one transaction.
public record AdjustStockBatchRequest(
    @NotNull(message = "El almacén es obligatorio") Long warehouseId,

    @NotEmpty(message = "Agrega al menos un producto a ajustar")
    @Valid
    List<AdjustStockItem> items,

    String reason,

    @NotNull(message = "El usuario que realiza el ajuste es obligatorio") UUID createdById
) {}
