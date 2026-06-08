package com.visco.backend.services;

import com.visco.backend.models.dtos.CategoryDTO;
import com.visco.backend.models.dtos.CreateCategoryRequest;
import com.visco.backend.models.dtos.UpdateCategoryRequest;
import com.visco.backend.models.entities.Category;
import com.visco.backend.repositories.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for product category operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

  private final CategoryRepository categoryRepository;

  /**
   * Retrieves a paginated list of all categories.
   *
   * @param pageable pagination information
   * @return page of category DTOs
   */
  public Page<CategoryDTO> getAllCategories(Pageable pageable) {
    return categoryRepository.findAll(pageable).map(CategoryDTO::fromEntity);
  }

  /**
   * Retrieves a category by its ID.
   *
   * @param id the category ID
   * @return the category DTO
   */
  public CategoryDTO getCategoryById(Long id) {
    return categoryRepository
      .findById(id)
      .map(CategoryDTO::fromEntity)
      .orElseThrow(() ->
        new EntityNotFoundException("Category not found: " + id)
      );
  }

  /**
   * Creates a new category with optional parent category.
   *
   * @param request the category creation request
   * @return the created category DTO
   */
  @Transactional
  public CategoryDTO createCategory(CreateCategoryRequest request) {
    String name = request.name().trim();
    categoryRepository.findByName(name).ifPresent(existing -> {
      throw new IllegalStateException(
        "Category with name '" + name + "' already exists"
      );
    });

    Category category = Category.builder().name(name).build();
    if (request.parentId() != null) {
      Category parent = categoryRepository
        .findById(request.parentId())
        .orElseThrow(() ->
          new EntityNotFoundException(
            "Parent category not found: " + request.parentId()
          )
        );
      category.setParentCategory(parent);
      category.setParentId(parent.getId());
    }

    return CategoryDTO.fromEntity(categoryRepository.save(category));
  }

  /**
   * Updates an existing category's name and parent.
   *
   * @param id      the category ID
   * @param request the update request
   * @return the updated category DTO
   */
  @Transactional
  public CategoryDTO updateCategory(Long id, UpdateCategoryRequest request) {
    Category existing = categoryRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Category not found: " + id)
      );

    String newName = request.name().trim();
    if (!existing.getName().equalsIgnoreCase(newName)) {
      categoryRepository.findByName(newName).ifPresent(duplicate -> {
        throw new IllegalStateException(
          "Category with name '" + newName + "' already exists"
        );
      });
    }
    existing.setName(newName);

    if (request.parentId() != null) {
      Category parent = categoryRepository
        .findById(request.parentId())
        .orElseThrow(() ->
          new EntityNotFoundException(
            "Parent category not found: " + request.parentId()
          )
        );
      existing.setParentCategory(parent);
      existing.setParentId(parent.getId());
    } else {
      existing.setParentCategory(null);
      existing.setParentId(null);
    }

    return CategoryDTO.fromEntity(categoryRepository.save(existing));
  }

  /**
   * Deletes a category by its ID.
   *
   * @param id the category ID
   */
  @Transactional
  public void deleteCategory(Long id) {
    if (!categoryRepository.existsById(id)) {
      throw new EntityNotFoundException("Category not found: " + id);
    }
    categoryRepository.deleteById(id);
  }
}
