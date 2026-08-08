package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CreateLocationRequest;
import com.visco.backend.models.dtos.LocationDTO;
import com.visco.backend.models.dtos.UpdateLocationRequest;
import com.visco.backend.models.dtos.WarehouseMapResponse;
import com.visco.backend.services.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouse/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Warehouse location management endpoints")
public class LocationController {

  private final LocationService locationService;

  @GetMapping("/warehouse/{warehouseId}")
  @Operation(summary = "List locations by warehouse", description = "Returns a paginated list of locations for a specific warehouse with optional search")
  public ResponseEntity<Page<LocationDTO>> getLocationsByWarehouse(
    @PathVariable Long warehouseId,
    @RequestParam(required = false) String search,
    Pageable pageable
  ) {
    return ResponseEntity.ok(locationService.getLocationsByWarehouseWithSearch(warehouseId, search, pageable));
  }

  @GetMapping("/warehouse/{warehouseId}/active")
  @Operation(summary = "List active locations by warehouse", description = "Returns all active locations for a specific warehouse")
  public ResponseEntity<List<LocationDTO>> getActiveLocationsByWarehouse(
    @PathVariable Long warehouseId
  ) {
    return ResponseEntity.ok(locationService.getActiveLocationsByWarehouse(warehouseId));
  }

  @GetMapping("/warehouse/{warehouseId}/map")
  @Operation(
    summary = "Get warehouse inventory map",
    description = "Returns the full inventory map for a warehouse: all locations (active and inactive) with their physical layout fields (zone, aisle, rack, level, position) so a 2D map can be rendered."
  )
  public ResponseEntity<WarehouseMapResponse> getWarehouseMap(
    @PathVariable Long warehouseId
  ) {
    return ResponseEntity.ok(locationService.getWarehouseMap(warehouseId));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get location by ID", description = "Returns a specific location")
  public ResponseEntity<LocationDTO> getLocation(@PathVariable Long id) {
    return ResponseEntity.ok(locationService.getLocationById(id));
  }

  @PostMapping
  @Operation(summary = "Create location", description = "Creates a new location within a warehouse")
  public ResponseEntity<LocationDTO> createLocation(
    @Valid @RequestBody CreateLocationRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      locationService.createLocation(request)
    );
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update location", description = "Updates location details")
  public ResponseEntity<LocationDTO> updateLocation(
    @PathVariable Long id,
    @Valid @RequestBody UpdateLocationRequest request
  ) {
    return ResponseEntity.ok(locationService.updateLocation(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete location", description = "Soft deletes a location by deactivating it")
  public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
    locationService.deleteLocation(id);
    return ResponseEntity.noContent().build();
  }
}
