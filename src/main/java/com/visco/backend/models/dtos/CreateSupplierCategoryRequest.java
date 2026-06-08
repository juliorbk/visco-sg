package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request payload for creating a supplier category.
public record CreateSupplierCategoryRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255)
    String name,

    String description
) {}
