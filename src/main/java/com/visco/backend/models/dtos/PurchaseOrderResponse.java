package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
  Long id,
  String orderNumber,
  String description,
  PurchaseOrderStatus status,
  String supplierName,
  PaymentMethod paymentMethod,
  PurchaseOrderType type,
  String createdBy,
  LocalDateTime createdAt,
  String approvalNotes,
  String rejectionReason,
  String approvedBy,
  LocalDateTime approvedAt,
  Long requisitionId,
  Long destinationWarehouseId,
  String destinationWarehouseName,
  Integer leadTime,
  List<PurchaseOrderItemResponse> items
) {}
