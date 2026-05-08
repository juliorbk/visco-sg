package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.RequestingArea;
import com.visco.backend.models.entities.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterRequest {

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Email is required")
  private String email;

  @NotBlank(message = "Password is required")
  private String password;

  private UserRole role;
  private RequestingArea area;
}
