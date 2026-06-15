package com.visco.backend.models.dtos;

import java.math.BigDecimal;

// Response DTO for a single line item within a requisition.
public record RequisitionItemResponse(
    Long productId,
    String productName,
    String productSku,
    String productInternalCode,
    String productSapCode,
    String uom,
    BigDecimal quantity,
    String notes
) {}
