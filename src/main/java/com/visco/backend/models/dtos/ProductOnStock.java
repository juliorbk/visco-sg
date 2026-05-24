package com.visco.backend.models.dtos;

public record ProductOnStock(
  Long id,
  String internalCode,
  String sku,
  String name,
  String sapCode,
  String uom
) {}
