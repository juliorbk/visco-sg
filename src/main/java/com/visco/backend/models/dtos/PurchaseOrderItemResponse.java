package com.visco.backend.models.dtos;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(Long productId, String productName, String productSku,
        int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
