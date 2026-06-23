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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
  name = "invoice_items",
  indexes = {
    @Index(name = "idx_ii_invoice", columnList = "invoice_id"),
    @Index(name = "idx_ii_product", columnList = "product_id"),
  }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
// Line item within an invoice with PO matching fields (quantity, price, line total).
public class InvoiceItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false) // FK to Invoice
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @JsonBackReference("invoice-items")
  private Invoice invoice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false) // FK to Product
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Product product;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
  private BigDecimal unitPrice;

  @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
  private BigDecimal lineTotal;

  @Column(name = "po_quantity", precision = 18, scale = 4)
  private BigDecimal poQuantity;

  @Column(name = "received_quantity", precision = 18, scale = 4)
  private BigDecimal receivedQuantity;

  @Column(name = "quantity_match")
  private Boolean quantityMatch;

  @Column(name = "price_match")
  private Boolean priceMatch;

  @Column(length = 500)
  private String notes;

  @PrePersist
  @PreUpdate
  private void computeLineTotal() {
    if (quantity != null && unitPrice != null) {
      this.lineTotal = quantity.multiply(unitPrice);
    }
  }
}
