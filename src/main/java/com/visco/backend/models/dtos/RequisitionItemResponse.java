package com.visco.backend.models.dtos;

import java.math.BigDecimal;

// Response DTO for a single line item within a requisition.
//
// Includes fulfillment progress fields (`awardedQuantity`, `pendingQuantity`,
// `fullyAwarded`) so the UI can render partial-conversion state without an
// extra round-trip. These are computed server-side and reflect the sum of
// quantities already awarded to purchase orders (excluding CANCELLED /
// REJECTED POs).
public record RequisitionItemResponse(
    Long id,
    Long productId,
    String productName,
    String productSku,
    String productInternalCode,
    String productSapCode,
    String uom,
    BigDecimal quantity,
    String notes,
    BigDecimal awardedQuantity,
    BigDecimal pendingQuantity,
    boolean fullyAwarded
) {}
