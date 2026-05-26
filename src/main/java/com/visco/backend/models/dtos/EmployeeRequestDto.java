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

  @NotBlank(message = "El ID desl centro de costo es obligatorio")
  Long costCenterId,

  Boolean isActive
) {}
