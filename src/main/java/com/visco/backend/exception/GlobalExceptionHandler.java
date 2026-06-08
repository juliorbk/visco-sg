package com.visco.backend.exception;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST controllers. Translates exceptions
 * thrown by the application layer into consistent JSON error responses with
 * appropriate HTTP status codes. Covers validation, authentication,
 * authorization, data integrity, and unexpected errors.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation errors (@Valid) ────────────────────────────────────────────

    /**
     * Handles {@link MethodArgumentNotValidException} thrown when
     * {@code @Valid} validation fails. Returns a 400 response with
     * per-field error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Validation failed", fieldErrors));
    }

    // ── Domain / business logic errors ───────────────────────────────────────

    /**
     * Handles {@link EntityNotFoundException} when a requested entity is
     * not found. Returns a 404 response.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * Handles {@link IllegalArgumentException} for invalid argument
     * values. Returns a 400 response with the exception message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * Handles {@link IllegalStateException} for business-logic conflicts.
     * Returns a 409 Conflict response.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    // ── Auth errors ───────────────────────────────────────────────────────────

    /**
     * Handles {@link BadCredentialsException} on login failure. Returns a
     * 401 response without revealing whether the email or password was
     * incorrect.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("Invalid email or password"));
    }

    /**
     * Handles {@link UsernameNotFoundException} when a user is not found.
     * Returns a 401 response.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("User not found"));
    }

    /**
     * Catch-all handler for other {@link AuthenticationException}
     * subtypes. Returns a 401 response.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("Authentication failed"));
    }

    /**
     * Handles {@link AuthorizationDeniedException} when the user lacks
     * the required role or permission. Returns a 403 response.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AuthorizationDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("You do not have permission to perform this action"));
    }

    // ── Type / format errors ──────────────────────────────────────────────────

    /**
     * Handles {@link DateTimeParseException} for invalid date string
     * formats. Returns a 400 response.
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParse(DateTimeParseException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Invalid date format: " + ex.getMessage()));
    }

    /**
     * Handles {@link MethodArgumentTypeMismatchException} when a method
     * argument cannot be converted to the required type. Returns a 400
     * response.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Invalid value for parameter: " + ex.getName()));
    }

    /**
     * Handles {@link HttpMessageNotReadableException} for malformed
     * request bodies (e.g. invalid JSON). Returns a 400 response.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Malformed request body"));
    }

    /**
     * Handles {@link MissingServletRequestParameterException} when a
     * required request parameter is missing. Returns a 400 response.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Missing required parameter: " + ex.getParameterName()));
    }

    /**
     * Handles {@link OptimisticLockException} for concurrent-update
     * conflicts. Returns a 409 response instructing the client to retry.
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("The record was updated by another user. Please refresh and retry."));
    }

    /**
     * Handles {@link DataIntegrityViolationException} for database
     * constraint violations such as unique-key duplicates. Returns a
     * 409 response.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("Operation violates a database constraint. Please check for duplicates or references."));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    /**
     * Catch-all handler for any unhandled exception. Logs the error and
     * returns a 500 Internal Server Error response with a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("An unexpected error occurred. Please try again later."));
    }

    // ── Response body ─────────────────────────────────────────────────────────

    /**
     * Standard error response body with a top-level message, optional
     * field-level errors, and a timestamp of when the error occurred.
     */
    public record ErrorResponse(
            String message,
            Map<String, String> errors,
            Instant timestamp) {

        static ErrorResponse of(String message) {
            return new ErrorResponse(message, null, Instant.now());
        }

        static ErrorResponse of(String message, Map<String, String> errors) {
            return new ErrorResponse(message, errors, Instant.now());
        }
    }
}