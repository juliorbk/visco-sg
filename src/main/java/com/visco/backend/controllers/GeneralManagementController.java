package com.visco.backend.controllers;

import com.visco.backend.models.dtos.GeneralManagementSimpleDto;
import com.visco.backend.services.GeneralManagementService;
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
@RequestMapping("/api/general-management")
@Tag(name = "General Management", description = "General management (gerencia general) endpoints")
@RequiredArgsConstructor
public class GeneralManagementController {

  private final GeneralManagementService generalManagementService;

  @GetMapping
  @Operation(summary = "List all general managements", description = "Returns all gerencias generales")
  public ResponseEntity<List<GeneralManagementSimpleDto>> getAll() {
    return ResponseEntity.ok(generalManagementService.getAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get general management by ID")
  public ResponseEntity<GeneralManagementSimpleDto> getById(@PathVariable Long id) {
    return ResponseEntity.ok(generalManagementService.getById(id));
  }
}
