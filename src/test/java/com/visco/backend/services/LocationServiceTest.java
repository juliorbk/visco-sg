package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visco.backend.models.dtos.CreateLocationRequest;
import com.visco.backend.models.dtos.LocationDTO;
import com.visco.backend.models.dtos.UpdateLocationRequest;
import com.visco.backend.models.dtos.WarehouseMapResponse;
import com.visco.backend.models.entities.Location;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.LocationRepository;
import com.visco.backend.repositories.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for LocationService covering structured physical fields
 * (zone/aisle/rack/level/position) and the warehouse inventory map.
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

  @Mock private LocationRepository locationRepository;
  @Mock private WarehouseRepository warehouseRepository;

  @InjectMocks private LocationService locationService;

  private Warehouse warehouse;

  @BeforeEach
  void setUp() {
    warehouse = Warehouse.builder()
      .id(10L)
      .name("Main Warehouse")
      .physicalAddress("Calle 1")
      .description("Main")
      .active(true)
      .build();
  }

  @Test
  void createLocation_persistsStructuredFields() {
    when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
    when(locationRepository.existsByWarehouseIdAndCode(10L, "A-01-03")).thenReturn(false);
    when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));

    CreateLocationRequest request = new CreateLocationRequest(
      "A-01-03",
      "Picking",
      "A",
      "01",
      "03",
      12,
      34,
      "Frente a la entrada",
      10L
    );

    LocationDTO result = locationService.createLocation(request);

    ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
    verify(locationRepository).save(captor.capture());
    Location saved = captor.getValue();

    assertEquals("A-01-03", saved.getCode());
    assertEquals("Picking", saved.getZone());
    assertEquals("A", saved.getAisle());
    assertEquals("01", saved.getRack());
    assertEquals("03", saved.getLevel());
    assertEquals(12, saved.getPositionX());
    assertEquals(34, saved.getPositionY());
    assertEquals("Frente a la entrada", saved.getDescription());
    assertTrue(saved.getActive());

    assertEquals("Picking", result.getZone());
    assertEquals(10L, result.getWarehouseId());
  }

  @Test
  void createLocation_throwsWhenWarehouseMissing() {
    when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

    EntityNotFoundException ex = assertThrows(
      EntityNotFoundException.class,
      () -> locationService.createLocation(new CreateLocationRequest(
        "X", null, null, null, null, null, null, null, 99L
      ))
    );
    assertTrue(ex.getMessage().contains("Warehouse not found"));
    verify(locationRepository, never()).save(any());
  }

  @Test
  void createLocation_throwsWhenCodeAlreadyExists() {
    when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
    when(locationRepository.existsByWarehouseIdAndCode(10L, "DUP")).thenReturn(true);

    IllegalArgumentException ex = assertThrows(
      IllegalArgumentException.class,
      () -> locationService.createLocation(new CreateLocationRequest(
        "DUP", null, null, null, null, null, null, null, 10L
      ))
    );
    assertTrue(ex.getMessage().contains("already exists"));
    verify(locationRepository, never()).save(any());
  }

  @Test
  void updateLocation_updatesOnlyNonNullFields() {
    Location existing = Location.builder()
      .id(1L)
      .code("A-01-03")
      .active(true)
      .warehouse(warehouse)
      .zone("Picking")
      .aisle("A")
      .rack("01")
      .level("03")
      .positionX(12)
      .positionY(34)
      .description("Old desc")
      .build();

    when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateLocationRequest request = new UpdateLocationRequest(
      null,
      "B",
      null,
      null,
      100,
      null,
      null,
      null
    );

    LocationDTO result = locationService.updateLocation(1L, request);

    // unchanged
    assertEquals("Picking", existing.getZone());
    assertEquals("01", existing.getRack());
    assertEquals("03", existing.getLevel());
    assertEquals(34, existing.getPositionY());
    assertEquals("Old desc", existing.getDescription());
    assertTrue(existing.getActive());
    // changed
    assertEquals("B", existing.getAisle());
    assertEquals(100, existing.getPositionX());

    assertEquals("B", result.getAisle());
    assertEquals(100, result.getPositionX());
  }

  @Test
  void updateLocation_togglesActive() {
    Location existing = Location.builder()
      .id(1L)
      .code("A-01-03")
      .active(true)
      .warehouse(warehouse)
      .build();

    when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateLocationRequest request = new UpdateLocationRequest(
      null, null, null, null, null, null, null, false
    );

    LocationDTO result = locationService.updateLocation(1L, request);

    assertEquals(false, existing.getActive());
    assertEquals(false, result.isActive());
  }

  @Test
  void getWarehouseMap_returnsAllLocationsAsNodes() {
    when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));

    Location active = Location.builder()
      .id(1L)
      .code("A-01-03")
      .active(true)
      .warehouse(warehouse)
      .zone("Picking")
      .aisle("A")
      .rack("01")
      .level("03")
      .positionX(12)
      .positionY(34)
      .description("Bin activo")
      .build();

    Location inactive = Location.builder()
      .id(2L)
      .code("B-02-01")
      .active(false)
      .warehouse(warehouse)
      .zone("Reserva")
      .aisle("B")
      .rack("02")
      .level("01")
      .positionX(5)
      .positionY(6)
      .description("Bin inactivo")
      .build();

    when(locationRepository.findAllByWarehouseId(10L)).thenReturn(List.of(active, inactive));

    WarehouseMapResponse map = locationService.getWarehouseMap(10L);

    assertEquals(10L, map.warehouseId());
    assertEquals("Main Warehouse", map.warehouseName());
    assertEquals(2, map.locations().size());

    var nodeActive = map.locations().get(0);
    assertEquals(1L, nodeActive.id());
    assertTrue(nodeActive.active());
    assertEquals("Picking", nodeActive.zone());
    assertEquals(12, nodeActive.positionX());

    var nodeInactive = map.locations().get(1);
    assertEquals(2L, nodeInactive.id());
    assertEquals(false, nodeInactive.active());
    assertEquals("Reserva", nodeInactive.zone());
  }

  @Test
  void getWarehouseMap_throwsWhenWarehouseMissing() {
    when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

    EntityNotFoundException ex = assertThrows(
      EntityNotFoundException.class,
      () -> locationService.getWarehouseMap(99L)
    );
    assertTrue(ex.getMessage().contains("Warehouse not found"));
    verify(locationRepository, never()).findAllByWarehouseId(any());
  }

  @Test
  void getWarehouseMap_returnsEmptyListWhenNoLocations() {
    when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
    when(locationRepository.findAllByWarehouseId(10L)).thenReturn(List.of());

    WarehouseMapResponse map = locationService.getWarehouseMap(10L);

    assertNotNull(map);
    assertEquals(10L, map.warehouseId());
    assertTrue(map.locations().isEmpty());
  }
}