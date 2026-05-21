package com.visco.backend.controllers;

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

import com.visco.backend.models.entities.Category;
import com.visco.backend.repositories.CategoryRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category management endpoints")
public class CategoryController {

	private final CategoryRepository categoryRepository;

	@GetMapping
	@Operation(summary = "List all categories", description = "Returns a paginated list of all product categories")
	public ResponseEntity<Page<Category>> getAllCategories(Pageable pageable) {
		return ResponseEntity.ok(categoryRepository.findAll(pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get category by ID", description = "Returns a specific product category")
	public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
		return ResponseEntity.ok(category);
	}

	@PostMapping
	@Operation(summary = "Create category", description = "Creates a new product category")
	public ResponseEntity<Category> createCategory(@Valid @RequestBody Category category) {
		if (categoryRepository.findByName(category.getName()).isPresent()) {
			throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists");
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(category));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update category", description = "Updates a product category")
	public ResponseEntity<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody Category updated) {
		Category existing = categoryRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
		if (!existing.getName().equals(updated.getName())
				&& categoryRepository.findByName(updated.getName()).isPresent()) {
			throw new IllegalArgumentException("Category with name '" + updated.getName() + "' already exists");
		}
		existing.setName(updated.getName());
		existing.setParentCategory(updated.getParentCategory());
		return ResponseEntity.ok(categoryRepository.save(existing));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete category", description = "Deletes a product category")
	public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
		categoryRepository.delete(category);
		return ResponseEntity.noContent().build();
	}
}
