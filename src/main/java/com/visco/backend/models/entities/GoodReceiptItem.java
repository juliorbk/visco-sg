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
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Item individual dentro de una nota de recepción
// Guarda cuánto se esperaba (según la PO) y cuánto realmente llegó
@Entity
@Table(
  name = "good_receipt_items",
  indexes = {
    @Index(name = "idx_gri_good_receipt", columnList = "good_receipt_id"),
    @Index(name = "idx_gri_product", columnList = "product_id"),
  }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoodReceiptItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Nota de recepción a la que pertenece
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "good_receipt_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @JsonBackReference("good-receipt-items")
  private GoodReceipt goodReceipt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Product product;

  @Column(name = "expected_quantity", nullable = false)
  private BigDecimal expectedQuantity; // Lo que decía la orden de compra

  @Column(name = "received_quantity", nullable = false)
  private BigDecimal receivedQuantity; // Lo que realmente llegó

  // Ubicación dentro del almacén donde se colocó la mercancía
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_id")
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Location location;
}
