package com.visco.backend.services;

import com.visco.backend.models.dtos.UpdateUserRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CostCenterRepository costCenterRepository;

    @InjectMocks
    private AdminService adminService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID()).name("Test User").email("test@example.com")
                .role(UserRole.USER).active(true).build();
    }

    @Test
    void getAllUsers_ReturnsPage() {
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<UserDTO> result = adminService.getAllUsers(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        UserDTO result = adminService.getUserById(testUser.getId());

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserById_FailsWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(unknownId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateUser_Success() {
        UpdateUserRequest request = new UpdateUserRequest(UserRole.ADMIN, 1L);
        CostCenter costCenter = CostCenter.builder().id(1L).fullDescription("Test CC").build();

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(costCenter));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = adminService.updateUser(testUser.getId(), request);

        assertThat(result).isNotNull();
        assertThat(testUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(testUser.getCostCenter()).isEqualTo(costCenter);
    }

    @Test
    void updateUser_WithNullCostCenter() {
        UpdateUserRequest request = new UpdateUserRequest(UserRole.ADMIN, null);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = adminService.updateUser(testUser.getId(), request);

        assertThat(result).isNotNull();
        assertThat(testUser.getCostCenter()).isNull();
    }

    @Test
    void updateUser_FailsWhenUserNotFound() {
        UpdateUserRequest request = new UpdateUserRequest(UserRole.ADMIN, 1L);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUser(testUser.getId(), request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateUser_FailsWhenCostCenterNotFound() {
        UpdateUserRequest request = new UpdateUserRequest(UserRole.ADMIN, 999L);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(costCenterRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUser(testUser.getId(), request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deactivateUser_Success() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.deactivateUser(testUser.getId());

        assertThat(testUser.getActive()).isFalse();
    }

    @Test
    void deactivateUser_FailsWhenNotFound() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deactivateUser(testUser.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void activateUser_Success() {
        testUser.setActive(false);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.activateUser(testUser.getId());

        assertThat(testUser.getActive()).isTrue();
    }
}
