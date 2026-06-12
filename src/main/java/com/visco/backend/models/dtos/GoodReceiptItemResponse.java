package com.visco.backend.models.dtos;

import java.math.BigDecimal;

// Item dentro de la respuesta de una recepción
// Muestra lo esperado vs lo recibido y la diferencia
public record GoodReceiptItemResponse(
    Long productId,
    String productName,
    String productSku,
    String uom,
    BigDecimal expectedQuantity, // Cantidad que se ordenó
    BigDecimal receivedQuantity, // Cantidad que llegó
    BigDecimal difference,       // received - expected (negativo si falta mercancía)
    Long locationId,
    String locationCode) {
}
