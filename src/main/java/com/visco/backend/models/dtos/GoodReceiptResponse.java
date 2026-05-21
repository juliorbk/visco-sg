package com.visco.backend.models.dtos;

import java.time.LocalDateTime;
import java.util.List;

import com.visco.backend.models.entities.PurchaseOrderStatus;

// Respuesta después de recibir mercancía
// Incluye los datos de la nota de recepción más el status actualizado de la orden
public record GoodReceiptResponse(
    Long id,
    String receiptNumber,
    Long purchaseOrderId,
    String orderNumber,
    PurchaseOrderStatus updatedStatus,
    LocalDateTime receivedAt,
    String notes,
    String receivedBy,
    List<GoodReceiptItemResponse> items) {
}
