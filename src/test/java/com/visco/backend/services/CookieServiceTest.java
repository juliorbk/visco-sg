package com.visco.backend.services;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CookieServiceTest {

    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new CookieService();
        ReflectionTestUtils.setField(cookieService, "cookieName", "visco_auth_token");
        ReflectionTestUtils.setField(cookieService, "cookieExpirationHours", 24);
        ReflectionTestUtils.setField(cookieService, "cookieSecure", true);
    }

    @Test
    void createJwtCookie_Success() {
        Cookie cookie = cookieService.createJwtCookie("test-jwt-token");

        assertThat(cookie).isNotNull();
        assertThat(cookie.getName()).isEqualTo("visco_auth_token");
        assertThat(cookie.getValue()).isEqualTo("test-jwt-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(24 * 60 * 60);
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    @Test
    void createLogoutCookie_Success() {
        Cookie cookie = cookieService.createLogoutCookie();

        assertThat(cookie).isNotNull();
        assertThat(cookie.getName()).isEqualTo("visco_auth_token");
        assertThat(cookie.getValue()).isNull();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
    }

    @Test
    void createJwtCookie_WithCustomValues() {
        ReflectionTestUtils.setField(cookieService, "cookieName", "custom_token");
        ReflectionTestUtils.setField(cookieService, "cookieExpirationHours", 48);
        ReflectionTestUtils.setField(cookieService, "cookieSecure", false);

        Cookie cookie = cookieService.createJwtCookie("custom-jwt");

        assertThat(cookie.getName()).isEqualTo("custom_token");
        assertThat(cookie.getMaxAge()).isEqualTo(48 * 60 * 60);
        assertThat(cookie.getSecure()).isFalse();
    }
}
