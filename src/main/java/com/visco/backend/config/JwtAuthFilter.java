package com.visco.backend.config;

// Asegúrate de importar tu servicio
import com.visco.backend.config.CustomUserDetailsService;
import com.visco.backend.services.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts a JWT from the {@code Authorization} header or an HttpOnly
 * cookie, validates it via {@link JwtService}, and sets the Spring Security
 * {@code SecurityContext} with a {@link UserPrincipal} loaded from the
 * database. Applied on every request through the filter chain.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  @Value("${jwt.cookie.name:visco_auth_token}")
  private String jwtCookieName;

  private final JwtService jwtService;
  // 1. Inyectamos tu nuevo servicio
  private final CustomUserDetailsService userDetailsService;

  /**
   * Extracts and validates the JWT from the request. If valid, loads the
   * corresponding {@link UserPrincipal} from the database and sets the
   * Spring Security authentication context. Returns 401 for expired or
   * invalid tokens, or when the referenced user no longer exists.
   */
  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    final String token = extractToken(request);

    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      final String email = jwtService.extractEmail(token);

      if (
        email != null &&
        SecurityContextHolder.getContext().getAuthentication() == null
      ) {
        if (jwtService.isTokenValid(token, email)) {
          // 2. Cargamos el usuario desde la base de datos usando el email.
          // Esto devuelve tu 'UserPrincipal' completo (con el UUID)
          UserDetails userDetails;
          try {
            userDetails = userDetailsService.loadUserByUsername(email);
          } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            // The JWT references a user that no longer exists (hard-deleted,
            // account purged, etc.). Clean the SecurityContext and short-circuit
            // with 401 instead of letting the exception bubble up as a 500.
            SecurityContextHolder.clearContext();
            log.warn("JWT references missing user: {}", email);
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"User no longer exists\"}");
            return;
          } catch (Exception ex) {
            // Any other failure (DB outage, query timeout, ...) should not
            // produce a 500 stack trace. Treat as an auth failure and
            // continue the chain so the entry point can issue 401.
            SecurityContextHolder.clearContext();
            log.error("Error loading user for JWT: {}", ex.getMessage(), ex);
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Authentication failed\"}");
            return;
          }

          // 2b. Verificamos que el usuario esté activo. Un usuario desactivado
          // que aún conserve un JWT no expirado queda bloqueado aquí.
          if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            log.warn("Intento de acceso con cuenta inactiva: {}", email);
            response.setStatus(401);
            response.setContentType("application/json");
            response
              .getWriter()
              .write("{\"error\": \"Cuenta inactiva o bloqueada\"}");
            return;
          }

          // 3. Pasamos tu 'userDetails' como el "Principal" principal
          var authToken = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
          );

          authToken.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
          );
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (ExpiredJwtException e) {
      log.warn("Token expirado para la ruta: {}", request.getRequestURI());
      response.setStatus(401);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Token expirado\"}");
      return;
    } catch (JwtException e) {
      log.warn("Token invalido: {}", e.getMessage());
      response.setStatus(401);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"Token invalido\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  /** Extrae el JWT del header Authorization o del cookie HttpOnly */
  private String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (jwtCookieName.equals(cookie.getName())) {
          String value = cookie.getValue();
          if (value != null && !value.isBlank()) {
            return value;
          }
        }
      }
    }
    return null;
  }
}
