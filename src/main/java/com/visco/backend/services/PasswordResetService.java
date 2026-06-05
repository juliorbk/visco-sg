package com.visco.backend.services;

import com.visco.backend.models.entities.PasswordResetToken;
import com.visco.backend.models.entities.User;
import com.visco.backend.repositories.PasswordResetTokenRepository;
import com.visco.backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the password-reset flow:
 *
 * <ol>
 *   <li>{@link #requestReset(String)} generates a single-use token, persists
 *       it and asks {@link EmailService} to email the user. The method never
 *       reveals whether the email exists in the system.</li>
 *   <li>{@link #consumeToken(String, String)} validates the token, updates the
 *       user's password and invalidates the token atomically.</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;

  private final PasswordResetTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  @Value("${app.password-reset.token-validity-hours:2}")
  private int validityHours;

  /**
   * Generates a reset token for the given email and dispatches the email
   * asynchronously. Returns {@code true} if the email matched a real user,
   * but callers should NOT expose that to the HTTP client (response is the
   * same 200 in both cases to prevent user enumeration).
   */
  @Transactional
  public boolean requestReset(String email) {
    String normalized = email.toLowerCase().trim();
    return userRepository
      .findByEmail(normalized)
      .filter(user -> Boolean.TRUE.equals(user.getActive()))
      .map(user -> {
        // Invalidate any previous outstanding tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(validityHours);

        tokenRepository.save(
          PasswordResetToken.builder()
            .token(token)
            .userId(user.getId())
            .expiresAt(expiresAt)
            .build()
        );

        emailService.sendPasswordResetEmail(
          user.getEmail(),
          user.getName(),
          token
        );

        log.info(
          "🔐 Password reset requested for user {} (token expires {})",
          user.getId(),
          expiresAt
        );
        return true;
      })
      .orElse(false);
  }

  /**
   * Validates a token and changes the password. Marks the token as used.
   * Throws on invalid / expired / already-used tokens.
   */
  @Transactional
  public void consumeToken(String tokenValue, String newPassword) {
    PasswordResetToken token = tokenRepository
      .findByToken(tokenValue)
      .orElseThrow(() ->
        new EntityNotFoundException("Invalid or expired reset token")
      );

    if (!token.isUsable()) {
      throw new IllegalStateException("Reset token is no longer valid");
    }

    User user = userRepository
      .findById(token.getUserId())
      .orElseThrow(() ->
        new EntityNotFoundException("User no longer exists")
      );

    if (!user.isEnabled()) {
      throw new IllegalStateException("User account is disabled");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    token.setUsedAt(LocalDateTime.now());
    tokenRepository.save(token);

    emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());

    log.info("🔐 Password reset completed for user {}", user.getId());
  }

  private String generateToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
