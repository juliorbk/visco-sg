package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Product;
import java.math.BigDecimal;
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
  private String internalCode;
  private String sku;
  private String name;
  private String description;
  private String sapCode;
  private String uom;
  private BigDecimal reorderPoint;

  private BigDecimal totalStock; // currentStock total del producto
  private BigDecimal totalPendingStock; // pendingStock total

  private Boolean active;
  private Long supplierId;
  private String supplierName;
  private Long categoryId;
  private String categoryName;

  public static ProductDTO fromEntity(
    Product product,
    BigDecimal totalStock,
    BigDecimal totalPendingStock
  ) {
    return ProductDTO.builder()
      .id(product.getId())
      .internalCode(product.getInternalCode())
      .sku(product.getSku())
      .name(product.getName())
      .description(product.getDescription())
      .sapCode(product.getSapCode())
      .uom(product.getUom().name())
      .reorderPoint(product.getReorderPoint())
      .totalStock(totalStock)
      .totalPendingStock(totalPendingStock)
      .active(product.getActive())
      .supplierId(
        product.getSupplier() != null ? product.getSupplier().getId() : null
      )
      .supplierName(
        product.getSupplier() != null ? product.getSupplier().getName() : null
      )
      .categoryId(
        product.getCategory() != null ? product.getCategory().getId() : null
      )
      .categoryName(
        product.getCategory() != null ? product.getCategory().getName() : null
      )
      .build();
  }
}
