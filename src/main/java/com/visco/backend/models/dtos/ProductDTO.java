package com.visco.backend.models.dtos;

import java.math.BigDecimal;

import com.visco.backend.models.entities.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
	private Long id;
	private String internalCode; // VIS-000001
	private String sku;
	private String name;
	private String description;
	private String sapCode;
	private String uom; // String para evitar exponer el enum
	private BigDecimal reorderPoint;
	private BigDecimal totalStock;
	private BigDecimal totalPendingStock;
	private Boolean active;
	private Long supplierId;
	private String supplierName;
	private Long categoryId;
	private String categoryName;

	public static ProductDTO fromEntity(Product product, BigDecimal totalStock, BigDecimal totalPendingStock) {
		return ProductDTO.builder()
				.id(product.getId())
				.internalCode(product.getInternalCode()) // -> 000001 +
				.sku(product.getSku())
				.name(product.getName())
				.description(product.getDescription())
				.sapCode(product.getSapCode())
				.uom(product.getUom().name())
				.reorderPoint(product.getReorderPoint())
				.totalStock(totalStock)
				.totalPendingStock(totalPendingStock)
				.active(product.getActive())
				.supplierId(product.getSupplier() != null ? product.getSupplier().getId() : null)
				.supplierName(product.getSupplier() != null ? product.getSupplier().getName() : null)
				.categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
				.categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
				.build();
	}
}