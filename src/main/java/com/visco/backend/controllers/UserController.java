package com.visco.backend.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visco.backend.models.dtos.UpdateUserRequest;
import com.visco.backend.models.dtos.UserDTO;
import com.visco.backend.models.dtos.UserReferencesResponse;
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
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Update user", description = "Updates user information")
	public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(adminService.updateUser(id, request));
	}

	@PatchMapping("/{id}/deactivate")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Deactivate user", description = "Deactivates a user account")
	public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
		adminService.deactivateUser(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/activate")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Activate user", description = "Activates a previously deactivated user account")
	public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
		adminService.activateUser(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/references")
	@PreAuthorize("hasRole('SUPERADMIN')")
	@Operation(
		summary = "Count rows that reference a user",
		description = "Returns a breakdown of how many rows in each table reference the user. " +
			"Use this before DELETE /api/users/{id} to see what would break."
	)
	public ResponseEntity<UserReferencesResponse> getUserReferences(@PathVariable UUID id) {
		return ResponseEntity.ok(adminService.countUserReferences(id));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('SUPERADMIN')")
	@Operation(
		summary = "Hard-delete a user (SUPERADMIN only)",
		description = "Removes the user row from the database. If the user is still referenced " +
			"anywhere, the call fails with 409. Pass ?force=true to nullify the references " +
			"in dependent rows first. Refuses to delete the last remaining SUPERADMIN."
	)
	public ResponseEntity<Void> hardDeleteUser(
		@PathVariable UUID id,
		@RequestParam(name = "force", defaultValue = "false") boolean force
	) {
		// Last-SUPERADMIN guard. We only need to enforce this when the
		// target user IS a SUPERADMIN, so load the user first.
		UserDTO target = adminService.getUserById(id);
		if (target.role().name().equals("SUPERADMIN") && adminService.countSuperadmins() <= 1) {
			throw new IllegalStateException(
				"Refusing to delete the last SUPERADMIN. Promote another user " +
				"to SUPERADMIN before deleting this one."
			);
		}
		adminService.hardDeleteUser(id, force);
		return ResponseEntity.noContent().build();
	}
}
