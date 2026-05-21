package com.visco.backend.models.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Nota de recepción — se crea cuando llega mercancía al warehouse
// Registra qué productos llegaron y en qué cantidad contra una orden de compra
@Entity
@Table(
  name = "good_receipts",
  indexes = {
    @Index(name = "idx_gr_purchase_order", columnList = "purchase_order_id"),
  }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoodReceipt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "receipt_number", unique = true, nullable = false)
  private String receiptNumber; // Nro interno ej: "GR-1-1680000000"

  // Orden de compra contra la que se recibe
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @Column(name = "received_at", nullable = false)
  private LocalDateTime receivedAt; // Fecha/hora de la recepción

  @Column(length = 1000)
  private String notes;

  @Column(nullable = false)
  private boolean closed;

  @Column(nullable = false)
  private Long destinationWarehouseId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "received_by_id")
  private User receivedBy;

  @OneToMany(
    mappedBy = "goodReceipt",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @Builder.Default
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private List<GoodReceiptItem> items = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    if (receivedAt == null) {
      receivedAt = LocalDateTime.now();
    }
  }
}
