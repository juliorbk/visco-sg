package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.CostCenter;

// Response DTO with cost center and associated management hierarchy.
public record CostCenterResponseDto(
  Long id,
  Boolean isActive,
  String code,
  String divisionDescription,
  String fullDescription,
  Long managementId,
  String managementCode,
  String managementDescription,
  Long generalManagementId,
  String generalManagementCode,
  String generalManagementDescription
) {
  public static CostCenterResponseDto fromEntity(CostCenter cc) {
    return new CostCenterResponseDto(
      cc.getId(),
      cc.isActive(),
      cc.getCode(),
      cc.getDivisionDescription(),
      cc.getFullDescription(),
      cc.getManagement().getId(),
      cc.getManagement().getCode(),
      cc.getManagement().getDescription(),
      cc.getManagement().getGeneralManagement().getId(),
      cc.getManagement().getGeneralManagement().getCode(),
      cc.getManagement().getGeneralManagement().getDescription()
    );
  }
}
