package com.visco.backend.models.dtos;

import java.time.LocalDateTime;
import java.util.List;

import com.visco.backend.models.entities.PurchaseOrderStatus;

public record PurchaseOrderResponse(Long id, String orderNumber, String description,
        PurchaseOrderStatus status, String supplierName, LocalDateTime createdAt,
        List<PurchaseOrderItemResponse> items) {
}
