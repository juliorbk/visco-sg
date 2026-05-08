package com.visco.backend.services;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.models.entities.User;
import com.visco.backend.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final EmailService emailService;

  public AuthService(
    UserRepository userRepository,
    PasswordEncoder passwordEncoder,
    JwtService jwtService,
    EmailService emailService
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.emailService = emailService;
  }

  //Method to handle user registration

  public AuthResponse register(UserRegisterRequest request) {
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      log.warn(
        "Registro fallido: Email ya registrado ({})",
        request.getEmail()
      );
      throw new BadCredentialsException("Use another email address");
    }

    User newUser = User.builder()
      .name(request.getName())
      .email(request.getEmail())
      .password(passwordEncoder.encode(request.getPassword()))
      .role(request.getRole())
      .area(request.getArea())
      .build();

    userRepository.save(newUser);

    emailService.sendWelcomeEmail(newUser.getEmail(), newUser.getName());

    // Generate JSON Web Token (JWT) for the new user
    String token = jwtService.generateToken(newUser);

    UserDTO userDTO = UserDTO.builder()
      .id(newUser.getId())
      .name(newUser.getName())
      .email(newUser.getEmail())
      .role(newUser.getRole())
      .build();

    log.info("Registro exitoso para usuario ID: {}", newUser.getId());

    //Return the token and user info in the response
    return AuthResponse.builder().token(token).user(userDTO).build();
  }

  // Method to handle user login

  public AuthResponse login(LoginRequest request) {
    log.info("Intento de login para el usuario: {}", request.getEmail());

    User user = userRepository
      .findByEmail(request.getEmail())
      .orElseThrow(() -> {
        log.warn(
          "Fallo de login: Usuario no encontrado ({})",
          request.getEmail()
        );
        return new BadCredentialsException("Credenciales inválidas");
      });

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      log.warn(
        "Fallo de login: Contraseña incorrecta para ({})",
        request.getEmail()
      );
      throw new BadCredentialsException("Credenciales inválidas");
    }

    // Generate JSON Web Token (JWT) for the authenticated user
    String token = jwtService.generateToken(user);

    UserDTO userDTO = UserDTO.builder()
      .id(user.getId())
      .name(user.getName())
      .email(user.getEmail())
      .role(user.getRole())
      .build();

    log.info("Login exitoso para usuario ID: {}", user.getId());

    //Return the token and user info in the response
    return AuthResponse.builder().token(token).user(userDTO).build();
  }
}
