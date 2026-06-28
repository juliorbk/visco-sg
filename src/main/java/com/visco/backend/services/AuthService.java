package com.visco.backend.services;

import com.visco.backend.models.dtos.AuthResponse;
import com.visco.backend.models.dtos.LoginRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserRegisterRequest;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.InviteToken;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.UserRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles authentication and registration business logic.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CostCenterRepository costCenterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final InviteTokenService inviteTokenService;
    private final Cloudinary cloudinary;

    /**
     * Registers a new user using an invite token and sends a welcome email.
     *
     * @param request the registration request
     * @return authentication response with JWT token
     */
    @Transactional
    public AuthResponse register(UserRegisterRequest request, MultipartFile profilePicture) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            log.warn("Registro fallido: Email ya registrado");
            throw new IllegalArgumentException("Email address is already in use");
        }

        InviteToken invite = inviteTokenService.findByToken(request.getInviteToken());
        UserRole intendedRole;
        if (invite.getIntendedRole() == null) {
            throw new IllegalStateException("Invite token is missing the intended role");
        }
        try {
            intendedRole = UserRole.valueOf(invite.getIntendedRole());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invite token has an invalid role configured");
        }

        CostCenter costCenter = null;
        if (invite.getCostCenterId() != null) {
            costCenter = costCenterRepository
                .findById(invite.getCostCenterId())
                .orElseThrow(() -> new IllegalArgumentException("Area no encontrada"));
        }

        User newUser = User.builder()
            .name(request.getName())
            .email(normalizedEmail)
            .password(passwordEncoder.encode(request.getPassword()))
            .role(intendedRole)
            .costCenter(costCenter)
            .active(true)
            .build();

        userRepository.save(newUser);
        inviteTokenService.consumeInvite(request.getInviteToken(), newUser);
        log.info("Registro exitoso: userId={}", newUser.getId());

        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    profilePicture.getInputStream(),
                    ObjectUtils.asMap(
                        "folder", "profile-pictures",
                        "public_id", "user_" + newUser.getId(),
                        "overwrite", true,
                        "resource_type", "image"
                    )
                );
                String url = (String) uploadResult.get("secure_url");
                newUser.setProfilePictureUrl(url);
                userRepository.save(newUser);
                log.info("Profile picture uploaded for user {}", newUser.getId());
            } catch (IOException e) {
                log.warn("Failed to upload profile picture for user {}: {}", newUser.getId(), e.getMessage());
            }
        }

        emailService.sendWelcomeEmail(newUser.getEmail(), newUser.getName());

        return buildResponse(newUser);
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login request
     * @return authentication response with JWT token and user info
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Intento de login");
        String email = request.getEmail().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.warn("Credenciales invalidas");
            throw e;
        }

        User user = userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> {
                log.error("Inconsistencia: Usuario autenticado pero no encontrado en DB");
                return new BadCredentialsException("Invalid credentials");
            });

        log.info("Autenticacion exitosa: userId={}", user.getId());
        log.debug("Generando JWT para usuario");

        // Retornamos la info completa para que el controlador decida qué hacer con ella
        return AuthResponse.builder()
            .token(jwtService.generateToken(user))
            .user(UserDTO.fromUser(user))
            .build();
    }

    private AuthResponse buildResponse(User user) {
        return AuthResponse.builder()
            .token(jwtService.generateToken(user))
            .user(UserDTO.fromUser(user))
            .build();
    }

    /**
     * Generates a new JWT token for the given user.
     *
     * @param user the authenticated user
     * @return a new JWT token string
     */
    public String refreshToken(User user) {
        return jwtService.generateToken(user);
    }

    /**
     * Retrieves the current user's profile by email.
     *
     * @param email the user's email
     * @return the current user DTO
     */
    public UserDTO getCurrentUser(String email) {
        User user = userRepository
            .findByEmailWithCostCenter(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return UserDTO.fromUser(user);
    }
}
