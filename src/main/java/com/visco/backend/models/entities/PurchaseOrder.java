package com.visco.backend.models.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

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

  @Column(nullable = false)
  private String description;

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

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  // Relación bidireccional con los items de la orden
  @OneToMany(
    mappedBy = "purchaseOrder",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @Builder.Default
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private List<PurchaseOrderItem> items = new ArrayList<>();
}
