package com.visco.backend.models.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

// Request payload for a single line item in a purchase order.
public record PurchaseOrderItemRequest(

        @NotNull(message = "Product ID is required") Long productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required") BigDecimal unitPrice) {
}
