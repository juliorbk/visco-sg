package com.visco.backend.config;

import com.visco.backend.config.UserPrincipal; // Ajusta el paquete según dónde hayas creado esta clase
import com.visco.backend.models.entities.User;
import com.visco.backend.repositories.UserRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementation of Spring Security's {@link UserDetailsService} that loads
 * user details from the database via {@link UserRepository}. Maps the
 * {@link User} entity to a {@link UserPrincipal} with the appropriate
 * Spring Security authorities (roles prefixed as {@code ROLE_}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  // Inyectamos tu repositorio existente
  private final UserRepository userRepository;

  /**
   * Loads a user by their email address. Looks up the user in the database
   * and returns a {@link UserPrincipal} with the role prefixed as
   * {@code ROLE_} for Spring Security authorization.
   *
   * @param email the email address identifying the user
   * @return the fully populated {@link UserDetails} instance
   * @throws UsernameNotFoundException if the user is not found
   */
  @Override
  public UserDetails loadUserByUsername(String email)
    throws UsernameNotFoundException {
    // 1. Buscar el usuario en tu base de datos usando tu repositorio
    User user = userRepository
      .findByEmailIgnoreCase(email) // Asegúrate de tener este método en tu UserRepository
      .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    log.info("loadUserByUsername: email={} active={}", user.getEmail(), user.getActive());

    // 2. Mapear el rol de tu entidad a las "Authorities" de Spring Security.
    // Basado en tu estructura, asumo que tienes un UserRole. Spring Security
    // requiere por convención que los roles tengan el prefijo "ROLE_".
    String roleName = "ROLE_" + user.getRole().name();
    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
      new SimpleGrantedAuthority(roleName)
    );

    // 3. Construir y retornar el UserPrincipal con los datos de tu entidad.
    // Se pasa un password vacío porque el principal se usa solo en el JWT
    // filter, nunca para autenticar — Spring solo necesita el password en
    // DaoAuthenticationProvider. Mantenerlo en memoria crea una vía de leak
    // si alguna vez el principal se serializa.
    return new UserPrincipal(
      user.getId(),
      user.getEmail(),
      user.getPassword(),
      Boolean.TRUE.equals(user.getActive()),
      authorities
    );
  }
}
