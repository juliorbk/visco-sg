package com.visco.backend.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Request payload for creating a dispatch (stock exit) note.
public record DispatchRequest(
    @NotNull(message = "El almacén es obligatorio") Long warehouseId,

    @NotNull(message = "El empleado que retira es obligatorio") Long employeeId,

    @NotNull(message = "El centro de costos es obligatorio") Long costCenterId,

    String notes,

    UUID createdById,

    @NotEmpty(message = "Debe haber al menos un producto") @Valid List<DispatchItem> items
) {
    public record DispatchItem(
        @NotNull(message = "El producto es obligatorio") Long productId,
        @NotNull(message = "La cantidad es obligatoria") @Positive(
            message = "La cantidad debe ser positiva"
        ) BigDecimal quantity
    ) {}
}
