package com.visco.backend.config;

import com.visco.backend.repositories.UserRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String JWT_COOKIE_NAME = "visco_auth_token";

  private final JwtService jwtService;
  private final UserRepository userRepository;

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
        userRepository
          .findByEmail(email)
          .ifPresent(user -> {
            if (jwtService.isTokenValid(token, user)) {
              var authToken = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
              );
              authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
              );
              SecurityContextHolder.getContext().setAuthentication(authToken);
            }
          });
      }
    } catch (ExpiredJwtException e) {
      log.warn("Token expirado para la ruta: {}", request.getRequestURI());
    } catch (JwtException e) {
      log.warn("Token invalido: {}", e.getMessage());
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
        if (JWT_COOKIE_NAME.equals(cookie.getName())) {
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
