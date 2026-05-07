package com.visco.backend.models.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "internal_code", unique = true, nullable = false)
  private String internalCode;

  @Column(unique = true, nullable = false)
  private String sku;

  @Column(nullable = false)
  private String name;

  @Column(name = "sap_code", nullable = false)
  private String sapCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Uom uom;

  // ==========================================
  // ¿Este es el stock global o el de un almacén en específico?
  // ==========================================
  @Column(name = "current_stock", nullable = false)
  private BigDecimal currentStock;

  @Column(name = "reorder_point", nullable = false)
  private BigDecimal reorderPoint;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  // ==========================================
  // RELACIONES (JOINS)
  // ==========================================

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "product_warehouses",
    joinColumns = @JoinColumn(name = "product_id"),
    inverseJoinColumns = @JoinColumn(name = "warehouse_id")
  )
  @Builder.Default
  private Set<Warehouse> warehouses = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", referencedColumnName = "id")
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", referencedColumnName = "id")
  private Category category;

  /*
   * Cuidado aquí también: Si el producto está en varios almacenes,
   * probablemente también esté en diferentes ubicaciones (pasillos/estantes)
   * dependiendo del almacén.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_id", referencedColumnName = "id")
  private Location location;

  @Transient
  public String getCalculatedStatus() {
    if (currentStock == null) return "Sin stock";
    if (currentStock.compareTo(BigDecimal.ZERO) <= 0) return "Sin stock";
    if (currentStock.compareTo(reorderPoint) <= 0) return "Bajo stock";
    return "En stock";
  }
}
