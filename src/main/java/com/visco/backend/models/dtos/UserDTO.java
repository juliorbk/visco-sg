package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import java.util.UUID;
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

    private Long costCenterId;
    private String costCenterName;

    public static UserDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        return UserDTO.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole())
            .costCenterId(user.getCostCenter() != null ? user.getCostCenter().getId() : null)
            .costCenterName(
                user.getCostCenter() != null
                    ? user.getCostCenter().getFullDescription()
                    : "Without area"
            )
            .build();
    }
}
