package com.visco.backend.models.entities;

import jakarta.persistence.*;
import lombok.Data;


@Data
@Entity
@Table(name = "stock_levels")
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
  private Double currentStock;

  @Column(nullable = false)
  private Double pendingStock;
}
