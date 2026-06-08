package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.SupplierCategory;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
// Response DTO for a supplier category with metadata.
public class SupplierCategoryDTO {

    private Long id;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupplierCategoryDTO fromCategory(SupplierCategory category) {
        if (category == null) return null;
        return SupplierCategoryDTO.builder()
            .id(category.getId())
            .name(category.getName())
            .description(category.getDescription())
            .active(Boolean.TRUE.equals(category.getActive()))
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
}
