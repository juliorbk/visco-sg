package com.visco.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
// Response DTO containing authentication token and user details.
public class AuthResponse {
  private UserDTO user;
  
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String token;
}
