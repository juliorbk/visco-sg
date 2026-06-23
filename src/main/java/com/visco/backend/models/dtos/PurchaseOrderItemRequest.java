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

        @NotNull @DecimalMin(value = "0.01",
                message = "Unit price must be greater than zero") BigDecimal unitPrice,

        // Optional: required when the PO is being created from a requisition,
        // identifies which RequisitionItem this line is fulfilling so the
        // backend can validate over-award and track partial conversion.
        Long requisitionItemId) {
}
