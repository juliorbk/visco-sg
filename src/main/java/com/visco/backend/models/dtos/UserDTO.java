package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.UserRole;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

  private UUID id;
  private String name;
  private String email;
  private UserRole role;
}
