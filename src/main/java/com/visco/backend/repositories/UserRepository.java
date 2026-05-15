package com.visco.backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmail(String email);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.area WHERE u.email = :email")
  Optional<User> findByEmailWithArea(@Param("email") String email);

  @Query("SELECT u.role as role, COUNT(u) as count FROM User u GROUP BY u.role")
  List<UserRoleCountProjection> countByRole();

  interface UserRoleCountProjection {
    UserRole getRole();

    Long getCount();
  }
}
