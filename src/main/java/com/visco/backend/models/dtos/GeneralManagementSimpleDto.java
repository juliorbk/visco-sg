package com.visco.backend.models.dtos;

// Response DTO with basic general management info.
public record GeneralManagementSimpleDto(
  Long id,
  String code,
  String description
) {}
