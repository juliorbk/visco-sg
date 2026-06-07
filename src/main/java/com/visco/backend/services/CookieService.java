package com.visco.backend.services;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

  @Value("${jwt.cookie.name:visco_auth_token}")
  private String cookieName;

  private final JwtService jwtService;

  @Value("${app.jwt.expiration-ms:86400000}")
  private long jwtExpirationMs;

  /**
   * Defaults to true — only set COOKIE_SECURE=false in local dev via .env.local. In any deployed
   * environment (staging, production) this must be true (HTTPS).
   */
  @Value("${jwt.cookie.secure:true}")
  private boolean cookieSecure;

  public CookieService(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  public Cookie createJwtCookie(String jwtToken) {
    Cookie cookie = new Cookie(cookieName, jwtToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/");
    // Sincronizado con la expiración real del JWT para evitar que la cookie
    // sobreviva tokens ya rechazados por el servidor.
    long maxAgeSeconds = Math.max(1, jwtExpirationMs / 1000);
    cookie.setMaxAge((int) Math.min(maxAgeSeconds, Integer.MAX_VALUE));
    if (cookieSecure) {
      cookie.setAttribute("SameSite", "None");
    } else {
      cookie.setAttribute("SameSite", "Lax");
    }
    return cookie;
  }

  public Cookie createLogoutCookie() {
    Cookie cookie = new Cookie(cookieName, null);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    if (cookieSecure) {
      cookie.setAttribute("SameSite", "None");
    } else {
      cookie.setAttribute("SameSite", "Lax");
    }

    return cookie;
  }
}
