package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.InviteToken;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
// Response DTO for an invitation token with full details.
public class InviteTokenResponse {
  UUID id;
  String token;
  String email;
  String intendedRole;
  Long costCenterId;
  UUID createdById;
  LocalDateTime createdAt;
  LocalDateTime expiresAt;
  LocalDateTime usedAt;
  UUID usedByUserId;
  boolean revoked;

  public static InviteTokenResponse fromEntity(InviteToken entity) {
    return InviteTokenResponse.builder()
      .id(entity.getId())
      .token(entity.getToken())
      .email(entity.getEmail())
      .intendedRole(entity.getIntendedRole())
      .costCenterId(entity.getCostCenterId())
      .createdById(entity.getCreatedById())
      .createdAt(entity.getCreatedAt())
      .expiresAt(entity.getExpiresAt())
      .usedAt(entity.getUsedAt())
      .usedByUserId(entity.getUsedByUserId())
      .revoked(entity.isRevoked())
      .build();
  }
}
