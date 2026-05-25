package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeRequestDto(
  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = 255)
  String fullName,

  @NotBlank(message = "El número de documento es obligatorio")
  @Size(max = 50)
  String documentNumber,

  @Size(max = 20)
  String phone,

  Long costCenterId,

  Boolean isActive
) {}
