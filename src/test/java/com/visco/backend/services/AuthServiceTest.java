package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.models.entities.RequestingArea;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.AreaRepository;
import com.visco.backend.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CookieService cookieService;

    @InjectMocks
    private AuthService authService;

    private UserRegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        registerRequest = new UserRegisterRequest();
        registerRequest.setName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(UserRole.MANAGER);
        registerRequest.setAreaId(1L);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(UserRole.MANAGER)
                .active(true)
                .build();
    }

    @Test
    void register_shouldSucceed_whenEmailIsNotTaken() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(areaRepository.findById(1L)).thenReturn(Optional.of(new RequestingArea()));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("test-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("Test User", response.getUser().getName());
        verify(emailService).sendWelcomeEmail("test@example.com", "Test User");
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldThrow_whenAreaNotFound() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(areaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldSucceed_whenAreaIdIsNull() {
        registerRequest.setAreaId(null);
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("test-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        verify(areaRepository, never()).findById(any());
    }

    @Test
    void login_shouldSucceed_withValidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("test-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        assertEquals("Test User", response.getUser().getName());
    }

    @Test
    void login_shouldThrow_whenBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void refreshToken_shouldReturnNewToken() {
        when(jwtService.generateToken(user)).thenReturn("refreshed-token");

        String token = authService.refreshToken(user);

        assertEquals("refreshed-token", token);
    }

    @Test
    void getCurrentUser_shouldReturnUserDTO() {
        RequestingArea area = new RequestingArea();
        area.setId(1L);
        area.setName("Warehouse Area");
        user.setArea(area);

        when(userRepository.findByEmailWithArea("test@example.com")).thenReturn(Optional.of(user));

        UserDTO dto = authService.getCurrentUser("test@example.com");

        assertNotNull(dto);
        assertEquals(userId, dto.getId());
        assertEquals("Test User", dto.getName());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals(UserRole.MANAGER, dto.getRole());
        assertEquals(1L, dto.getAreaId());
        assertEquals("Warehouse Area", dto.getAreaName());
    }

    @Test
    void getCurrentUser_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmailWithArea("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.getCurrentUser("unknown@example.com"));
    }
}
