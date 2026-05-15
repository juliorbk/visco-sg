package com.visco.backend.models.dtos;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupplierPerformanceDTO {
	private Long supplierId;
	private String supplierName;
	private List<MonthlyEntry> months;
	private long totalOrders;
	private long totalDelivered;
	private double fulfillmentRate; // porcentaje 0-100
	private BigDecimal totalSpend;

	@Data
	@Builder
	public static class MonthlyEntry {
		private String month; // "2025-07"
		private long totalOrders;
		private long deliveredOrders;
		private BigDecimal totalSpend;
		private double fulfillmentRate;
	}
}