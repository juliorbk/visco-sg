package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.Quotation;
import com.visco.backend.models.entities.QuotationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record QuotationResponse(
    Long id,
    String quotationNumber,
    Long requisitionId,
    String requisitionNumber,
    Long supplierId,
    String supplierName,
    String createdByName,
    QuotationStatus status,
    Currency currency,
    LocalDateTime validUntil,
    String shippingConditions,
    String paymentConditions,
    PaymentMethod paymentMethod,
    String warrantyTerms,
    String notes,
    String editReason,
    LocalDateTime submittedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    BigDecimal offeredTotal,
    boolean itemsLoaded,
    List<QuotationItemResponse> items
) {
    public static QuotationResponse from(Quotation q, boolean itemsLoaded) {
        List<QuotationItemResponse> itemDtos = itemsLoaded && q.getItems() != null
            ? q.getItems().stream().map(QuotationItemResponse::from).toList()
            : List.of();

        return new QuotationResponse(
            q.getId(),
            q.getQuotationNumber(),
            q.getRequisition() != null ? q.getRequisition().getId() : null,
            q.getRequisition() != null ? q.getRequisition().getRequisitionNumber() : null,
            q.getSupplier() != null ? q.getSupplier().getId() : null,
            q.getSupplier() != null ? q.getSupplier().getName() : null,
            q.getCreatedBy() != null ? q.getCreatedBy().getName() : null,
            q.getStatus(),
            q.getCurrency(),
            q.getValidUntil(),
            q.getShippingConditions(),
            q.getPaymentConditions(),
            q.getPaymentMethod(),
            q.getWarrantyTerms(),
            q.getNotes(),
            q.getEditReason(),
            q.getSubmittedAt(),
            q.getCreatedAt(),
            q.getUpdatedAt(),
            q.getOfferedTotal(),
            itemsLoaded,
            itemDtos
        );
    }
}
