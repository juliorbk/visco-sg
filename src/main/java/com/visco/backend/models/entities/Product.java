package com.visco.backend.models.entities;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
  @SequenceGenerator(name = "product_seq", sequenceName = "product_code_seq", allocationSize = 1)
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
  private Boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", referencedColumnName = "id")
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", referencedColumnName = "id")
  private Category category;

  // ELIMINADOS: currentStock, warehouses, location y getCalculatedStatus()
}
