package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
    Long id,
    String invoiceNumber,
    Long purchaseOrderId,
    String orderNumber,
    String supplierName,
    LocalDate invoiceDate,
    LocalDate dueDate,
    BigDecimal totalAmount,
    BigDecimal taxAmount,
    InvoiceStatus status,
    String matchingNotes,
    LocalDate paymentDate,
    String notes,
    LocalDateTime createdAt,
    List<InvoiceItemResponse> items
) {}
