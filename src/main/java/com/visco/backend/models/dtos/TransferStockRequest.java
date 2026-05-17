package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferStockRequest(
    @NotNull(message = "El producto es obligatorio") Long productId,

    @NotNull(message = "La ubicación origen es obligatoria") Long fromLocationId,

    @NotNull(message = "La ubicación destino es obligatoria") Long toLocationId,

    @NotNull @Positive(message = "La cantidad debe ser positiva") BigDecimal quantity,

    String reason,

    @NotNull(message = "El usuario que realiza la transferencia es obligatorio") UUID createdById
) {}
