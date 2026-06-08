package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

// Request payload for creating a new product category.
public record CreateCategoryRequest(
  @NotBlank(message = "El nombre es obligatorio") String name,
  @Positive(message = "El parentId debe ser positivo") Long parentId
) {}
