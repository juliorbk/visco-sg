package com.visco.backend.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visco.backend.models.dtos.UpdateUserRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.services.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints (admin only)")
public class UserController {

	private final AdminService adminService;

	@GetMapping
	@Operation(summary = "List all users", description = "Returns a paginated list of all users")
	public ResponseEntity<Page<UserDTO>> getAllUsers(Pageable pageable) {
		return ResponseEntity.ok(adminService.getAllUsers(pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get user by ID", description = "Returns a specific user")
	public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
		return ResponseEntity.ok(adminService.getUserById(id));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update user", description = "Updates user information")
	public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(adminService.updateUser(id, request));
	}

	@PatchMapping("/{id}/deactivate")
	@Operation(summary = "Deactivate user", description = "Deactivates a user account")
	public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
		adminService.deactivateUser(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/activate")
	@Operation(summary = "Activate user", description = "Activates a previously deactivated user account")
	public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
		adminService.activateUser(id);
		return ResponseEntity.noContent().build();
	}
}
