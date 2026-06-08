package com.visco.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
  name = "inventory_movements",
  indexes = {
    @Index(name = "idx_im_product", columnList = "product_id"),
    @Index(name = "idx_im_from_warehouse", columnList = "from_warehouse_id"),
    @Index(name = "idx_im_to_warehouse", columnList = "to_warehouse_id"),
    @Index(name = "idx_im_created_by", columnList = "created_by_id"),
  }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Records inventory movements (input, output, transfer, adjustment, dispatch).
public class InventoryMovement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false) // FK to Product
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_warehouse_id") // FK to source Warehouse
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Warehouse fromWarehouse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_warehouse_id") // FK to destination Warehouse
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Warehouse toWarehouse;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MovementType type;

  @Column(length = 500)
  private String reason;

  @Column(name = "entry_unit_price", precision = 18, scale = 4)
  private BigDecimal entryUnitPrice;

  @Column(name = "exit_unit_price", precision = 18, scale = 4)
  private BigDecimal exitUnitPrice;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false) // FK to User
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private User createdBy;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
