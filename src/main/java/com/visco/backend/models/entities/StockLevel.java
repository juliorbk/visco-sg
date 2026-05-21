package com.visco.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
  name = "stock_levels",
  indexes = {
    @Index(name = "idx_sl_product", columnList = "product_id"),
    @Index(name = "idx_sl_warehouse", columnList = "warehouse_id"),
    @Index(
      name = "idx_sl_product_warehouse",
      columnList = "product_id,warehouse_id"
    ),
  }
)
public class StockLevel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @Version
  private Long version;

  @Column(nullable = false)
  private BigDecimal currentStock;

  @Column(nullable = false)
  private BigDecimal pendingStock;

  @Column(nullable = false)
  private BigDecimal freeStock;
}
