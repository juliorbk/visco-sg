package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Currency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

// Request payload for creating a new supplier.
public record CreateSupplierRequest(
  @NotBlank String name,
  @NotBlank String address,
  @Email @NotBlank String email,
  @Size(min = 1) Set<String> phoneNumbers,
  @NotBlank String description,
  @NotNull Currency currency,
  Set<String> representativeIds,
  Long categoryId
) {}
