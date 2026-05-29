package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateLocationRequest;
import com.visco.backend.models.dtos.LocationDTO;
import com.visco.backend.models.dtos.UpdateLocationRequest;
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

@Service
@RequiredArgsConstructor
public class LocationService {

  private final LocationRepository locationRepository;
  private final WarehouseRepository warehouseRepository;

  @Transactional(readOnly = true)
  public Page<LocationDTO> getLocationsByWarehouse(
    Long warehouseId,
    Pageable pageable
  ) {
    return locationRepository
      .findByWarehouseId(warehouseId, pageable)
      .map(LocationDTO::fromEntity);
  }

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

  @Transactional(readOnly = true)
  public List<LocationDTO> getActiveLocationsByWarehouse(Long warehouseId) {
    return locationRepository
      .findByWarehouseIdAndActiveTrue(warehouseId)
      .stream()
      .map(LocationDTO::fromEntity)
      .toList();
  }

  @Transactional(readOnly = true)
  public LocationDTO getLocationById(Long id) {
    Location location = locationRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Location not found: " + id)
      );
    return LocationDTO.fromEntity(location);
  }

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
      .build();

    return LocationDTO.fromEntity(locationRepository.save(location));
  }

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

    return LocationDTO.fromEntity(locationRepository.save(location));
  }

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
}
