package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateLocationRequest;
import com.visco.backend.models.dtos.LocationDTO;
import com.visco.backend.models.dtos.LocationNode;
import com.visco.backend.models.dtos.UpdateLocationRequest;
import com.visco.backend.models.dtos.WarehouseMapResponse;
import com.visco.backend.models.entities.Location;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.LocationRepository;
import com.visco.backend.repositories.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for warehouse location operations.
 */
@Service
@RequiredArgsConstructor
public class LocationService {

  private final LocationRepository locationRepository;
  private final WarehouseRepository warehouseRepository;

  /**
   * Retrieves paginated locations for a specific warehouse.
   *
   * @param warehouseId the warehouse ID
   * @param pageable    pagination information
   * @return page of location DTOs
   */
  @Transactional(readOnly = true)
  public Page<LocationDTO> getLocationsByWarehouse(
    Long warehouseId,
    Pageable pageable
  ) {
    return locationRepository
      .findByWarehouseId(warehouseId, pageable)
      .map(LocationDTO::fromEntity);
  }

  /**
   * Retrieves paginated locations for a warehouse filtered by search term.
   *
   * @param warehouseId the warehouse ID
   * @param search      the search string
   * @param pageable    pagination information
   * @return page of location DTOs
   */
  @Transactional(readOnly = true)
  public Page<LocationDTO> getLocationsByWarehouseWithSearch(
    Long warehouseId,
    String search,
    Pageable pageable
  ) {
    return locationRepository
      .findByWarehouseIdWithSearch(warehouseId, search, pageable)
      .map(LocationDTO::fromEntity);
  }

  /**
   * Retrieves all active locations for a warehouse.
   *
   * @param warehouseId the warehouse ID
   * @return list of active location DTOs
   */
  @Transactional(readOnly = true)
  public List<LocationDTO> getActiveLocationsByWarehouse(Long warehouseId) {
    return locationRepository
      .findByWarehouseIdAndActiveTrue(warehouseId)
      .stream()
      .map(LocationDTO::fromEntity)
      .toList();
  }

  /**
   * Retrieves a location by its ID.
   *
   * @param id the location ID
   * @return the location DTO
   */
  @Transactional(readOnly = true)
  public LocationDTO getLocationById(Long id) {
    Location location = locationRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Location not found: " + id)
      );
    return LocationDTO.fromEntity(location);
  }

  /**
   * Creates a new location in a warehouse with a unique code.
   *
   * @param request the location creation request
   * @return the created location DTO
   */
  @Transactional
  public LocationDTO createLocation(CreateLocationRequest request) {
    Warehouse warehouse = warehouseRepository
      .findById(request.warehouseId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Warehouse not found: " + request.warehouseId()
        )
      );

    if (
      locationRepository.existsByWarehouseIdAndCode(
        request.warehouseId(),
        request.code()
      )
    ) {
      throw new IllegalArgumentException(
        "Location with code '" +
          request.code() +
          "' already exists in this warehouse"
      );
    }

    Location location = Location.builder()
      .code(request.code())
      .warehouse(warehouse)
      .active(true)
      .zone(request.zone())
      .aisle(request.aisle())
      .rack(request.rack())
      .level(request.level())
      .positionX(request.positionX())
      .positionY(request.positionY())
      .description(request.description())
      .build();

    return LocationDTO.fromEntity(locationRepository.save(location));
  }

  /**
   * Updates a location's active status.
   *
   * @param id      the location ID
   * @param request the update request
   * @return the updated location DTO
   */
  @Transactional
  public LocationDTO updateLocation(Long id, UpdateLocationRequest request) {
    Location location = locationRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Location not found: " + id)
      );

    if (request.active() != null) {
      location.setActive(request.active());
    }
    if (request.zone() != null) {
      location.setZone(request.zone());
    }
    if (request.aisle() != null) {
      location.setAisle(request.aisle());
    }
    if (request.rack() != null) {
      location.setRack(request.rack());
    }
    if (request.level() != null) {
      location.setLevel(request.level());
    }
    if (request.positionX() != null) {
      location.setPositionX(request.positionX());
    }
    if (request.positionY() != null) {
      location.setPositionY(request.positionY());
    }
    if (request.description() != null) {
      location.setDescription(request.description());
    }

    return LocationDTO.fromEntity(locationRepository.save(location));
  }

  /**
   * Soft-deletes a location by setting it inactive.
   *
   * @param id the location ID
   */
  @Transactional
  public void deleteLocation(Long id) {
    Location location = locationRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Location not found: " + id)
      );
    location.setActive(false);
    locationRepository.save(location);
  }

  /**
   * Builds the full inventory map for a warehouse: returns all locations
   * (active and inactive) with their physical layout fields so the
   * frontend can render a 2D view.
   *
   * @param warehouseId the warehouse ID
   * @return the warehouse map response
   */
  @Transactional(readOnly = true)
  public WarehouseMapResponse getWarehouseMap(Long warehouseId) {
    Warehouse warehouse = warehouseRepository
      .findById(warehouseId)
      .orElseThrow(() ->
        new EntityNotFoundException("Warehouse not found: " + warehouseId)
      );

    List<LocationNode> nodes = locationRepository
      .findAllByWarehouseId(warehouseId)
      .stream()
      .map(l ->
        new LocationNode(
          l.getId(),
          l.getCode(),
          Boolean.TRUE.equals(l.getActive()),
          l.getZone(),
          l.getAisle(),
          l.getRack(),
          l.getLevel(),
          l.getPositionX(),
          l.getPositionY(),
          l.getDescription()
        )
      )
      .toList();

    return new WarehouseMapResponse(
      warehouse.getId(),
      warehouse.getName(),
      nodes
    );
  }
}
