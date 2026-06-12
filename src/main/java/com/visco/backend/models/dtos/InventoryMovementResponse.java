package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Response DTO for a single inventory movement transaction.
public record InventoryMovementResponse(
    Long id,
    Long productId,
    String productName,
    String productSku,
    String type,
    BigDecimal quantity,
    BigDecimal entryUnitPrice,
    String fromWarehouseName,
    String toWarehouseName,
    String reason,
    LocalDateTime createdAt,
    String createdByName,
    BigDecimal runningBalance
) {}
