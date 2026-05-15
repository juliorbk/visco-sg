package com.visco.backend.controllers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.services.AuthService;
import com.visco.backend.services.CookieService;
import com.visco.backend.services.JwtService;

import jakarta.servlet.http.Cookie;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CookieService cookieService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void register_shouldReturn200() throws Exception {
        UserDTO userDTO = UserDTO.builder()
                .id(UUID.randomUUID()).name("Test User")
                .email("test@example.com").role(UserRole.MANAGER).build();

        when(authService.register(any())).thenReturn(AuthResponse.builder()
                .token("jwt-token").user(userDTO).build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test User\",\"email\":\"test@example.com\",\"password\":\"password123\",\"role\":\"MANAGER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.name").value("Test User"));
    }

    @Test
    void register_shouldReturn400_whenEmailAlreadyExists() throws Exception {
        when(authService.register(any()))
                .thenThrow(new IllegalArgumentException("Email address is already in use"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"test@example.com\",\"password\":\"password123\",\"role\":\"MANAGER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200() throws Exception {
        UserDTO userDTO = UserDTO.builder()
                .id(UUID.randomUUID()).name("Test User")
                .email("test@example.com").role(UserRole.MANAGER).build();

        when(authService.login(any(LoginRequest.class))).thenReturn(
                AuthResponse.builder().token("jwt-token").user(userDTO).build());
        when(cookieService.createJwtCookie("jwt-token"))
                .thenReturn(new Cookie("visco_auth_token", "jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void login_shouldReturn401_whenBadCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_shouldReturn200() throws Exception {
        when(cookieService.createLogoutCookie()).thenReturn(new Cookie("visco_auth_token", null));

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logout successful"));
    }

}
