package com.visco.backend.models.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

// Request payload for a single line item in a purchase order.
public record PurchaseOrderItemRequest(

        @NotNull(message = "Product ID is required") Long productId,

        @NotNull(message = "Quantity is required") BigDecimal quantity,

        @NotNull(message = "Unit price is required") BigDecimal unitPrice) {
}
