package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Currency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateSupplierRequest(
  @NotBlank(message = "El nombre es obligatorio") String name,

  @NotBlank(message = "La dirección es obligatoria") String address,

  @Email(message = "Formato de email inválido")
  @NotBlank(message = "El email es obligatorio")
  String email,

  @Size(min = 1, message = "Debe incluir al menos un teléfono")
  Set<String> phoneNumbers,

  @NotBlank(message = "La descripción es obligatoria") String description,

  @NotNull(message = "La moneda es obligatoria") Currency currency,

  Set<Long> representativeIds,

  Long categoryId
) {}
