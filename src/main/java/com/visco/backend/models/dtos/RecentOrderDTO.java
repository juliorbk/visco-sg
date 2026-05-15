package com.visco.backend.models.dtos;

import java.time.LocalDateTime;

import com.visco.backend.models.entities.PurchaseOrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentOrderDTO {
	private Long id;
	private String orderNumber;
	private LocalDateTime createdAt;
	private String supplierName;
	private PurchaseOrderStatus status;
}