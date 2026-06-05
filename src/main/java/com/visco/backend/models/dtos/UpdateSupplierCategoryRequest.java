package com.visco.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSupplierCategoryRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255)
    String name,

    String description,

    Boolean active
) {}
