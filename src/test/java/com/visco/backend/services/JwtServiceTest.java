package com.visco.backend.services;

import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "c2VjcmV0a2V5MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODk=");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.USER)
                .active(true)
                .build();
    }

    @Test
    void generateToken_Success() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_FromToken() {
        String token = jwtService.generateToken(testUser);

        String email = jwtService.extractEmail(token);

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void extractRole_FromToken() {
        String token = jwtService.generateToken(testUser);

        String role = jwtService.extractRole(token);

        assertThat(role).isEqualTo("USER");
    }

    @Test
    void isTokenValid_WithUser() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_WithEmail() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, "test@example.com");

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_FailsWithWrongEmail() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, "wrong@example.com");

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_FailsWithInvalidToken() {
        assertThatThrownBy(() -> jwtService.isTokenValid("invalid.token.here", testUser))
                .isInstanceOf(Exception.class);
    }
}
