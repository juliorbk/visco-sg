package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request payload for updating a Quotation.
 *
 * <p>Business rules enforced by the service layer:
 * <ul>
 *   <li>If the current status is not {@code DRAFT}, the {@code editReason}
 *       field becomes mandatory (min 5 characters).</li>
 *   <li>If the quotation is in a terminal state ({@code CANCELLED},
 *       {@code AWARDED}, {@code PARTIALLY_AWARDED}, {@code REJECTED}),
 *       the service rejects the update with 409 Conflict.</li>
 *   <li>Items are replaced wholesale: the client sends the desired final
 *       state of the items list. To drop a line, simply omit it.</li>
 * </ul>
 */
public record UpdateQuotationRequest(
    Currency currency,
    PaymentMethod paymentMethod,
    LocalDateTime validUntil,
    @Size(max = 1000) String shippingConditions,
    @Size(max = 1000) String paymentConditions,
    @Size(max = 1000) String warrantyTerms,
    @Size(max = 1000) String notes,

    // Mandatory (min 5 chars) when editing a non-DRAFT quotation.
    // Ignored when status == DRAFT.
    @Size(max = 1000) String editReason,

    @Valid List<QuotationItemRequest> items
) {}
