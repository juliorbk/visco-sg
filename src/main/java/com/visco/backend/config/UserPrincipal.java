package com.visco.backend.config; // Ajusta si lo pones en otro lado

import java.util.Collection;
import java.util.UUID; // O Long, dependiendo del tipo de ID de tu User
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

  private final UUID id;
  private final String email;
  private final String password;
  private final boolean active;
  private final Collection<? extends GrantedAuthority> authorities;

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
