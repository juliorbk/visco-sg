package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InvoiceItemRequest(
    @NotNull(message = "Product ID is required") Long productId,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @Positive BigDecimal unitPrice,
    String notes
) {}
