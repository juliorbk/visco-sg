package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReceiveGoodsRequest(

    @NotEmpty(message = "Al menos un item es requerido") @Valid List<ReceiveItem> items,

    String notes,

    @NotNull(message = "El ID del almacén destino es obligatorio") Long destinationWarehouseId,

    UUID receivedById

) {
  public record ReceiveItem(
      @NotNull(message = "El ID del producto es obligatorio") Long productId,

      @NotNull(message = "La cantidad recibida es obligatoria") BigDecimal receivedQuantity) {
  }
}