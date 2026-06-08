package com.visco.backend.models.dtos;

import java.math.BigDecimal;

// Response DTO for an invoice line item with PO matching info.
public record InvoiceItemResponse(
    Long productId,
    String productName,
    String productSku,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal,
    BigDecimal poQuantity,
    BigDecimal receivedQuantity,
    Boolean quantityMatch,
    Boolean priceMatch,
    String notes
) {}
