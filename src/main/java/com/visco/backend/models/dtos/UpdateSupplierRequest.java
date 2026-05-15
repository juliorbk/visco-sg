package com.visco.backend.models.dtos;

import java.util.Set;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequest(


        @NotBlank(message = "La dirección es obligatoria") String address,

        @Email(message = "Formato de email inválido") @NotBlank(
                message = "El email es obligatorio") String email,

        @Size(min = 1, message = "Debe incluir al menos un teléfono") Set<String> phoneNumbers,

        @NotBlank(message = "La descripción es obligatoria") String description,

        Set<Long> representativeIds) {
}


