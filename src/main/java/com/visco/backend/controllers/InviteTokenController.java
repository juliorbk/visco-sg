package com.visco.backend.controllers;

import com.visco.backend.config.UserPrincipal;
import com.visco.backend.models.dtos.CreateInviteRequest;
import com.visco.backend.models.dtos.InviteTokenResponse;
import com.visco.backend.services.InviteTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(
  name = "Invites",
  description = "Invite token administration (ADMIN only)"
)
public class InviteTokenController {

  private final InviteTokenService inviteTokenService;

  @PostMapping
  @Operation(
    summary = "Create an invite token",
    description = "Generates a one-time token bound to an email and an intended role. " +
      "The token must be supplied to POST /api/auth/register."
  )
  public ResponseEntity<InviteTokenResponse> create(
    @Valid @RequestBody CreateInviteRequest request,
    @AuthenticationPrincipal UserPrincipal principal
  ) {
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(inviteTokenService.createInvite(request, principal.getId()));
  }

  @GetMapping
  @Operation(summary = "List all invite tokens")
  public ResponseEntity<List<InviteTokenResponse>> list() {
    return ResponseEntity.ok(inviteTokenService.listInvites());
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Revoke an invite token")
  public ResponseEntity<InviteTokenResponse> revoke(@PathVariable UUID id) {
    return ResponseEntity.ok(inviteTokenService.revokeInvite(id));
  }

  @GetMapping("/by-token/{token}")
  @PreAuthorize("permitAll()")
  @Operation(
    summary = "Resolve an invite token",
    description = "Public endpoint that returns invite details for the registration flow."
  )
  public ResponseEntity<InviteTokenResponse> resolve(@PathVariable String token) {
    return ResponseEntity.ok(inviteTokenService.resolveByToken(token));
  }
}
