package com.visco.backend.config;

import com.visco.backend.repositories.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

  @Value(
    "${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173, https://viscoorinocosia.vercel.app/**}"
  )
  private String[] allowedOrigins;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(allowedOrigins));
    configuration.setAllowedMethods(
      List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
    );
    configuration.setAllowedHeaders(
      List.of("Authorization", "Content-Type", "Cookie")
    );
    configuration.setExposedHeaders(List.of("Set-Cookie"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
      new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
    throws Exception {
    http
      .csrf(csrf -> csrf.disable()) // CSRF is usually disabled for stateless APIs
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .httpBasic(httpBasic -> httpBasic.disable())
      .exceptionHandling(ex ->
        ex.authenticationEntryPoint((request, response, authException) -> {
          response.setStatus(401);
          response.setContentType("application/json");
          response.getWriter().write("{\"error\": \"Unauthorized\"}");
        })
      )
      .authorizeHttpRequests(auth ->
        auth
          // Público
          .requestMatchers(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/logout",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/migration/**"
          )
          .permitAll()
          // Solo ADMIN (y cost-centers requiere al menos autenticación)
          .requestMatchers("/api/users/**", "/api/cost-centers/**")
          .hasAuthority("ADMIN")
          // ADMIN, MANAGER y PROCUREMENT
          .requestMatchers(
            "/api/suppliers/**",
            "/api/procurement/**",
            "/api/requisitions/**"
          )
          .hasAnyAuthority("ADMIN", "MANAGER", "PROCUREMENT")
          // ADMIN, MANAGER, PROCUREMENT y WAREHOUSEMAN
          .requestMatchers("/api/warehouse/**")
          .hasAnyAuthority("ADMIN", "MANAGER", "PROCUREMENT", "WAREHOUSEMAN")
          // ADMIN, MANAGER, PROCUREMENT
          .requestMatchers("/api/invoices/**")
          .hasAnyAuthority("ADMIN", "MANAGER", "PROCUREMENT")
          // Roles específicos
          .requestMatchers("/api/inventory/**")
          .hasAnyAuthority("ADMIN", "MANAGER", "WAREHOUSEMAN")
          .requestMatchers("/api/dashboard/**")
          .hasAnyAuthority("ADMIN", "MANAGER", "WAREHOUSEMAN", "PROCUREMENT")
          // Cualquier otra petición debe estar autenticada
          .anyRequest()
          .authenticated()
      )
      // Define el manejo de sesiones como STATELESS (Sin estado) para JWT
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      // Aquí es donde tu código se cortó
      .authenticationProvider(authenticationProvider())
      // Añadir el filtro JWT antes del filtro de autenticación por usuario/contraseña
      // estándar
      .addFilterBefore(
        jwtAuthFilter,
        UsernamePasswordAuthenticationFilter.class
      );

    return http.build();
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
