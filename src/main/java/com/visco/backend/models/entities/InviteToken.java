package com.visco.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(
  name = "invite_tokens",
  indexes = {
    @Index(name = "idx_invite_token", columnList = "token", unique = true),
    @Index(name = "idx_invite_email", columnList = "email"),
  }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteToken {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  @JsonIgnore
  private String token;

  @Column(nullable = false)
  private String email;

  @Column(name = "intended_role", nullable = false)
  private String intendedRole;

  @Column(name = "cost_center_id")
  private Long costCenterId;

  @Column(name = "created_by_id", nullable = false)
  private UUID createdById;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  @Column(name = "used_by_user_id")
  private UUID usedByUserId;

  @Column(nullable = false)
  private boolean revoked;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  public boolean isUsable() {
    return !revoked && usedAt == null && expiresAt.isAfter(LocalDateTime.now());
  }
}
