package com.visco.backend.models.dtos;

import java.math.BigDecimal;

// Response DTO for a single line item within a purchase order.
public record PurchaseOrderItemResponse(Long productId, String productName, String productSku,
        BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
