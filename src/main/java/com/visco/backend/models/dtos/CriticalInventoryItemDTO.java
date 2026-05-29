package com.visco.backend.models.dtos;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CriticalInventoryItemDTO {
	private Long productId;
	private String productName;
	private String sku;
	private BigDecimal currentStock;
	private BigDecimal reorderPoint;
	private BigDecimal maxStock;
	private String severity; // "CRITICAL" si stock=0, "WARNING" si stock <= reorderPoint, "OVERSTOCK" si stock >= maxStock
}