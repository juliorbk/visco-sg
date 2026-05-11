package com.visco.backend.models.dtos;

import java.util.List;

import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.PurchaseOrderType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreatePurchaseOrderRequest(

        @NotBlank(message = "Order number is required") String orderNumber,

        @NotBlank(message = "Description is required") String description,

        @NotNull(message = "Supplier ID is required") Long supplierId,

        @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,

        @NotNull(message = "Purchase order type is required") PurchaseOrderType type,

        @NotEmpty(
                message = "At least one item is required") @Valid List<PurchaseOrderItemRequest> items) {
}
