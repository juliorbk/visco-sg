package com.visco.backend.models.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
    name = "quotation_items",
    indexes = {
        @Index(name = "idx_qi_quotation", columnList = "quotation_id"),
        @Index(name = "idx_qi_req_item", columnList = "requisition_item_id"),
        @Index(name = "idx_qi_product", columnList = "offered_product_id"),
        @Index(name = "idx_qi_line", columnList = "line_number")
    }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
// A single line in a Quotation: anchored to a RequisitionItem (the requested line)
// and optionally to a Product (the supplier's offered product, which may differ
// from the originally requested one).
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonBackReference("quotation-items")
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_item_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private RequisitionItem requisitionItem;

    // Optional: the product the supplier is offering. If null, we assume the
    // offered product equals the RequisitionItem.product.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_product_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Product offeredProduct;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "offered_description", length = 500)
    private String offeredDescription;

    @Column(name = "offered_sku", length = 100)
    private String offeredSku;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String model;

    // Copied from RequisitionItem.quantity at creation time. Not editable
    // (the requested quantity is the source of truth; offeredQuantity may differ).
    @Column(name = "requested_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal requestedQuantity;

    @Column(name = "offered_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal offeredQuantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    // Computed at save time: true if offeredQuantity != requestedQuantity.
    // Surfaced as a warning in UI and PDF, never as an error.
    @Column(name = "quantity_mismatch_warning", nullable = false)
    @Builder.Default
    private Boolean quantityMismatchWarning = false;

    // Recompute the warning flag from the current quantity values.
    public void recomputeQuantityMismatchWarning() {
        if (offeredQuantity == null || requestedQuantity == null) {
            this.quantityMismatchWarning = false;
        } else {
            this.quantityMismatchWarning = offeredQuantity.compareTo(requestedQuantity) != 0;
        }
    }
}
