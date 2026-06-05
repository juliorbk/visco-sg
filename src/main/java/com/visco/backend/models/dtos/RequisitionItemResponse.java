package com.visco.backend.models.dtos;

import java.math.BigDecimal;

public record RequisitionItemResponse(
    Long productId,
    String productName,
    String productSku,
    BigDecimal quantity,
    String notes
) {}
