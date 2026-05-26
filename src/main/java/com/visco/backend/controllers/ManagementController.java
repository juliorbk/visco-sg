package com.visco.backend.controllers;

import com.visco.backend.models.dtos.ManagementDTO;
import com.visco.backend.services.ManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management")
@Tag(name = "Management", description = "Gerencia management endpoints")
@RequiredArgsConstructor
public class ManagementController {

  private final ManagementService managementService;

  @GetMapping
  @Operation(summary = "List all managements", description = "Returns all gerencias")
  public ResponseEntity<List<ManagementDTO>> getAll() {
    return ResponseEntity.ok(managementService.getAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get management by ID")
  public ResponseEntity<ManagementDTO> getById(@PathVariable Long id) {
    return ResponseEntity.ok(managementService.getById(id));
  }
}
