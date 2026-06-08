package com.visco.backend.models.dtos;

// Request payload for updating a warehouse location.
public record UpdateLocationRequest(
  String aisle,
  String shelf,
  String bin,
  String description,
  Boolean active
) {}
