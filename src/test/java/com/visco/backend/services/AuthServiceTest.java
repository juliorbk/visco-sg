package com.visco.backend.services;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.CreateInviteRequest;
import com.visco.backend.models.dtos.InviteTokenResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.models.entities.InviteToken;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.InviteTokenRepository;
import com.visco.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

  @Autowired private AuthService authService;
  @Autowired private InviteTokenService inviteTokenService;
  @Autowired private UserRepository userRepository;
  @Autowired private InviteTokenRepository inviteTokenRepository;
  @Autowired private CostCenterRepository costCenterRepository;
  @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
  @Autowired private com.visco.backend.config.CustomUserDetailsService userDetailsService;
  @Autowired private jakarta.persistence.EntityManager entityManager;

  @BeforeEach
  void setUp() {
    inviteTokenRepository.deleteAll();
    userRepository.deleteAll();
    costCenterRepository.deleteAll();
  }

  @Test
  void register_succeedsWithValidInvite() {
    InviteTokenResponse invite = mintInvite("new@example.com", "USER");
    UserRegisterRequest req = registerRequest(
      "New",
      "new@example.com",
      "password123",
      invite.getToken()
    );

    AuthResponse response = authService.register(req);

    assertNotNull(response.getUser());
    assertEquals("new@example.com", response.getUser().getEmail());
    assertEquals(UserRole.USER, response.getUser().getRole());
    assertNotNull(response.getToken());
    assertTrue(userRepository.findByEmail("new@example.com").isPresent());
    // The token is consumed.
    InviteToken persisted = inviteTokenRepository
      .findByToken(invite.getToken()).orElseThrow();
    assertNotNull(persisted.getUsedAt());
  }

  @Test
  void register_rejectsDuplicateEmail() {
    User existing = userRepository.save(
      User.builder()
        .name("Existing")
        .email("dup@example.com")
        .password(passwordEncoder.encode("password123"))
        .role(UserRole.USER)
        .active(true)
        .build()
    );
    InviteTokenResponse invite = mintInvite("dup@example.com", "USER");
    UserRegisterRequest req = registerRequest(
      "Dup", "dup@example.com", "password123", invite.getToken()
    );

    assertThrows(IllegalArgumentException.class, () -> authService.register(req));
  }

  @Test
  void register_rejectsInvalidInviteToken() {
    UserRegisterRequest req = registerRequest(
      "New", "x@example.com", "password123", "no-such-token"
    );

    assertThrows(RuntimeException.class, () -> authService.register(req));
  }

  @Test
  void register_emailIsEncodedAsPassword() {
    InviteTokenResponse invite = mintInvite("encode@example.com", "USER");
    UserRegisterRequest req = registerRequest(
      "Encoded", "encode@example.com", "plain-password", invite.getToken()
    );

    authService.register(req);

    User persisted = userRepository.findByEmail("encode@example.com").orElseThrow();
    assertNotEquals("plain-password", persisted.getPassword());
    assertTrue(passwordEncoder.matches("plain-password", persisted.getPassword()));
  }

  @Test
  void login_returnsTokenForValidCredentials() {
    InviteTokenResponse invite = mintInvite("login@example.com", "USER");
    UserRegisterRequest register = registerRequest(
      "Login User", "login@example.com", "password123", invite.getToken()
    );
    authService.register(register);

    LoginRequest login = new LoginRequest("login@example.com", "password123");
    AuthResponse response = authService.login(login);

    assertNotNull(response.getToken());
    assertEquals("login@example.com", response.getUser().getEmail());
  }

  @Test
  void login_rejectsInvalidPassword() {
    InviteTokenResponse invite = mintInvite("badpass@example.com", "USER");
    authService.register(registerRequest(
      "User", "badpass@example.com", "password123", invite.getToken()
    ));

    LoginRequest login = new LoginRequest("badpass@example.com", "wrong");
    assertThrows(BadCredentialsException.class, () -> authService.login(login));
  }

  @Test
  void login_rejectsUnknownEmail() {
    LoginRequest login = new LoginRequest("nobody@example.com", "password123");
    assertThrows(BadCredentialsException.class, () -> authService.login(login));
  }

  @Test
  void login_rejectsDeactivatedUser() {
    InviteTokenResponse invite = mintInvite("deact@example.com", "USER");
    authService.register(registerRequest(
      "Deact", "deact@example.com", "password123", invite.getToken()
    ));
    User user = userRepository.findByEmail("deact@example.com").orElseThrow();
    user.setActive(false);
    userRepository.saveAndFlush(user);
    // Detach the entity from the L1 cache and clear the persistence
    // context so the next read goes back to the database. Without
    // this, the test transaction and the AuthenticationProvider's
    // own read can each see their own copy of the row.
    entityManager.flush();
    entityManager.clear();
    User reloaded = userRepository.findByEmail("deact@example.com").orElseThrow();
    assertEquals(Boolean.FALSE, reloaded.getActive(),
      "precondition: user must be deactivated in the DB before login");

    var loaded = userDetailsService.loadUserByUsername("deact@example.com");
    assertFalse(loaded.isEnabled(),
      "precondition: UserDetailsService should report the user as disabled");

    LoginRequest login = new LoginRequest("deact@example.com", "password123");
    // The DaoAuthenticationProvider surfaces a deactivated user via
    // DisabledException (mapped to 401 by the global handler). What
    // matters is that login does NOT succeed.
    assertThrows(RuntimeException.class, () -> authService.login(login));
  }

  @Test
  void getCurrentUser_returnsUserForKnownEmail() {
    InviteTokenResponse invite = mintInvite("me@example.com", "MANAGER");
    authService.register(registerRequest(
      "Me", "me@example.com", "password123", invite.getToken()
    ));

    var dto = authService.getCurrentUser("me@example.com");

    assertNotNull(dto);
    assertEquals("me@example.com", dto.getEmail());
    assertEquals(UserRole.MANAGER, dto.getRole());
  }

  @Test
  void getCurrentUser_throwsForUnknownEmail() {
    assertThrows(RuntimeException.class,
      () -> authService.getCurrentUser("nobody@example.com"));
  }

  private InviteTokenResponse mintInvite(String email, String role) {
    CreateInviteRequest req = new CreateInviteRequest();
    req.setEmail(email);
    req.setRole(role);
    User creator = userRepository.save(
      User.builder()
        .name("Invite Creator")
        .email("creator-" + System.nanoTime() + "@example.com")
        .password("x")
        .role(UserRole.ADMIN)
        .active(true)
        .build()
    );
    return inviteTokenService.createInvite(req, creator.getId());
  }

  private UserRegisterRequest registerRequest(
    String name, String email, String password, String inviteToken
  ) {
    UserRegisterRequest req = new UserRegisterRequest();
    req.setName(name);
    req.setEmail(email);
    req.setPassword(password);
    req.setInviteToken(inviteToken);
    return req;
  }
}
