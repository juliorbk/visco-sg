package com.visco.backend.models.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
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

  @Column(length = 1000)
  private String description;

  @Column(name = "sap_code", nullable = false)
  private String sapCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Uom uom;

  // Punto de reorden global (cuándo hay que comprar más)
  @Column(name = "reorder_point", nullable = false)
  private BigDecimal reorderPoint;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", referencedColumnName = "id")
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", referencedColumnName = "id")
  private Category category;

  // ELIMINADOS: currentStock, warehouses, location y getCalculatedStatus()
}
