package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Employee;

public record EmployeeResponseDto(
  Long id,
  String fullName,
  String documentNumber,
  String phone,
  Long costCenterId,
  String costCenterDescription,
  Boolean isActive
) {
  public static EmployeeResponseDto fromEntity(Employee emp) {
    return new EmployeeResponseDto(
      emp.getId(),
      emp.getFullName(),
      emp.getDocumentNumber(),
      emp.getPhone(),
      emp.getCostCenter() != null ? emp.getCostCenter().getId() : null,
      emp.getCostCenter() != null ? emp.getCostCenter().getFullDescription() : null,
      emp.isActive()
    );
  }
}
