package com.visco.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
  name = "cost_centers",
  indexes = {
    @Index(name = "idx_cost_center_management", columnList = "management_id"),
  }
)
public class CostCenter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String code;

  @Column(name = "full_description", nullable = false)
  private String fullDescription;

  @Column(name = "division_description")
  private String divisionDescription;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "management_id", nullable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private Management management;

  /*  Next Feat:
  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  */
}
