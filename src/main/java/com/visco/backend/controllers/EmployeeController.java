package com.visco.backend.controllers;

import com.visco.backend.models.dtos.EmployeeRequestDto;
import com.visco.backend.models.dtos.EmployeeResponseDto;
import com.visco.backend.services.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@Tag(
  name = "Employees",
  description = "Employee management endpoints (admin only)"
)
@RequiredArgsConstructor
public class EmployeeController {

  private final EmployeeService employeeService;

  @GetMapping
  @Operation(
    summary = "List all employees",
    description = "Returns a paginated list of all employees"
  )
  public ResponseEntity<Page<EmployeeResponseDto>> getAllEmployees(
    Pageable pageable
  ) {
    return ResponseEntity.ok(employeeService.getAllEmployees(pageable));
  }

  @GetMapping("/{document}")
  @Operation(summary = "Get employee by document")
  public ResponseEntity<EmployeeResponseDto> getEmployeeByDocument(
    @PathVariable String document
  ) {
    return ResponseEntity.ok(employeeService.getEmployeeByDocument(document));
  }

  @PostMapping
  @Operation(summary = "Create employee")
  public ResponseEntity<EmployeeResponseDto> createEmployee(
    @Valid @RequestBody EmployeeRequestDto request
  ) {
    return ResponseEntity.ok(employeeService.createEmployee(request));
  }

  @PutMapping("/{document}")
  @Operation(summary = "Update employee")
  public ResponseEntity<EmployeeResponseDto> updateEmployee(
    @PathVariable String document,
    @Valid @RequestBody EmployeeRequestDto request
  ) {
    return ResponseEntity.ok(employeeService.updateEmployee(document, request));
  }

  @PatchMapping("/{document}/deactivate")
  @Operation(summary = "Deactivate employee")
  public ResponseEntity<Void> deactivateEmployee(@PathVariable String document) {
    employeeService.deactivateEmployee(document);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{document}/activate")
  @Operation(summary = "Activate employee")
  public ResponseEntity<Void> activateEmployee(@PathVariable String document) {
    employeeService.activateEmployee(document);
    return ResponseEntity.noContent().build();
  }
}
