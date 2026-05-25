package com.visco.backend.models.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
  name = "dispatch_notes",
  indexes = {
    @Index(name = "idx_dn_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_dn_employee", columnList = "withdrawn_by_id"),
    @Index(name = "idx_dn_created_by", columnList = "created_by_id"),
  }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchNote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "dispatch_number", unique = true, nullable = false)
  private String dispatchNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "withdrawn_by_id", nullable = false)
  private Employee withdrawnBy;

  @Column(length = 1000)
  private String notes;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  @OneToMany(
    mappedBy = "dispatchNote",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @Builder.Default
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private List<DispatchNoteItem> items = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
