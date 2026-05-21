package com.visco.backend.models.dtos;

public record UpdateLocationRequest(
  String aisle,
  String shelf,
  String bin,
  String description,
  Boolean active
) {}
