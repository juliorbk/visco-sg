package com.visco.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchaseOrder {

  @Id
  private Long id;

  @Column(unique = true, nullable = false)
  private String orderNumber;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private String status;
}
