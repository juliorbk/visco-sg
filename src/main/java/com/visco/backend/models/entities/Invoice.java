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
import java.math.BigDecimal;
import java.time.LocalDate;
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
  name = "invoices",
  indexes = {
    @Index(name = "idx_invoice_po", columnList = "purchase_order_id"),
    @Index(name = "idx_invoice_supplier", columnList = "supplier_id"),
    @Index(name = "idx_invoice_status", columnList = "status"),
  }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Invoice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "invoice_number", unique = true, nullable = false)
  private String invoiceNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Supplier supplier;

  @Column(name = "invoice_date", nullable = false)
  private LocalDate invoiceDate;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(name = "tax_amount")
  private BigDecimal taxAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InvoiceStatus status;

  @Column(name = "matching_notes", length = 1000)
  private String matchingNotes;

  @Column(name = "payment_date")
  private LocalDate paymentDate;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @OneToMany(
    mappedBy = "invoice",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @Builder.Default
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private List<InvoiceItem> items = new ArrayList<>();

  @Version
  @Column(nullable = false)
  private Long version;

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
    if (status == null) status = InvoiceStatus.PENDING;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
