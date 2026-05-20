package com.visco.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.services.AuthService;
import com.visco.backend.services.CookieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CookieService cookieService;

    @MockitoBean
    private org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration webSecurityConfiguration;

    @Test
    void registerUser_Success() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.USER);

        AuthResponse response = new AuthResponse();
        response.setToken("jwt-token");
        response.setUser(new UserDTO());

        when(authService.register(any(UserRegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(authService).register(any(UserRegisterRequest.class));
    }

    @Test
    void loginUser_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponse response = new AuthResponse();
        response.setToken("jwt-token");
        response.setUser(new UserDTO());

        Cookie jwtCookie = new Cookie("visco_auth_token", "jwt-token");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);
        when(cookieService.createJwtCookie(anyString())).thenReturn(jwtCookie);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("visco_auth_token"));

        verify(authService).login(any(LoginRequest.class));
        verify(cookieService).createJwtCookie(anyString());
    }

    @Test
    void logoutUser_Success() throws Exception {
        Cookie logoutCookie = new Cookie("visco_auth_token", null);
        logoutCookie.setMaxAge(0);

        when(cookieService.createLogoutCookie()).thenReturn(logoutCookie);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logout successful"));

        verify(cookieService).createLogoutCookie();
    }

    @Test
    void getCurrentUser_Success() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("test@example.com");
        userDTO.setName("Test User");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@example.com");
        when(authService.getCurrentUser("test@example.com")).thenReturn(userDTO);

        mockMvc.perform(get("/api/auth/me")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(authService).getCurrentUser("test@example.com");
    }
}
