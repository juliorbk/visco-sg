package com.visco.backend.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.models.entities.User;
import com.visco.backend.services.AuthService;
import com.visco.backend.services.CookieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final CookieService cookieService;

  @PostMapping("/register")
  @Operation(summary = "Register a new user", description = "Registers a new user and returns an authentication token")
  public ResponseEntity<?> registerUser(
      @Valid @RequestBody UserRegisterRequest request) {
    AuthResponse registeredUser = authService.register(request);
    return ResponseEntity.ok(registeredUser);
  }

  @PostMapping("/login")
  @Operation(summary = "Login user", description = "Authenticates a user and returns an authentication token")
  public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request,
      HttpServletResponse response) {
    // We create de login request
    AuthResponse authData = authService.login(request);
    // if validates ok we extract the JWT and create the cookie
    var jwtCookie = cookieService.createJwtCookie(authData.getToken());

    // add the cookie to the http response
    response.addCookie(jwtCookie);

    // We delete the token, for security
    authData.setToken(null);
    return ResponseEntity.ok(authData);
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logoutUser(HttpServletResponse response) {
    var logoutCookie = cookieService.createLogoutCookie();
    response.addCookie(logoutCookie);
    return ResponseEntity.ok().body("Logout successful");
  }

  @GetMapping("/me")
  @Operation(summary = "Get current user",
      description = "Returns the authenticated user information from the JWT cookie or bearer token")
  public ResponseEntity<?> getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()
        || "anonymousUser".equals(auth.getPrincipal())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "No autenticado"));
    }
    User user = (User) auth.getPrincipal();
    return ResponseEntity.ok(UserDTO.fromUser(user));
  }
}
