package com.visco.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
  name = "products",
  indexes = {
    @Index(name = "idx_product_supplier", columnList = "supplier_id"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_active", columnList = "is_active"),
  }
)
@SQLDelete(sql = "UPDATE products SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
// Represents a product/item with inventory, procurement, and supplier attributes.
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
  @SequenceGenerator(
    name = "product_seq",
    sequenceName = "product_code_seq",
    allocationSize = 1
  )
  private Long id;

  @Column(name = "internal_code", unique = true, nullable = false)
  private String internalCode;

  @Column(unique = true, nullable = false)
  private String sku;

  @Column(nullable = false, length = 500)
  private String name;

  @Column(length = 1000)
  private String description;

  @Column(name = "sap_code", nullable = false)
  private String sapCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Uom uom;

  @Column(name = "reorder_point", nullable = false)
  private BigDecimal reorderPoint;

  @Column(name = "max_stock", nullable = false)
  private BigDecimal maxStock;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", referencedColumnName = "id") // FK to Supplier
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", referencedColumnName = "id") // FK to Category
  private Category category;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
