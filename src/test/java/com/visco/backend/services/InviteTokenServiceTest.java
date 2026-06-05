package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateInviteRequest;
import com.visco.backend.models.dtos.InviteTokenResponse;
import com.visco.backend.models.entities.InviteToken;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.InviteTokenRepository;
import com.visco.backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InviteTokenServiceTest {

  @Autowired private InviteTokenService inviteTokenService;
  @Autowired private InviteTokenRepository inviteTokenRepository;
  @Autowired private UserRepository userRepository;

  private User creator;

  @BeforeEach
  void setUp() {
    inviteTokenRepository.deleteAll();
    userRepository.deleteAll();
    creator = userRepository.save(
      User.builder()
        .name("Alice Creator")
        .email("alice@example.com")
        .password("x")
        .role(UserRole.ADMIN)
        .active(true)
        .build()
    );
  }

  @Test
  void createInvite_mintsUniqueTokenWithDefaultExpiry() {
    CreateInviteRequest req = new CreateInviteRequest();
    req.setEmail("newuser@example.com");
    req.setRole("USER");

    InviteTokenResponse response = inviteTokenService.createInvite(req, creator.getId());

    assertNotNull(response.getId());
    assertNotNull(response.getToken());
    assertFalse(response.getToken().isBlank());
    assertEquals("newuser@example.com", response.getEmail());
    assertEquals("USER", response.getIntendedRole());
    assertEquals(creator.getId(), response.getCreatedById());
    assertTrue(response.getExpiresAt().isAfter(LocalDateTime.now()));
  }

  @Test
  void createInvite_lowercasesAndTrimsEmail() {
    CreateInviteRequest req = new CreateInviteRequest();
    req.setEmail("  Mixed@Example.COM  ");
    req.setRole("USER");

    InviteTokenResponse response = inviteTokenService.createInvite(req, creator.getId());

    assertEquals("mixed@example.com", response.getEmail());
  }

  @Test
  void createInvite_rejectsPastExpiry() {
    CreateInviteRequest req = new CreateInviteRequest();
    req.setEmail("u@example.com");
    req.setRole("USER");
    req.setExpiresAt(LocalDateTime.now().minusHours(1));

    assertThrows(IllegalArgumentException.class,
      () -> inviteTokenService.createInvite(req, creator.getId()));
  }

  @Test
  void findByToken_returnsPersistedInvite() {
    InviteTokenResponse created = inviteTokenService.createInvite(
      newInvite("foo@example.com", "USER"),
      creator.getId()
    );

    InviteToken fetched = inviteTokenService.findByToken(created.getToken());

    assertEquals(created.getId(), fetched.getId());
    assertEquals("foo@example.com", fetched.getEmail());
  }

  @Test
  void findByToken_throwsForUnknownToken() {
    assertThrows(EntityNotFoundException.class,
      () -> inviteTokenService.findByToken("does-not-exist"));
  }

  @Test
  void consumeInvite_marksTokenUsedAndRecordsUser() {
    InviteTokenResponse created = inviteTokenService.createInvite(
      newInvite("consume@example.com", "USER"),
      creator.getId()
    );

    User newUser = userRepository.save(
      User.builder()
        .name("New User")
        .email("consume@example.com")
        .password("x")
        .role(UserRole.USER)
        .active(true)
        .build()
    );

    InviteToken consumed = inviteTokenService.consumeInvite(created.getToken(), newUser);

    assertNotNull(consumed.getUsedAt());
    assertEquals(newUser.getId(), consumed.getUsedByUserId());
    assertFalse(consumed.isUsable());
  }

  @Test
  void consumeInvite_rejectsEmailMismatch() {
    InviteTokenResponse created = inviteTokenService.createInvite(
      newInvite("intended@example.com", "USER"),
      creator.getId()
    );

    User otherUser = userRepository.save(
      User.builder()
        .name("Other")
        .email("other@example.com")
        .password("x")
        .role(UserRole.USER)
        .active(true)
        .build()
    );

    assertThrows(IllegalArgumentException.class,
      () -> inviteTokenService.consumeInvite(created.getToken(), otherUser));
  }

  @Test
  void consumeInvite_rejectsAlreadyUsedToken() {
    InviteTokenResponse created = inviteTokenService.createInvite(
      newInvite("reuse@example.com", "USER"),
      creator.getId()
    );

    User first = userRepository.save(
      User.builder().name("First").email("reuse@example.com")
        .password("x").role(UserRole.USER).active(true).build()
    );
    inviteTokenService.consumeInvite(created.getToken(), first);

    // Build a second user with the SAME email but a fresh row is not
    // possible (User.email is unique). So we manually flip the
    // consumeInvite checks: clear the email-uniqueness guard by
    // simulating what the service does (which is to look up the
    // email on the token and compare to the new user's email).
    // Easier: re-run consumeInvite with the same user (token is now
    // used; the service should refuse regardless of the user).
    assertThrows(IllegalStateException.class,
      () -> inviteTokenService.consumeInvite(created.getToken(), first));
  }

  @Test
  void consumeInvite_rejectsExpiredToken() {
    InviteToken invite = InviteToken.builder()
      .token("expired-token")
      .email("exp@example.com")
      .intendedRole("USER")
      .createdById(creator.getId())
      .expiresAt(LocalDateTime.now().minusMinutes(1))
      .revoked(false)
      .build();
    inviteTokenRepository.save(invite);

    User user = userRepository.save(
      User.builder().name("x").email("exp@example.com")
        .password("x").role(UserRole.USER).active(true).build()
    );

    assertThrows(IllegalStateException.class,
      () -> inviteTokenService.consumeInvite("expired-token", user));
  }

  @Test
  void consumeInvite_rejectsRevokedToken() {
    InviteToken invite = InviteToken.builder()
      .token("revoked-token")
      .email("rev@example.com")
      .intendedRole("USER")
      .createdById(creator.getId())
      .expiresAt(LocalDateTime.now().plusHours(1))
      .revoked(true)
      .build();
    inviteTokenRepository.save(invite);

    User user = userRepository.save(
      User.builder().name("x").email("rev@example.com")
        .password("x").role(UserRole.USER).active(true).build()
    );

    assertThrows(IllegalStateException.class,
      () -> inviteTokenService.consumeInvite("revoked-token", user));
  }

  @Test
  void revokeInvite_marksTokenRevoked() {
    InviteTokenResponse created = inviteTokenService.createInvite(
      newInvite("rev@example.com", "USER"),
      creator.getId()
    );

    InviteTokenResponse revoked = inviteTokenService.revokeInvite(created.getId());

    assertTrue(revoked.isRevoked());
    // Calling revoke a second time is a no-op (idempotent) — does not throw.
    InviteTokenResponse again = inviteTokenService.revokeInvite(created.getId());
    assertTrue(again.isRevoked());
  }

  @Test
  void revokeInvite_throwsForUnknownId() {
    assertThrows(EntityNotFoundException.class,
      () -> inviteTokenService.revokeInvite(UUID.randomUUID()));
  }

  @Test
  void revokeInvite_refusesUsedToken() {
    InviteTokenResponse created = inviteTokenService.createInvite(
      newInvite("used@example.com", "USER"),
      creator.getId()
    );
    User u = userRepository.save(
      User.builder().name("x").email("used@example.com")
        .password("x").role(UserRole.USER).active(true).build()
    );
    inviteTokenService.consumeInvite(created.getToken(), u);

    assertThrows(IllegalStateException.class,
      () -> inviteTokenService.revokeInvite(created.getId()));
  }

  @Test
  void listInvites_returnsAll() {
    inviteTokenService.createInvite(newInvite("a@x", "USER"), creator.getId());
    inviteTokenService.createInvite(newInvite("b@x", "ADMIN"), creator.getId());
    inviteTokenService.createInvite(newInvite("c@x", "MANAGER"), creator.getId());

    List<InviteTokenResponse> all = inviteTokenService.listInvites();

    assertEquals(3, all.size());
  }

  private CreateInviteRequest newInvite(String email, String role) {
    CreateInviteRequest req = new CreateInviteRequest();
    req.setEmail(email);
    req.setRole(role);
    return req;
  }
}
