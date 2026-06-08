package com.visco.backend.config; // Ajusta si lo pones en otro lado

import com.visco.backend.models.entities.UserRole;
import java.util.Collection;
import java.util.UUID; // O Long, dependiendo del tipo de ID de tu User
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Represents the authenticated user principal in Spring Security. Wraps a
 * {@link User} entity and provides access to the user's ID, email, roles,
 * and account status. Used throughout the application as the principal
 * returned by {@link CustomUserDetailsService}.
 */
public class UserPrincipal implements UserDetails {

  private final UUID id;
  private final String email;
  private final String password;
  private final boolean active;
  private final Collection<? extends GrantedAuthority> authorities;

  /**
   * Constructs a new {@link UserPrincipal} with the given ID, email,
   * password, account status, and granted authorities.
   *
   * @param id          the user's unique identifier
   * @param email       the user's email address (used as username)
   * @param password    the user's hashed password
   * @param active      whether the account is enabled
   * @param authorities the granted authorities / roles
   */
  public UserPrincipal(
    UUID id,
    String email,
    String password,
    boolean active,
    Collection<? extends GrantedAuthority> authorities
  ) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.active = active;
    this.authorities = authorities;
  }

  /**
   * Resolve the {@link UserRole} from the principal's authorities. Falls back
   * to {@code USER} when the principal is null (anonymous request) so callers
   * can use the result safely in role-comparison checks.
   */
  public static UserRole resolveRole(UserPrincipal principal) {
    if (principal == null || principal.getAuthorities() == null) {
      return UserRole.USER;
    }
    for (GrantedAuthority a : principal.getAuthorities()) {
      String name = a.getAuthority();
      if (name == null) continue;
      if (name.startsWith("ROLE_")) {
        name = name.substring("ROLE_".length());
      }
      try {
        return UserRole.valueOf(name);
      } catch (IllegalArgumentException ignored) {}
    }
    return UserRole.USER;
  }

  /**
   * Returns the unique identifier of the authenticated user.
   *
   * @return the user's UUID
   */
  public UUID getId() {
    return id;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
