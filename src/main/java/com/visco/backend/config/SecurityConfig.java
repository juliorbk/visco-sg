package com.visco.backend.config;

import com.visco.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final UserRepository userRepository;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
    throws Exception {
    return http
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(s ->
        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(auth ->
        auth
          // Público
          .requestMatchers("/api/auth/**")
          .permitAll()
          .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
          .permitAll()
          // Solo ADMIN
          .requestMatchers("/api/users/**")
          .hasAuthority("ADMIN")
          .requestMatchers("/api/suppliers/**")
          .hasAnyAuthority("ADMIN", "MANAGER")
          // ADMIN y MANAGER
          .requestMatchers("/api/procurement/**")
          .hasAnyAuthority("ADMIN", "MANAGER")
          .requestMatchers("/api/warehouses/**")
          .hasAnyAuthority("ADMIN", "MANAGER")
          // Todos los autenticados
          .requestMatchers("/api/inventory/**")
          .hasAnyAuthority("ADMIN", "MANAGER", "USER")
          .requestMatchers("/api/dashboard/**")
          .hasAnyAuthority("ADMIN", "MANAGER", "USER")
          .anyRequest()
          .authenticated()
      )
      .authenticationProvider(authenticationProvider())
      .addFilterBefore(
        jwtAuthFilter,
        UsernamePasswordAuthenticationFilter.class
      )
      .build();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return email ->
      userRepository
        .findByEmail(email)
        .orElseThrow(() ->
          new UsernameNotFoundException("Usuario no encontrado: " + email)
        );
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
      userDetailsService()
    );
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(
    AuthenticationConfiguration config
  ) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
