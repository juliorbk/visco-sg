package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.RequestingArea;
import com.visco.backend.models.entities.User;
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
  private RequestingArea area;

  public static UserDTO fromUser(User user) {
    if (user == null) {
      return null;
    }
    return UserDTO.builder()
      .id(user.getId())
      .name(user.getName())
      .email(user.getEmail())
      .role(user.getRole())
      .area(user.getArea())
      .build();
  }
}
