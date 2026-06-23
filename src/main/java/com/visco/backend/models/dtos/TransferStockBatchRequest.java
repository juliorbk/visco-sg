package com.visco.backend.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

// Request payload for transferring stock of multiple products
// between the same pair of warehouses in a single transaction.
public record TransferStockBatchRequest(
    @NotNull(message = "El almacén origen es obligatorio") Long fromWarehouseId,

    @NotNull(message = "El almacén destino es obligatorio") Long toWarehouseId,

    @NotEmpty(message = "Agrega al menos un producto a transferir")
    @Valid
    List<TransferStockItem> items,

    String reason,

    @NotNull(message = "El usuario que realiza la transferencia es obligatorio") UUID createdById
) {}
