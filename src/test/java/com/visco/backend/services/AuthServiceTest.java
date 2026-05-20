package com.visco.backend.services;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CostCenterRepository costCenterRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserRegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(UserRole.USER)
                .active(true)
                .build();

        registerRequest = new UserRegisterRequest();
        registerRequest.setName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(UserRole.USER);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse result = authService.register(registerRequest);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");
        verify(emailService).sendWelcomeEmail(eq("test@example.com"), eq("Test User"));
    }

    @Test
    void register_FailsWhenEmailAlreadyExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_WithValidCostCenter() {
        CostCenter costCenter = new CostCenter();
        costCenter.setId(1L);

        registerRequest.setCostCenterId(1L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(costCenter));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse result = authService.register(registerRequest);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("jwt-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_FailsWhenCostCenterNotFound() {
        registerRequest.setCostCenterId(999L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(costCenterRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void login_Success() {
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse result = authService.login(loginRequest);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUser()).isNotNull();
    }

    @Test
    void login_FailsWithBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void getCurrentUser_Success() {
        when(userRepository.findByEmailWithCostCenter(anyString())).thenReturn(Optional.of(testUser));

        UserDTO result = authService.getCurrentUser("test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getCurrentUser_FailsWhenUserNotFound() {
        when(userRepository.findByEmailWithCostCenter(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("unknown@example.com"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void refreshToken_Success() {
        when(jwtService.generateToken(any(User.class))).thenReturn("new-jwt-token");

        String result = authService.refreshToken(testUser);

        assertThat(result).isEqualTo("new-jwt-token");
    }
}
