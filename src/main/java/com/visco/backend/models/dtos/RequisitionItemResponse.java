package com.visco.backend.models.dtos;

public record RequisitionItemResponse(
    Long productId,
    String productName,
    String productSku,
    int quantity,
    String notes
) {}
