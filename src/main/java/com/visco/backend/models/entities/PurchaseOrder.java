package com.visco.backend.models.entities;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
  name = "purchase_orders",
  indexes = {
    @Index(name = "idx_po_supplier", columnList = "supplier_id"),
    @Index(name = "idx_po_created_by", columnList = "created_by_id"),
    @Index(name = "idx_po_warehouse", columnList = "destination_warehouse_id"),
    @Index(name = "idx_po_status", columnList = "status"),
    @Index(name = "idx_po_created_at", columnList = "created_at"),
  }
)
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
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private User createdBy;

  @Column(nullable = false)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentMethod paymentMethod;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PurchaseOrderType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PurchaseOrderStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Supplier supplier;

  // Auditoría básica
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", updatable = true)
  private LocalDateTime updatedAt;

  @Column(name = "lead_time")
  private Integer leadTime = 0;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "approval_notes", length = 1000)
  private String approvalNotes;

  @Column(name = "ship_conditions", length = 1000)
  private String shipConditions;

  @Column(name = "rejection_reason", length = 1000)
  private String rejectionReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by_id")
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private User approvedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rejected_by_id")
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private User rejectedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requisition_id")
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Requisition requisition;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "destination_warehouse_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Warehouse destinationWarehouse;

  @Version
  @Column(nullable = false)
  private Long version;

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
