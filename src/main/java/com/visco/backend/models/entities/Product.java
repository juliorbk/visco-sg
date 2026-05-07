package com.visco.backend.models.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "internal_code", unique = true, nullable = false)
  private String internalCode; // Ej: INV-0842

  @Column(unique = true, nullable = false)
  private String sku; // Ej: CHM-LUB-001

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String sap_code; // Código SAP para integración

  @Column(nullable = false)
  private Uom uom; // Unidad de medida (Ej: Litros, Kilogramos, Unidades)

  @Column(name = "current_stock", nullable = false)
  private BigDecimal currentStock;

  @Column(name = "reorder_point", nullable = false)
  private BigDecimal reorderPoint;

  @Column(nullable = false)
  private String unit;

  // Relaciones iniciales para el MVP
  @Column(name = "warehouse_id")
  private Long warehouseId;

  @Column(name = "supplier_id")
  private Long supplierId;

  @Column(name = "category_id")
  private Long categoryId;

  @Column(name = "location_id")
  private Long locationId;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  // Lógica de dominio encapsulada para el frontend
  @Transient
  public String getCalculatedStatus() {
    if (currentStock.compareTo(BigDecimal.ZERO) <= 0) return "Sin stock";
    if (currentStock.compareTo(reorderPoint) <= 0) return "Bajo stock";
    return "En stock";
  }
}
