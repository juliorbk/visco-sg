package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.PurchaseOrderStatus;
import java.time.LocalDateTime;
import java.util.List;

// Respuesta después de recibir mercancía
// Incluye los datos de la nota de recepción más el status actualizado de la orden
public record GoodReceiptResponse(
    Long id,
    String receiptNumber,
    Long purchaseOrderId,
    String warehousePhysicalAddress,
    String orderNumber,
    PurchaseOrderStatus updatedStatus,
    LocalDateTime receivedAt,
    String notes,
    String receivedBy,
    List<GoodReceiptItemResponse> items
) {}
