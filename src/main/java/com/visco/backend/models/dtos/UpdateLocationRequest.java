package com.visco.backend.models.dtos;

// Request payload for updating a warehouse location.
// All fields are optional: only non-null values are applied.
public record UpdateLocationRequest(
  String zone,
  String aisle,
  String rack,
  String level,
  Integer positionX,
  Integer positionY,
  String description,
  Boolean active
) {}