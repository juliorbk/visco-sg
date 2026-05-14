
package com.visco.backend.services;

import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

	@Value("${jwt.cookie.name:visco_auth_token}")
	private String cookieName;

	@Value("${jwt.expiration.hours:24}")
	private int cookieExpirationHours;

	// Crea la cookie segura para el Login
	public Cookie createJwtCookie(String jwtToken) {
		Cookie cookie = new Cookie(cookieName, jwtToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(false); // PONER EN TRUE EN PRODUCCIÓN (HTTPS)
		cookie.setPath("/"); // Accesible en toda la app
		cookie.setMaxAge(cookieExpirationHours * 60 * 60); // Segundos
		cookie.setAttribute("SameSite", "Lax"); // 'Lax' o 'Strict' para evitar CSRF
		return cookie;
	}

	// Crea una cookie "muerta" para el Logout
	public Cookie createLogoutCookie() {
		Cookie cookie = new Cookie(cookieName, null);
		cookie.setHttpOnly(true);
		cookie.setSecure(false); // Igual que arriba
		cookie.setPath("/");
		cookie.setMaxAge(0); // Esto le dice al navegador: "Bórrala ya"
		cookie.setAttribute("SameSite", "Lax");
		return cookie;
	}
}