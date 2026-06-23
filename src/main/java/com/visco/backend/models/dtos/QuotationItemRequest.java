package com.visco.backend.models.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request payload for a single Quotation line item.
 *
 * <p>{@code requisitionItemId} is mandatory: every Quotation line must be
 * anchored to a line of the originating Requisition. The service copies the
 * requested quantity from that RequisitionItem into
 * {@code QuotationItem.requestedQuantity} on creation.
 *
 * <p>{@code offeredProductId} is optional. If the supplier is offering the
 * exact same product as requested, leave it null and the system uses the
 * RequisitionItem.product implicitly.
 */
public record QuotationItemRequest(
    @NotNull(message = "Requisition item ID is required") Long requisitionItemId,

    Long offeredProductId,

    Integer lineNumber,

    String offeredDescription,
    String offeredSku,
    String brand,
    String model,

    @NotNull(message = "Offered quantity is required")
    @DecimalMin(value = "0.0001", message = "Offered quantity must be greater than zero")
    BigDecimal offeredQuantity,

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0000", message = "Unit price cannot be negative")
    BigDecimal unitPrice,

    Integer deliveryDays
) {}
