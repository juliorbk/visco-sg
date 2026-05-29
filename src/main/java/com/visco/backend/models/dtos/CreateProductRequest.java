package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Uom;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateProductRequest(
  @NotBlank(message = "El nombre es obligatorio") String name,

  @NotBlank(message = "El SKU es obligatorio") String sku,

  String description,

  @NotBlank(message = "El código SAP es obligatorio") String sapCode,

  @NotNull(message = "La unidad de medida es obligatoria") Uom uom,

  @NotNull
  @PositiveOrZero(message = "El punto de reorden no puede ser negativo")
  BigDecimal reorderPoint,

  @NotNull
  @PositiveOrZero(message = "El stock máximo no puede ser negativo")
  BigDecimal maxStock,

  Long supplierId,

  Long categoryId
) {}
