package com.visco.backend.models.dtos;

import java.util.UUID;

import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

  private UUID id;
  private String name;
  private String email;
  private UserRole role;

  private Long areaId;
  private String areaName;

  public static UserDTO fromUser(User user) {
    if (user == null) {
      return null;
    }
    return UserDTO.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .role(user.getRole())
        .areaId(user.getArea() != null ? user.getArea().getId() : null)
        .areaName(user.getArea() != null ? user.getArea().getName() : "Without area")
        .build();
  }
}
