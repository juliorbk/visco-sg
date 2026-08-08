package com.visco.backend.models.dtos;

import java.util.List;

// Warehouse inventory map: returns all locations (active and inactive)
// of a warehouse so the frontend can render a 2D layout view.
public record WarehouseMapResponse(
  Long warehouseId,
  String warehouseName,
  List<LocationNode> locations
) {}