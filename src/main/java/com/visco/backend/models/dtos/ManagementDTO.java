package com.visco.backend.models.dtos;

// Response DTO with management code and description.
public record ManagementDTO(Long id, String code, String description, Long generalManagementId) {}
