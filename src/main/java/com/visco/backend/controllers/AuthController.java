package com.visco.backend.controllers;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @Operation(
    summary = "Register a new user",
    description = "Registers a new user and returns an authentication token"
  )
  public ResponseEntity<?> registerUser(
   @Valid @RequestBody UserRegisterRequest request
  ) {
    AuthResponse registeredUser = authService.register(request);
    return ResponseEntity.ok(registeredUser);
  }

  @PostMapping("/login")
  @Operation(
    summary = "Login user",
    description = "Authenticates a user and returns an authentication token"
  )
  public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request) {
    AuthResponse authResponse = authService.login(request);
    return ResponseEntity.ok(authResponse);
  }
}
