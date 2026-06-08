package com.visco.backend.controllers;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.ForgotPasswordRequest;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.ResetPasswordRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.services.AuthService;
import com.visco.backend.services.CookieService;
import com.visco.backend.services.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Registers a new user using an invite token and returns auth data"
    )
    public ResponseEntity<?> registerUser(
        @Valid @RequestBody UserRegisterRequest request,
        HttpServletResponse response
    ) {
        AuthResponse registeredUser = authService.register(request);
        var jwtCookie = cookieService.createJwtCookie(registeredUser.getToken());
        response.addCookie(jwtCookie);
        registeredUser.setToken(null);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticates a user and returns an authentication token"
    )
    public ResponseEntity<AuthResponse> loginUser(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        AuthResponse authData = authService.login(request);
        var jwtCookie = cookieService.createJwtCookie(authData.getToken());
        response.addCookie(jwtCookie);
        authData.setToken(null);
        return ResponseEntity.ok(authData);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Clears the JWT cookie and logs out the user")
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        var logoutCookie = cookieService.createLogoutCookie();
        response.addCookie(logoutCookie);
        return ResponseEntity.ok().body("Logout successful");
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Request password reset",
        description = "Generates a single-use reset token and emails it. Always returns 200 to prevent user enumeration."
    )
    public ResponseEntity<?> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.requestReset(request.email());
        // Identical response regardless of whether the email exists.
        return ResponseEntity.ok(
          Map.of(
            "message",
            "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña."
          )
        );
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password",
        description = "Validates a reset token and updates the user's password."
    )
    public ResponseEntity<?> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.consumeToken(request.token(), request.newPassword());
        return ResponseEntity.ok(
          Map.of("message", "Contraseña actualizada exitosamente.")
        );
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current user",
        description = "Returns the authenticated user information from the JWT cookie or bearer token"
    )
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        log.debug("getCurrentUser called, authentication: {}", authentication);
        if (authentication == null || authentication.getName() == null) {
            log.warn("No authentication found in /me request");
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        log.debug("Fetching user for email: {}", email);
        UserDTO user = authService.getCurrentUser(email);
        log.debug("User fetched: {}", user);
        return ResponseEntity.ok(user);
    }
}
