package com.visco.backend.models.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequisitionItemRequest(
    @NotNull(message = "Product ID is required") Long productId,
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
    BigDecimal quantity,
    String notes
) {}
