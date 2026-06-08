package com.visco.backend.repositories;

import com.visco.backend.models.entities.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// Repository for password reset tokens.
public interface PasswordResetTokenRepository
  extends JpaRepository<PasswordResetToken, UUID> {

  // Looks up a reset token by its string value.
  Optional<PasswordResetToken> findByToken(String token);

  // Removes all tokens issued to a specific user (e.g. after password change).
  void deleteByUserId(UUID userId);
}
