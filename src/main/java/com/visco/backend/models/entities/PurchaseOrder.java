package com.visco.backend.models.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "purchase_orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchaseOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
  private Long id;

  @Column(unique = true, nullable = false)
  private String orderNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @Column(nullable = false)
  private String description;

  // Corregido: Usamos tu Enum en lugar de un String
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentMethod paymentMethod;

  // Corregido: Usamos tu Enum en lugar de un String
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PurchaseOrderType type;

  // Corregido: Usamos tu Enum en lugar de un String
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PurchaseOrderStatus status;

  // Relación con el Proveedor
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", nullable = false)
  private Supplier supplier;

  // Auditoría básica
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", updatable = true)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
  // Dentro de tu clase PurchaseOrder.java

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "destination_warehouse_id", nullable = false)
  private Warehouse destinationWarehouse;
  // Relación bidireccional con los items de la orden
  @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private List<PurchaseOrderItem> items = new ArrayList<>();

}
