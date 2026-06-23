package com.visco.backend.models.entities;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
    name = "quotation_awards",
    indexes = {
        @Index(name = "idx_qa_requisition", columnList = "requisition_id"),
        @Index(name = "idx_qa_quotation_item", columnList = "winning_quotation_item_id"),
        @Index(name = "idx_qa_supplier", columnList = "awarded_supplier_id")
    },
    // One award decision per RequisitionItem. Re-awarding the same line
    // overwrites the previous QuotationAward (handled in service layer).
    uniqueConstraints = @UniqueConstraint(
        name = "uq_qa_req_item",
        columnNames = "requisition_item_id"
    )
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
// Persists the per-line decision made during the award step. Each record says
// "this RequisitionItem was awarded to this QuotationItem (from this supplier)
// at this price/qty". Created during the comparison + award flow.
public class QuotationAward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Requisition requisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_item_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private RequisitionItem requisitionItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_quotation_item_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private QuotationItem winningQuotationItem;

    // Denormalized for fast supplier-level rollups without joining through the item.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_supplier_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Supplier awardedSupplier;

    @Column(name = "awarded_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal awardedQuantity;

    @Column(name = "awarded_unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal awardedUnitPrice;

    // awardedQuantity * awardedUnitPrice, computed at save time.
    @Column(name = "awarded_subtotal", nullable = false, precision = 18, scale = 4)
    private BigDecimal awardedSubtotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AwardStatus status;

    @Column(length = 1000)
    private String justification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_by_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User awardedBy;

    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (awardedSubtotal == null && awardedQuantity != null && awardedUnitPrice != null) {
            awardedSubtotal = awardedQuantity.multiply(awardedUnitPrice);
        }
    }
}
