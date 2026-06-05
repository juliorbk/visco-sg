package com.visco.backend.repositories;

import com.visco.backend.models.entities.InviteToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {
  Optional<InviteToken> findByToken(String token);
}
