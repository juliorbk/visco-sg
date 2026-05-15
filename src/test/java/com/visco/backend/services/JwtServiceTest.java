package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private String secret;

    @BeforeEach
    void setUp() {
        // Generate a valid HS256 key for testing
        SecretKey key = Keys.hmacShaKeyFor(Jwts.SIG.HS256.key().build().getEncoded());
        secret = Base64.getEncoder().encodeToString(key.getEncoded());

        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L); // 1 hour
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.MANAGER)
                .build();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.MANAGER)
                .build();

        String token = jwtService.generateToken(user);
        String email = jwtService.extractEmail(token);

        assertEquals("test@example.com", email);
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.ADMIN)
                .build();

        String token = jwtService.generateToken(user);
        String role = jwtService.extractRole(token);

        assertEquals("ADMIN", role);
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.MANAGER)
                .build();

        String token = jwtService.generateToken(user);
        boolean valid = jwtService.isTokenValid(token, user);

        assertTrue(valid);
    }

    @Test
    void isTokenValid_shouldReturnFalseForWrongUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.MANAGER)
                .build();

        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Other User")
                .email("other@example.com")
                .role(UserRole.ADMIN)
                .build();

        String token = jwtService.generateToken(user);
        boolean valid = jwtService.isTokenValid(token, otherUser);

        assertFalse(valid);
    }

    @Test
    void tokenShouldContainClaims() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.MANAGER)
                .build();

        String token = jwtService.generateToken(user);

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(keyBytes))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("test@example.com", claims.getSubject());
        assertEquals("MANAGER", claims.get("role"));
        assertEquals("Test User", claims.get("name"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }
}
