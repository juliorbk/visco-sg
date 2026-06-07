package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CategoryDTO;
import com.visco.backend.models.dtos.CreateCategoryRequest;
import com.visco.backend.models.dtos.UpdateCategoryRequest;
import com.visco.backend.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category management endpoints")
public class CategoryController {

  private final CategoryService categoryService;

  @GetMapping
  @Operation(
    summary = "List all categories",
    description = "Returns a paginated list of all product categories"
  )
  public ResponseEntity<Page<CategoryDTO>> getAllCategories(Pageable pageable) {
    return ResponseEntity.ok(categoryService.getAllCategories(pageable));
  }

  @GetMapping("/{id}")
  @Operation(
    summary = "Get category by ID",
    description = "Returns a specific product category"
  )
  public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
    return ResponseEntity.ok(categoryService.getCategoryById(id));
  }

  @PostMapping
  @Operation(
    summary = "Create category",
    description = "Creates a new product category"
  )
  public ResponseEntity<CategoryDTO> createCategory(
    @Valid @RequestBody CreateCategoryRequest request
  ) {
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(categoryService.createCategory(request));
  }

  @PutMapping("/{id}")
  @Operation(
    summary = "Update category",
    description = "Updates a product category"
  )
  public ResponseEntity<CategoryDTO> updateCategory(
    @PathVariable Long id,
    @Valid @RequestBody UpdateCategoryRequest request
  ) {
    return ResponseEntity.ok(categoryService.updateCategory(id, request));
  }

  @DeleteMapping("/{id}")
  @Operation(
    summary = "Delete category",
    description = "Deletes a product category"
  )
  public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.noContent().build();
  }
}
