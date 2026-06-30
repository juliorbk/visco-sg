package com.visco.backend.config;

import com.visco.backend.config.CustomUserDetailsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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

/**
 * Configures Spring Security for the application: stateless session management,
 * JWT-based authentication via {@link JwtAuthFilter}, CORS, role-based
 * authorization with a role hierarchy (SUPERADMIN > ADMIN > MANAGER >
 * PROCUREMENT / WAREHOUSEMAN > USER), and BCrypt password encoding.
 */
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

  @Value("${app.openapi.enabled:false}")
  private boolean openapiEnabled;

  /**
   * Configures CORS with allowed origins from {@code app.cors.allowed-origins},
   * standard HTTP methods, and credential support (cookies). Used by the
   * {@code cors()} DSL in the security filter chain.
   *
   * @return the {@link CorsConfigurationSource} for the application
   */
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

  /**
   * Role hierarchy. The {@code ROLE_} prefix is REQUIRED so that
   * {@code RoleHierarchyImpl} can match the entries against the
   * {@code SimpleGrantedAuthority} names set by
   * {@code CustomUserDetailsService} (which prepends {@code ROLE_}).
   *
   * <p>A line like {@code "ROLE_SUPERADMIN > ROLE_ADMIN"} means that
   * a user holding {@code ROLE_SUPERADMIN} is also considered to hold
   * {@code ROLE_ADMIN}, so {@code hasRole("ADMIN")} (and
   * {@code hasAnyRole("ADMIN", ...)}) returns true for them.
   *
   * <p>This lets a {@code SUPERADMIN} pass every {@code hasRole("ADMIN")}
   * check across the app without having to rewrite every matchers list.
   */
  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
      """
      ROLE_SUPERADMIN > ROLE_ADMIN
      ROLE_ADMIN > ROLE_MANAGER
      ROLE_ADMIN > ROLE_PROCUREMENT
      ROLE_ADMIN > ROLE_WAREHOUSEMAN
      ROLE_MANAGER > ROLE_USER
      ROLE_PROCUREMENT > ROLE_USER
      ROLE_WAREHOUSEMAN > ROLE_USER
      """
    );
  }

  /**
   * Defines the Spring Security filter chain: disables CSRF, configures CORS,
   * sets security headers (HSTS, CSP, frame-options), registers the JWT auth
   * filter, and enforces role-based access rules for each API route group.
   *
   * @param http the {@link HttpSecurity} to configure
   * @return the built {@link SecurityFilterChain}
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
    throws Exception {
    http
      .csrf(csrf -> csrf.disable()) // CSRF is usually disabled for stateless APIs
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .httpBasic(httpBasic -> httpBasic.disable())
      .headers(headers ->
        headers
          .httpStrictTransportSecurity(hsts ->
            hsts
              .includeSubDomains(true)
              .maxAgeInSeconds(31536000)
          )
          .contentSecurityPolicy(csp ->
            csp.policyDirectives("default-src 'self'")
          )
          .frameOptions(frame -> frame.deny())
          .contentTypeOptions(contentType -> {})
      )
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
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/health"
          )
          .permitAll()
          // Auth endpoints que requieren autenticación
          .requestMatchers("/api/auth/me", "/api/auth/logout")
          .authenticated()
          // OpenAPI / Swagger: gated by app.openapi.enabled (default OFF in prod)
          .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
          .access((authentication, context) ->
            new org.springframework.security.authorization.AuthorizationDecision(openapiEnabled)
          )
          // Solo ADMIN
          .requestMatchers(
            "/api/users/**"
          )
          .hasRole("ADMIN")
          // Cualquier usuario autenticado (dropdowns de solo lectura)
          .requestMatchers(
            "/api/cost-centers/**",
            "/api/management/**",
            "/api/general-management/**"
          )
          .authenticated()
          // Cualquier usuario autenticado: GET de referencia (listas, dropdowns, dashboards)
          .requestMatchers(
            HttpMethod.GET,
            "/api/employees/**",
            "/api/suppliers/**",
            "/api/supplier-categories/**",
            "/api/procurement/**",
            "/api/requisitions/**",
            "/api/warehouse/**",
            "/api/invoices/**",
            "/api/inventory/**",
            "/api/reports/**",
            "/api/dashboard/**"
          )
          .authenticated()
          // ADMIN, MANAGER y PROCUREMENT: escritura proveedores, compras, requisiciones
          .requestMatchers(
            "/api/suppliers/**",
            "/api/supplier-categories/**",
            "/api/procurement/**",
            "/api/requisitions/**"
          )
          .hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT")
          // ADMIN, MANAGER, PROCUREMENT y WAREHOUSEMAN: escritura almacen
          .requestMatchers("/api/warehouse/**")
          .hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT", "WAREHOUSEMAN")
          // ADMIN, MANAGER, PROCUREMENT: escritura facturas
          .requestMatchers("/api/invoices/**")
          .hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT")
          //Admin
          .requestMatchers("/api/migration/**")
          .hasRole("ADMIN")
          .requestMatchers("/api/admin/**")
          .hasRole("ADMIN")
          .requestMatchers("/api/invites/by-token/**")
          .permitAll()
          .requestMatchers("/api/invites/**")
          .hasRole("ADMIN")
          .requestMatchers(HttpMethod.DELETE, "/api/users/**")
          .hasRole("SUPERADMIN")
          .requestMatchers(HttpMethod.GET, "/api/users/*/references")
          .hasRole("SUPERADMIN")
          // Métricas Prometheus públicas (solo lectura, sin datos sensibles)
          .requestMatchers("/actuator/prometheus")
          .permitAll()
          .requestMatchers("/actuator/**")
          .hasRole("SUPERADMIN")
          // Roles específicos: escritura inventario, reportes
          .requestMatchers("/api/inventory/**")
          .hasAnyRole("ADMIN", "MANAGER", "WAREHOUSEMAN")
          .requestMatchers("/api/reports/**")
          .hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT")
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

  /**
   * Returns an {@link AccessDeniedHandler} that responds with a 403 status
   * and a JSON body instead of redirecting to a login page.
   *
   * @return the access-denied handler bean
   */
  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setStatus(403);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Access denied\"}");
    };
  }

  /**
   * Configures a {@link DaoAuthenticationProvider} backed by
   * {@link CustomUserDetailsService} and the BCrypt password encoder.
   *
   * @return the authentication provider bean
   */
  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
      customUserDetailsService
    );
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  /**
   * Exposes the {@link AuthenticationManager} from Spring's
   * {@link AuthenticationConfiguration} for use in custom authentication
   * controllers.
   *
   * @param config the authentication configuration
   * @return the authentication manager
   * @throws Exception if the manager cannot be obtained
   */
  @Bean
  public AuthenticationManager authenticationManager(
    AuthenticationConfiguration config
  ) throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * Provides a {@link BCryptPasswordEncoder} for hashing and verifying
   * user passwords.
   *
   * @return the password encoder bean
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
