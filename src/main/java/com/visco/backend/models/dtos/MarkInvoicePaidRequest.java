package com.visco.backend.models.dtos;

import java.time.LocalDate;

public record MarkInvoicePaidRequest(LocalDate paymentDate) {}
