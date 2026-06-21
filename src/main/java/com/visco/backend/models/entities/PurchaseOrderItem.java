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

@Entity
@Table(name = "purchase_order_items", indexes = {
    @Index(name = "idx_poi_purchase_order", columnList = "purchase_order_id"),
    @Index(name = "idx_poi_product", columnList = "product_id"),
    @Index(name = "idx_poi_requisition_item", columnList = "requisition_item_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchaseOrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @JsonBackReference("purchase-order-items")
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Product product;

  // Optional back-reference to the RequisitionItem this PO line is fulfilling.
  // Null when the PO is not tied to a requisition (direct POs) or for legacy data.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requisition_item_id")
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private RequisitionItem requisitionItem;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal quantity; // Cantidad a comprar (admite fracciones)

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal unitPrice; // Precio acordado con el proveedor
}
