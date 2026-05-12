package com.visco.backend.models.dtos;

import java.time.LocalDateTime;
import java.util.List;

import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;

public record PurchaseOrderResponse(Long id, String orderNumber, String description,
                PurchaseOrderStatus status, String supplierName, PaymentMethod paymentMethod,
                PurchaseOrderType type, String createdBy, LocalDateTime createdAt,
                List<PurchaseOrderItemResponse> items) {
}
