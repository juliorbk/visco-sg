package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

// Request payload for transferring stock between warehouses.
public record TransferStockRequest(
    @NotNull(message = "El producto es obligatorio") Long productId,

    @NotNull(message = "El almacén origen es obligatorio") Long fromWarehouseId,

    @NotNull(message = "El almacén destino es obligatorio") Long toWarehouseId,

    @NotNull @Positive(message = "La cantidad debe ser positiva") BigDecimal quantity,

    String reason,

    @NotNull(message = "El usuario que realiza la transferencia es obligatorio") UUID createdById,

    BigDecimal unitCost
) {}
