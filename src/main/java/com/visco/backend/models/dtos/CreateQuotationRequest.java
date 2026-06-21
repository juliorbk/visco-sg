package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating a new Quotation in DRAFT state.
 *
 * <p>The items are optional at creation time — the service can also create a
 * DRAFT quotation pre-populated with one item per RequisitionItem when
 * invoked via the convenience endpoint
 * {@code POST /api/quotations/from-requisition/{reqId}/{supplierId}}.
 * In that case, the client doesn't need to send {@code items} here.
 */
public record CreateQuotationRequest(
    @NotNull(message = "Requisition ID is required") Long requisitionId,
    @NotNull(message = "Supplier ID is required")    Long supplierId,
    @NotNull(message = "Created by user ID is required") UUID createdById,

    // Currency chosen by the supplier; required from day one so totals and
    // comparison groups can be computed deterministically.
    @NotNull(message = "Currency is required") Currency currency,

    @NotNull(message = "Payment method is required") PaymentMethod paymentMethod,

    // Commercial conditions: all optional at creation; the buyer may fill
    // them in later via PATCH.
    LocalDateTime validUntil,
    String shippingConditions,
    String paymentConditions,
    String warrantyTerms,
    String notes,

    // Optional: if present, the service seeds one QuotationItem per
    // RequisitionItem with the requestedQuantity copied to offeredQuantity.
    @Valid List<QuotationItemRequest> items
) {}
