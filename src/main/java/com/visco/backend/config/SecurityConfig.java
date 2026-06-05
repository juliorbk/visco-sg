package com.visco.backend.config;

import com.visco.backend.config.CustomUserDetailsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
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
  private final CustomUserDetailsService customUserDetailsService;

  @Value(
    "${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://viscoorinocosia.vercel.app}"
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
        ex
          .authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
          })
          .accessDeniedHandler(accessDeniedHandler())
      )
      .authorizeHttpRequests(auth ->
        auth
          // Público
          .requestMatchers(
            "/api/auth/register",
            "/api/auth/login",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/health"
          )
          .permitAll()
          // Solo ADMIN (y cost-centers requiere al menos autenticación)
          .requestMatchers(
            "/api/users/**",
            "/api/employees/**",
            "/api/cost-centers/**",
            "/api/management/**",
            "/api/general-management/**"
          )
          .hasRole("ADMIN")
          // ADMIN, MANAGER y PROCUREMENT
          .requestMatchers(
            "/api/suppliers/**",
            "/api/procurement/**",
            "/api/requisitions/**"
          )
          .hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT")
          // ADMIN, MANAGER, PROCUREMENT y WAREHOUSEMAN
          .requestMatchers("/api/warehouse/**")
          .hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT", "WAREHOUSEMAN")
          // ADMIN, MANAGER, PROCUREMENT
          .requestMatchers("/api/invoices/**")
          .hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT")
          //Admin
          .requestMatchers("/api/migration/**")
          .hasRole("ADMIN")
          .requestMatchers("/api/admin/**")
          .hasRole("ADMIN")
          .requestMatchers("/api/invites/**")
          .hasRole("ADMIN")
          .requestMatchers(HttpMethod.DELETE, "/api/users/**")
          .hasRole("SUPERADMIN")
          .requestMatchers(HttpMethod.GET, "/api/users/*/references")
          .hasRole("SUPERADMIN")
          .requestMatchers("/actuator/**")
          .hasRole("ADMIN")
          // Roles específicos
          .requestMatchers("/api/inventory/**", "/api/warehouse/dispatches/**")
          .hasAnyRole("ADMIN", "MANAGER", "WAREHOUSEMAN")
          .requestMatchers("/api/reports/**")
          .hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT")
          .requestMatchers("/api/dashboard/**")
          .hasAnyRole("ADMIN", "MANAGER", "WAREHOUSEMAN", "PROCUREMENT")
          // Cualquier otra petición debe estar autenticada
          .anyRequest()
          .authenticated()
      )
      // Define el manejo de sesiones como STATELESS (Sin estado) para JWT
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authenticationProvider(authenticationProvider())
      // Añadir el filtro JWT antes del filtro de autenticación por usuario/contraseña estándar
      .addFilterBefore(
        jwtAuthFilter,
        UsernamePasswordAuthenticationFilter.class
      );

    return http.build();
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setStatus(403);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Access denied\"}");
    };
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
      customUserDetailsService
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
