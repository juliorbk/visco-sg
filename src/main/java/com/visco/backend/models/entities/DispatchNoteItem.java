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
  name = "dispatch_note_items",
  indexes = {
    @Index(name = "idx_dni_dispatch_note", columnList = "dispatch_note_id"),
    @Index(name = "idx_dni_product", columnList = "product_id"),
  }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DispatchNoteItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dispatch_note_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @JsonBackReference("dispatch-note-items")
  private DispatchNote dispatchNote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Product product;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal quantity;

  @Column(name = "exit_unit_price", precision = 18, scale = 4)
  private BigDecimal exitUnitPrice;
}
