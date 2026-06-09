// ... other imports
package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.PurchaseOrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

// Request payload for creating a purchase order.
public record CreatePurchaseOrderRequest(
  String orderNumber,
  @NotBlank(message = "Description is required") String description,
  @NotNull(message = "Supplier ID is required") Long supplierId,

  // Renamed for consistency with supplierId and createdById
  @NotNull(message = "Destination warehouse ID is required")
  Long destinationWarehouseId,

  @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,
  @NotNull(message = "Purchase order type is required") PurchaseOrderType type,
  @NotNull(message = "Created by is required") UUID createdById,
  Long requisitionId,
  Integer leadTime, //Days
  @NotEmpty(message = "At least one item is required")
  @Valid
  List<PurchaseOrderItemRequest> items
) {}
