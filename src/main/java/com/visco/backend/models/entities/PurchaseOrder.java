package com.visco.backend.models.entities;

import jakarta.persistence.*;
import lombok.*;

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
