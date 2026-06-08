package com.visco.backend.models.dtos;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
// DTO representing monthly spending with actual and projected values.
public class MonthlySpendingDTO {
	private String month; // "2025-07"
	private BigDecimal actual;
	private BigDecimal projected; // actual * 1.10 como estimación simple
}