package com.visco.backend.models.dtos;

// Single location rendered on a warehouse inventory map.
public record LocationNode(
  Long id,
  String code,
  boolean active,
  String zone,
  String aisle,
  String rack,
  String level,
  Integer positionX,
  Integer positionY,
  String description
) {}