package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.CostCenter;

public record CostCenterResponseDto(
  Long id,
  Boolean isActive,
  String code,
  String divisionDescription,
  String fullDescription,
  String internalCc,
  Long managementId,
  String managementCode,
  String managementDescription,
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
      cc.getInternalCc(),
      cc.getManagement().getId(),
      cc.getManagement().getCode(),
      cc.getManagement().getDescription(),
      cc.getManagement().getGeneralManagement().getCode(),
      cc.getManagement().getGeneralManagement().getDescription()
    );
  }
}
