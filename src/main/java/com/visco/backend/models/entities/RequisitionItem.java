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
@Table(
  name = "requisition_items",
  indexes = {
    @Index(name = "idx_ri_requisition", columnList = "requisition_id"),
    @Index(name = "idx_ri_product", columnList = "product_id"),
  }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequisitionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requisition_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @JsonBackReference("requisition-items")
  private Requisition requisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Product product;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal quantity;

    @Column(length = 500)
    private String notes;
}
