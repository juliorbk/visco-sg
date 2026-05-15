package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// Request para recibir mercancía físicamente en el WMS
// Ej: POST /api/procurement/orders/1/receive
// Body: { "items": [{ "productId": 5, "receivedQuantity": 100 }], "notes": "Todo en orden", "destinationLocationId": 15 }
public record ReceiveGoodsRequest(

    @NotEmpty(message = "Al menos un item es requerido") @Valid List<ReceiveItem> items,

    String notes,

    @NotNull(message = "El ID de la ubicación destino (estante/zona) es obligatorio") Long destinationLocationId

) {
  // Anidado CORRECTAMENTE dentro de las llaves del record principal.
  // Usamos 'record' en lugar de 'class' para mantenerlo conciso y generar los
  // getters automáticamente.
  public record ReceiveItem(
      @NotNull(message = "El ID del producto es obligatorio") Long productId,

      @NotNull(message = "La cantidad recibida es obligatoria") BigDecimal receivedQuantity) {
  }
}