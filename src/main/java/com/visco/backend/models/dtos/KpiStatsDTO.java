package com.visco.backend.models.dtos;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiStatsDTO {
	private long totalOrders;
	private BigDecimal totalInventoryUnits;
	private BigDecimal monthlySpend;
	private double fulfillmentRate; // porcentaje 0-100
}