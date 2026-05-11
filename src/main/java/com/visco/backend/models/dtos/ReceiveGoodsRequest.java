package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// Request para recibir mercancía contra una orden de compra
// Ej: POST /api/procurement/orders/1/receive
// Body: { "items": [{ "productId": 5, "receivedQuantity": 100 }], "notes": "Todo en orden" }
public record ReceiveGoodsRequest(
    @NotEmpty(message = "At least one item is required") @Valid List<ReceiveItem> items,

    String notes) {

  // Producto recibido y cantidad que realmente llegó
  public record ReceiveItem(
      @NotNull(message = "Product ID is required") Long productId,

      @NotNull(message = "Received quantity is required") BigDecimal receivedQuantity) {
  }
}
