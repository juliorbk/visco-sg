package com.visco.backend.config;

import com.visco.backend.repositories.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final UserRepository userRepository;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
      List.of("http://localhost:3000", "http://localhost:5173")
    ); // Add your React ports
    configuration.setAllowedMethods(
      List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
    );
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

    UrlBasedCorsConfigurationSource source =
      new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
    throws Exception {
    return http
      .cors(Customizer.withDefaults())
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
          .hasAnyAuthority("ADMIN", "MANAGER", "WAREHOUSEMAN")
          .requestMatchers("/api/dashboard/**")
          .hasAnyAuthority("ADMIN", "MANAGER", "WAREHOUSEMAN", "PROCUREMENT")
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
