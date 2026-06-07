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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

  private final CategoryRepository categoryRepository;

  public Page<CategoryDTO> getAllCategories(Pageable pageable) {
    return categoryRepository.findAll(pageable).map(CategoryDTO::fromEntity);
  }

  public CategoryDTO getCategoryById(Long id) {
    return categoryRepository
      .findById(id)
      .map(CategoryDTO::fromEntity)
      .orElseThrow(() ->
        new EntityNotFoundException("Category not found: " + id)
      );
  }

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

  @Transactional
  public void deleteCategory(Long id) {
    if (!categoryRepository.existsById(id)) {
      throw new EntityNotFoundException("Category not found: " + id);
    }
    categoryRepository.deleteById(id);
  }
}
