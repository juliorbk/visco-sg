package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Category;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
// Response DTO with product category details.
public class CategoryDTO {

  private Long id;
  private String name;
  private Long parentId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static CategoryDTO fromEntity(Category category) {
    if (category == null) return null;
    Long parentId = category.getParentId() != null
      ? category.getParentId()
      : (category.getParentCategory() != null ? category.getParentCategory().getId() : null);
    return CategoryDTO.builder()
      .id(category.getId())
      .name(category.getName())
      .parentId(parentId)
      .createdAt(category.getCreatedAt())
      .updatedAt(category.getUpdatedAt())
      .build();
  }
}
