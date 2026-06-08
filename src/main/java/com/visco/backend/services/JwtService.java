package com.visco.backend.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.visco.backend.models.entities.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Handles JWT token generation, parsing, and validation.
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}") // Must match the key in application.properties exactly
    private String secretKey;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @jakarta.annotation.PostConstruct
    private void validateSecret() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                "app.jwt.secret (JWT_SECRET env var) is not configured. " +
                "Set it to a Base64-encoded value of at least 32 bytes (256 bits)."
            );
        }
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                    "app.jwt.secret decodes to " + keyBytes.length +
                    " bytes; HS256 requires at least 32. Generate a stronger secret."
                );
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "app.jwt.secret is not valid Base64: " + e.getMessage()
            );
        }
    }

    private SecretKey getSigningKey() {
        // This correctly decodes a Base64 string from your properties file
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token containing user claims.
     *
     * @param user the authenticated user
     * @return the signed JWT token string
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("name", user.getName());

        return Jwts.builder().claims(claims).subject(user.getEmail()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey()).compact();
    }

    /**
     * Extracts the email (subject) from a JWT token.
     *
     * @param token the JWT token
     * @return the email claim
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts the role claim from a JWT token.
     *
     * @param token the JWT token
     * @return the role string
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Validates a JWT token against a user entity.
     *
     * @param token the JWT token
     * @param user  the user to validate against
     * @return true if the token is valid and matches the user
     */
    public boolean isTokenValid(String token, User user) {
        final String email = extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token);
    }

    /**
     * Validates a JWT token against an email string.
     *
     * @param token the JWT token
     * @param email the expected email
     * @return true if the token is valid and matches the email
     */
    public boolean isTokenValid(String token, String email) {
        return extractEmail(token).equals(email) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token)
                .getPayload();
    }
}
