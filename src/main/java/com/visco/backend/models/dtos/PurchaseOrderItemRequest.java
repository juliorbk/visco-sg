package com.visco.backend.models.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PurchaseOrderItemRequest(

        @NotNull(message = "Product ID is required") Long productId,

        @Min(value = 1, message = "Quantity must be at least 1") int quantity,

        @NotNull @DecimalMin(value = "0.01",
                message = "Unit price must be greater than zero") BigDecimal unitPrice) {
}
