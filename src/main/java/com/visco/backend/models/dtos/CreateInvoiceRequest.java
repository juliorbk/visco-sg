package com.visco.backend.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(
    @NotBlank(message = "Invoice number is required") String invoiceNumber,
    @NotNull(message = "Purchase order ID is required") Long purchaseOrderId,
    @NotNull(message = "Supplier ID is required") Long supplierId,
    @NotNull(message = "Invoice date is required") LocalDate invoiceDate,
    LocalDate dueDate,
    @NotNull(message = "Total amount is required") BigDecimal totalAmount,
    BigDecimal taxAmount,
    String notes,
    @NotEmpty(message = "At least one item is required") @Valid List<InvoiceItemRequest> items
) {}
