package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Lightweight summary of a purchase order from the perspective of a single
// product. Returned by the "orders containing this product" endpoint.
public record ProductPurchaseOrderSummary(
    Long orderId,
    String orderNumber,
    String supplierName,
    LocalDateTime createdAt,
    BigDecimal quantityOrdered,
    BigDecimal unitPrice
) {}
