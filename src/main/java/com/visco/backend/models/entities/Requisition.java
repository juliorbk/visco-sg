package com.visco.backend.models.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
  name = "requisitions",
  indexes = {
    @Index(name = "idx_req_requested_by", columnList = "requested_by_id"),
    @Index(name = "idx_req_cost_center", columnList = "cost_center_id"),
    @Index(name = "idx_req_approved_by", columnList = "approved_by_id"),
    @Index(name = "idx_req_status", columnList = "status"),
    @Index(name = "idx_req_created_at", columnList = "created_at"),
  }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
// Represents a purchase requisition requesting items for a cost center.
public class Requisition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "requisition_number", unique = true, nullable = false)
  private String requisitionNumber;

  @Column(nullable = false)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by_id", nullable = false) // FK to User (requester)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private User requestedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cost_center_id", nullable = false) // FK to CostCenter
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private CostCenter costCenter;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RequisitionStatus status;

  @Column(name = "rejection_reason", length = 1000)
  private String rejectionReason;

  @Column(name = "approval_notes", length = 1000)
  private String approvalNotes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by_id") // FK to User (approver)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private User approvedBy;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  @OneToMany(
    mappedBy = "requisition",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  @Builder.Default
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @JsonManagedReference("requisition-items")
  private List<RequisitionItem> items = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
    if (status == null) status = RequisitionStatus.DRAFT;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
