package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateInviteRequest;
import com.visco.backend.models.dtos.InviteTokenResponse;
import com.visco.backend.models.entities.InviteToken;
import com.visco.backend.models.entities.User;
import com.visco.backend.repositories.InviteTokenRepository;
import jakarta.persistence.EntityNotFoundException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for user invitation token management.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InviteTokenService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;

  private final InviteTokenRepository inviteTokenRepository;
  private final EmailService emailService;

  @Value("${app.invite.default-validity-hours:72}")
  private int defaultValidityHours;

  /**
   * Creates an invite token and sends the invitation email.
   *
   * @param request     the invite creation request
   * @param createdById the ID of the user creating the invite
   * @return the created invite token response
   */
  @Transactional
  public InviteTokenResponse createInvite(
    CreateInviteRequest request,
    UUID createdById
  ) {
    LocalDateTime expiresAt = request.getExpiresAt() != null
      ? request.getExpiresAt()
      : LocalDateTime.now().plusHours(defaultValidityHours);

    if (!expiresAt.isAfter(LocalDateTime.now())) {
      throw new IllegalArgumentException("La fecha de expiración debe ser futura");
    }

    InviteToken invite = InviteToken.builder()
      .token(generateToken())
      .email(request.getEmail().toLowerCase().trim())
      .intendedRole(request.getRole())
      .costCenterId(request.getCostCenterId())
      .createdById(createdById)
      .expiresAt(expiresAt)
      .revoked(false)
      .build();

    InviteToken saved = inviteTokenRepository.save(invite);

    emailService.sendInviteEmail(
      saved.getEmail(),
      null,
      saved.getToken(),
      saved.getIntendedRole()
    );

    return InviteTokenResponse.fromEntity(saved);
  }

  /**
   * Consumes an invite token during user registration.
   *
   * @param tokenValue the token string
   * @param newUser    the newly registered user
   * @return the consumed invite token
   */
  @Transactional
  public InviteToken consumeInvite(String tokenValue, User newUser) {
    InviteToken invite = inviteTokenRepository
      .findByToken(tokenValue)
      .orElseThrow(() -> new EntityNotFoundException("Invalid invite token"));

    if (!invite.isUsable()) {
      throw new IllegalStateException("Invite token is no longer valid");
    }

    if (!invite.getEmail().equalsIgnoreCase(newUser.getEmail())) {
      throw new IllegalArgumentException(
        "Invite token email does not match the registration email"
      );
    }

    invite.setUsedAt(LocalDateTime.now());
    invite.setUsedByUserId(newUser.getId());
    return inviteTokenRepository.save(invite);
  }

  /**
   * Lists all invite tokens.
   *
   * @return list of invite token responses
   */
  public List<InviteTokenResponse> listInvites() {
    return inviteTokenRepository
      .findAll()
      .stream()
      .map(InviteTokenResponse::fromEntity)
      .toList();
  }

  /**
   * Revokes an unused invite token by its ID.
   *
   * @param id the invite token ID
   * @return the updated invite token response
   */
  @Transactional
  public InviteTokenResponse revokeInvite(UUID id) {
    InviteToken invite = inviteTokenRepository
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Invite not found"));

    if (invite.isRevoked()) {
      return InviteTokenResponse.fromEntity(invite);
    }
    if (invite.getUsedAt() != null) {
      throw new IllegalStateException("Cannot revoke a used invite token");
    }

    invite.setRevoked(true);
    return InviteTokenResponse.fromEntity(inviteTokenRepository.save(invite));
  }

  /**
   * Finds an invite token by its string value.
   *
   * @param tokenValue the token string
   * @return the invite token entity
   */
  public InviteToken findByToken(String tokenValue) {
    return inviteTokenRepository
      .findByToken(tokenValue)
      .orElseThrow(() -> new EntityNotFoundException("Invalid invite token"));
  }

  /**
   * Resolves an invite token for the public registration flow.
   * Returns the invite details without requiring authentication.
   *
   * @param tokenValue the token string
   * @return the invite token response
   */
  public InviteTokenResponse resolveByToken(String tokenValue) {
    return InviteTokenResponse.fromEntity(findByToken(tokenValue));
  }

  private String generateToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
