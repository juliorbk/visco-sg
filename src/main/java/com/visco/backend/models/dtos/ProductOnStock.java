package com.visco.backend.models.dtos;

import java.math.BigDecimal;

// Response DTO for a product with current and pending stock quantities.
public record ProductOnStock(
  Long id,
  String internalCode,
  String sku,
  String name,
  String sapCode,
  String uom,
  BigDecimal currentStock,
  BigDecimal pendingStock,
  BigDecimal reorderPoint,
  BigDecimal maxStock
) {}
