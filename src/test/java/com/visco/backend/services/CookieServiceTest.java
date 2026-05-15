package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class CookieServiceTest {

    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new CookieService();
        ReflectionTestUtils.setField(cookieService, "cookieName", "visco_auth_token");
        ReflectionTestUtils.setField(cookieService, "cookieExpirationHours", 24);
    }

    @Test
    void createJwtCookie_shouldReturnSecureCookie() {
        String jwtToken = "test-jwt-token-value";

        Cookie cookie = cookieService.createJwtCookie(jwtToken);

        assertNotNull(cookie);
        assertEquals("visco_auth_token", cookie.getName());
        assertEquals("test-jwt-token-value", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.getSecure());
        assertEquals("/", cookie.getPath());
        assertEquals(24 * 60 * 60, cookie.getMaxAge());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
    }

    @Test
    void createLogoutCookie_shouldReturnExpiredCookie() {
        Cookie cookie = cookieService.createLogoutCookie();

        assertNotNull(cookie);
        assertEquals("visco_auth_token", cookie.getName());
        assertNull(cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.getSecure());
        assertEquals("/", cookie.getPath());
        assertEquals(0, cookie.getMaxAge());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
    }
}
