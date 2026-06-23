package com.visco.backend.models.dtos;

import java.time.LocalDate;

// Request payload to mark an invoice as paid.
public record MarkInvoicePaidRequest(LocalDate paymentDate) {}
