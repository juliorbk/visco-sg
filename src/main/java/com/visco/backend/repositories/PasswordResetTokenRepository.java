package com.visco.backend.repositories;

import com.visco.backend.models.entities.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository
  extends JpaRepository<PasswordResetToken, UUID> {

  Optional<PasswordResetToken> findByToken(String token);

  void deleteByUserId(UUID userId);
}
