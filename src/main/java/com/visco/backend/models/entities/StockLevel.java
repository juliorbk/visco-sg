package com.visco.backend.models.entities;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "stock_levels", indexes = {
    @Index(name = "idx_sl_product", columnList = "product_id"),
    @Index(name = "idx_sl_location", columnList = "location_id"),
    @Index(name = "idx_sl_product_location", columnList = "product_id,location_id")
})
public class StockLevel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne
  @JoinColumn(name = "location_id")
  private Location location;

  @Column(nullable = false)
  private BigDecimal currentStock;

  @Column(nullable = false)
  private BigDecimal pendingStock;
}
