package com.visco.backend.config;

import com.visco.backend.config.UserPrincipal; // Ajusta el paquete según dónde hayas creado esta clase
import com.visco.backend.models.entities.User;
import com.visco.backend.repositories.UserRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  // Inyectamos tu repositorio existente
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email)
    throws UsernameNotFoundException {
    // 1. Buscar el usuario en tu base de datos usando tu repositorio
    User user = userRepository
      .findByEmail(email) // Asegúrate de tener este método en tu UserRepository
      .orElseThrow(() ->
        new UsernameNotFoundException(
          "Usuario no encontrado con el email: " + email
        )
      );

    // 2. Mapear el rol de tu entidad a las "Authorities" de Spring Security.
    // Basado en tu estructura, asumo que tienes un UserRole. Spring Security
    // requiere por convención que los roles tengan el prefijo "ROLE_".
    String roleName = "ROLE_" + user.getRole().name();
    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
      new SimpleGrantedAuthority(roleName)
    );

    // 3. Construir y retornar el UserPrincipal con los datos de tu entidad
    return new UserPrincipal(
      user.getId(),
      user.getEmail(),
      user.getPassword(),
      authorities
    );
  }
}
