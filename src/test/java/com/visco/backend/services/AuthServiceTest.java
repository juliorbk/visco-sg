package com.visco.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CostCenterRepository costCenterRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    @Captor private ArgumentCaptor<User> userCaptor;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long COST_CENTER_ID = 1L;

    // ── Helpers ──────────────────────────────────────────────────────

    private UserRegisterRequest buildRegisterRequest(Long costCenterId) {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setName("Test User");
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setRole(UserRole.USER);
        req.setCostCenterId(costCenterId);
        return req;
    }

    private User buildUser() {
        return User.builder()
                .id(USER_ID)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPass")
                .role(UserRole.USER)
                .costCenter(CostCenter.builder().id(COST_CENTER_ID).fullDescription("Test Area").build())
                .active(true)
                .build();
    }

    private CostCenter buildCostCenter() {
        return CostCenter.builder()
                .id(COST_CENTER_ID)
                .code("CC-001")
                .fullDescription("Test Cost Center")
                .active(true)
                .build();
    }

    // ── register ────────────────────────────────────────────────────

    @Test
    void shouldRegisterUser_whenEmailIsNotTaken() {
        UserRegisterRequest request = buildRegisterRequest(COST_CENTER_ID);
        CostCenter costCenter = buildCostCenter();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(costCenterRepository.findById(COST_CENTER_ID)).thenReturn(Optional.of(costCenter));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPass");
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(USER_ID);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getToken()).isEqualTo("jwt-token");

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedPass");
        assertThat(userCaptor.getValue().getActive()).isTrue();
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void shouldRegisterUserWithoutCostCenter_whenNotProvided() {
        UserRegisterRequest request = buildRegisterRequest(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPass");
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(USER_ID);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUser().getCostCenterId()).isNull();
        verify(costCenterRepository, never()).findById(any());
    }

    @Test
    void shouldRegisterUser_whenCostCenterProvided() {
        UserRegisterRequest request = buildRegisterRequest(COST_CENTER_ID);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(costCenterRepository.findById(COST_CENTER_ID)).thenReturn(Optional.of(buildCostCenter()));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPass");
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(USER_ID);
            return u;
        });
        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldThrowIllegalArgumentException_whenEmailAlreadyExists() {
        UserRegisterRequest request = buildRegisterRequest(COST_CENTER_ID);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(buildUser()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email address is already in use");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void shouldThrowIllegalArgumentException_whenCostCenterNotFound() {
        UserRegisterRequest request = buildRegisterRequest(99L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(costCenterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Área no encontrada");
    }

    // ── login ───────────────────────────────────────────────────────

    @Test
    void shouldLoginSuccessfully_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = buildUser();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void shouldThrowBadCredentialsException_whenInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldThrowBadCredentialsException_whenUserNotFoundAfterAuth() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");
    }

    // ── getCurrentUser ──────────────────────────────────────────────

    @Test
    void shouldGetCurrentUser_whenEmailExists() {
        User user = buildUser();
        when(userRepository.findByEmailWithCostCenter("test@example.com"))
                .thenReturn(Optional.of(user));

        UserDTO result = authService.getCurrentUser("test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("Test User");
        assertThat(result.getCostCenterId()).isEqualTo(COST_CENTER_ID);
    }

    @Test
    void shouldThrowUsernameNotFoundException_whenEmailNotFound() {
        when(userRepository.findByEmailWithCostCenter("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldGetCurrentUser_withoutCostCenter() {
        User user = User.builder()
                .id(USER_ID)
                .name("No CC User")
                .email("nocc@example.com")
                .password("encoded")
                .role(UserRole.USER)
                .costCenter(null)
                .active(true)
                .build();

        when(userRepository.findByEmailWithCostCenter("nocc@example.com"))
                .thenReturn(Optional.of(user));

        UserDTO result = authService.getCurrentUser("nocc@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getCostCenterId()).isNull();
        assertThat(result.getCostCenterName()).isEqualTo("Without area");
    }

    // ── refreshToken ────────────────────────────────────────────────

    @Test
    void shouldRefreshToken_whenUserIsValid() {
        User user = buildUser();
        when(jwtService.generateToken(user)).thenReturn("new-jwt-token");

        String token = authService.refreshToken(user);

        assertThat(token).isEqualTo("new-jwt-token");
    }
}
