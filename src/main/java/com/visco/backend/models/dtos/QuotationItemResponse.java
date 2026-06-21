package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.QuotationItem;
import com.visco.backend.models.entities.QuotationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuotationItemResponse(
    Long id,
    Long requisitionItemId,
    Integer requisitionItemLineNumber,
    String requisitionItemDescription,
    Long offeredProductId,
    String offeredProductName,
    Integer lineNumber,
    String offeredDescription,
    String offeredSku,
    String brand,
    String model,
    BigDecimal requestedQuantity,
    BigDecimal offeredQuantity,
    BigDecimal unitPrice,
    BigDecimal subtotal,
    Integer deliveryDays,
    Boolean quantityMismatchWarning,
    String quantityMismatchMessage
) {
    public static QuotationItemResponse from(QuotationItem item) {
        BigDecimal subtotal = (item.getUnitPrice() != null && item.getOfferedQuantity() != null)
            ? item.getUnitPrice().multiply(item.getOfferedQuantity())
            : BigDecimal.ZERO;

        boolean mismatch = Boolean.TRUE.equals(item.getQuantityMismatchWarning());
        String mismatchMsg = mismatch
            ? String.format(
                "Cantidad ofertada (%s) difiere de la solicitada (%s)",
                item.getOfferedQuantity(),
                item.getRequestedQuantity()
            )
            : null;

        return new QuotationItemResponse(
            item.getId(),
            item.getRequisitionItem() != null ? item.getRequisitionItem().getId() : null,
            item.getRequisitionItem() != null ? item.getRequisitionItem().getLineNumber() : null,
            item.getRequisitionItem() != null && item.getRequisitionItem().getProduct() != null
                ? item.getRequisitionItem().getProduct().getName() : null,
            item.getOfferedProduct() != null ? item.getOfferedProduct().getId() : null,
            item.getOfferedProduct() != null ? item.getOfferedProduct().getName() : null,
            item.getLineNumber(),
            item.getOfferedDescription(),
            item.getOfferedSku(),
            item.getBrand(),
            item.getModel(),
            item.getRequestedQuantity(),
            item.getOfferedQuantity(),
            item.getUnitPrice(),
            subtotal,
            item.getDeliveryDays(),
            item.getQuantityMismatchWarning(),
            mismatchMsg
        );
    }
}
