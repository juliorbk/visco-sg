package com.visco.backend.services;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.models.entities.RequestingArea;
import com.visco.backend.models.entities.User;
import com.visco.backend.repositories.AreaRepository;
import com.visco.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final AreaRepository areaRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final AuthenticationManager authenticationManager;

  public AuthResponse register(UserRegisterRequest request) {
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      log.warn(
        "Registro fallido: Email ya registrado ({})",
        request.getEmail()
      );
      throw new IllegalArgumentException("Email address is already in use");
    }

    RequestingArea area = null;
    if (request.getAreaId() != null) {
      area = areaRepository
        .findById(request.getAreaId())
        .orElseThrow(() -> new IllegalArgumentException("Área no encontrada"));
    }

    User newUser = User.builder()
      .name(request.getName())
      .email(request.getEmail())
      .password(passwordEncoder.encode(request.getPassword()))
      .role(request.getRole())
      .area(area)
      .active(true)
      .build();

    userRepository.save(newUser);
    log.info("Registro exitoso para usuario ID: {}", newUser.getId());

    try {
      emailService.sendWelcomeEmail(newUser.getEmail(), newUser.getName());
    } catch (Exception e) {
      log.error(
        "Failed to send welcome email to {}. Error: {}",
        newUser.getEmail(),
        e.getMessage()
      );
    }

    return buildResponse(newUser);
  }

  public AuthResponse login(LoginRequest request) {
    log.info("Intento de login para el usuario: {}", request.getEmail());

    authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(
        request.getEmail(),
        request.getPassword()
      )
    );

    User user = userRepository
      .findByEmail(request.getEmail())
      .orElseThrow(() -> {
        log.error(
          "Inconsistencia: Usuario autenticado pero no encontrado en DB ({})",
          request.getEmail()
        );
        return new BadCredentialsException("Invalid credentials");
      });

    return buildResponse(user);
  }

  private AuthResponse buildResponse(User user) {
    return AuthResponse.builder()
      .token(jwtService.generateToken(user))
      .user(UserDTO.fromUser(user))
      .build();
  }
}
