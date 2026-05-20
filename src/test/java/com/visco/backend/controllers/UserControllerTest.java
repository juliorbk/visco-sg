package com.visco.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.models.dtos.UpdateUserRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.services.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void getAllUsers_ReturnsPage() throws Exception {
        UserDTO user = new UserDTO();
        user.setEmail("test@example.com");
        user.setName("Test User");

        Page<UserDTO> page = new PageImpl<>(List.of(user));
        when(adminService.getAllUsers(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/users").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"));
    }

    @Test
    void getUserById_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDTO user = new UserDTO();
        user.setEmail("test@example.com");
        user.setName("Test User");

        when(adminService.getUserById(userId)).thenReturn(user);

        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void updateUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest(UserRole.ADMIN, 1L);

        UserDTO response = new UserDTO();
        response.setEmail("test@example.com");
        response.setName("Test User");

        when(adminService.updateUser(eq(userId), any(UpdateUserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void deactivateUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(adminService).deactivateUser(userId);

        mockMvc.perform(patch("/api/users/" + userId + "/deactivate"))
                .andExpect(status().isNoContent());

        verify(adminService).deactivateUser(userId);
    }

    @Test
    void activateUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(adminService).activateUser(userId);

        mockMvc.perform(patch("/api/users/" + userId + "/activate"))
                .andExpect(status().isNoContent());

        verify(adminService).activateUser(userId);
    }
}
