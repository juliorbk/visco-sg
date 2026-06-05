package com.visco.backend.models.dtos;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(Long productId, String productName, String productSku,
        BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
