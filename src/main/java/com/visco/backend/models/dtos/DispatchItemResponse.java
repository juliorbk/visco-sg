package com.visco.backend.models.dtos;

import java.math.BigDecimal;

// Response DTO for a single item within a dispatch note.
public record DispatchItemResponse(
  Long productId,
  String productName,
  String productSku,
  String productInternalCode,
  String productSapCode,
  String uom,
  BigDecimal quantity
) {}
