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
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa el nivel de stock de un producto en un almacén específico.
 *
 * Definición de campos:
 *
 *   currentStock  — Unidades físicamente presentes en el almacén.
 *                   Se incrementa al recibir mercancía (GoodReceipt).
 *                   Se decrementa en transferencias salientes y ajustes.
 *                   Es la fuente de verdad del inventario físico.
 *
 *   pendingStock  — Unidades en tránsito: órdenes de compra aprobadas
 *                   que aún no han sido recibidas físicamente.
 *                   Se incrementa al aprobar una PO.
 *                   Se decrementa al recibir la mercancía.
 */
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

  // Stock físico presente en el almacén
  @Builder.Default
  @Column(nullable = false)
  private BigDecimal currentStock = BigDecimal.ZERO;

  // Stock en tránsito (POs aprobadas no recibidas aún)
  @Builder.Default
  @Column(nullable = false)
  private BigDecimal pendingStock = BigDecimal.ZERO;
}
