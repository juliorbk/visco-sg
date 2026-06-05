package com.visco.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
  name = "employees",
  indexes = {
    @Index(name = "idx_employee_cost_center", columnList = "cost_center_id"),
  }
)
@SQLDelete(sql = "UPDATE employees SET is_active = false WHERE id = ?")
@SQLRestriction("is_active = true")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Employee {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "document_number", nullable = false, unique = true)
  private String documentNumber;

  @Column(name = "shirt_size")
  private String shirtSize;

  @Column(name = "pants_size")
  private String pantsSize;

  @Column(name = "shoes_size")
  private String shoesSize;

  @Column(name = "gender")
  private String gender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_center_id")
  private CostCenter costCenter;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
