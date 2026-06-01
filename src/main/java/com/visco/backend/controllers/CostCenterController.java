package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CostCenterResponseDto;
import com.visco.backend.services.CostCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cost-centers")
@Tag(name = "Cost Centers", description = "Cost center management endpoints")
@RequiredArgsConstructor
public class CostCenterController {

  private final CostCenterService costCenterService;

  @GetMapping
  @Operation(
    summary = "List all cost centers",
    description = "Returns a paginated list of all cost centers"
  )
  public ResponseEntity<Page<CostCenterResponseDto>> getAllCostCenters(
    Pageable pageable
  ) {
    return ResponseEntity.ok(costCenterService.getCostCenters(pageable));
  }

  @GetMapping("/all")
  @Operation(
    summary = "List all cost centers (unpaged)",
    description = "Returns all cost centers without pagination, useful for dropdowns"
  )
  public ResponseEntity<List<CostCenterResponseDto>> getAllUnpaged() {
    return ResponseEntity.ok(costCenterService.getAllCostCenters());
  }

  @GetMapping("/{id}")
  @Operation(
    summary = "Get cost center by ID",
    description = "Returns a specific cost center"
  )
  public ResponseEntity<CostCenterResponseDto> getById(@PathVariable Long id) {
    return ResponseEntity.ok(costCenterService.getCostCenterById(id));
  }
}
