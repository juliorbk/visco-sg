package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(
  @NotBlank(message = "El nombre es obligatorio") String name,
  Long parentId
) {}
