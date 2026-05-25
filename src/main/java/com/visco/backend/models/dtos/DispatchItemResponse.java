package com.visco.backend.models.dtos;

import java.math.BigDecimal;

public record DispatchItemResponse(
  Long productId,
  String productName,
  String productSku,
  BigDecimal quantity,
  BigDecimal exitUnitPrice
) {}
