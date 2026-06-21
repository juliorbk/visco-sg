package com.visco.backend.models.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    name = "quotations",
    indexes = {
        @Index(name = "idx_q_requisition", columnList = "requisition_id"),
        @Index(name = "idx_q_supplier", columnList = "supplier_id"),
        @Index(name = "idx_q_status", columnList = "status"),
        @Index(name = "idx_q_currency", columnList = "currency"),
        @Index(name = "idx_q_created_at", columnList = "created_at"),
        @Index(name = "idx_q_number", columnList = "quotation_number", unique = true)
    }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
// Represents a supplier's quotation (RFQ response) against a Requisition.
// Created in DRAFT state, populated with items + commercial conditions, then
// SUBMITTED for comparison against other quotations of the same requisition.
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quotation_number", unique = true, nullable = false)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Requisition requisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationStatus status;

    // Currency chosen by the supplier for this quotation. Allows multi-currency
    // comparison scenarios: the comparison view groups quotations by currency.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "shipping_conditions", length = 1000)
    private String shippingConditions;

    @Column(name = "payment_conditions", length = 1000)
    private String paymentConditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "warranty_terms", length = 1000)
    private String warrantyTerms;

    @Column(length = 1000)
    private String notes;

    // Mandatory when editing a non-DRAFT quotation. Logged at WARN level for audit.
    @Column(name = "edit_reason", length = 1000)
    private String editReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(
        mappedBy = "quotation",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonManagedReference("quotation-items")
    private List<QuotationItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = QuotationStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Computed total of all items (qty * unitPrice). Not persisted.
    public BigDecimal getOfferedTotal() {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
        return items.stream()
            .map(i -> {
                if (i.getUnitPrice() == null || i.getOfferedQuantity() == null) {
                    return BigDecimal.ZERO;
                }
                return i.getUnitPrice().multiply(i.getOfferedQuantity());
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
