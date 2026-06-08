package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
// DTO with detailed spending statistics broken down by category and month.
public class SpendingStatsDTO {
	private BigDecimal totalMonthly;
	private List<MonthlySpendingDTO> monthlyBreakdown;
	private Map<String, BigDecimal> byCategory; // { "Componentes": 128000, ... }
	private Map<String, Double> byCategoryPercent; // { "Componentes": 45.0, ... }
}