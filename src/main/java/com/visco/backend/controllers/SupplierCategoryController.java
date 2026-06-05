package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CreateSupplierCategoryRequest;
import com.visco.backend.models.dtos.SupplierCategoryDTO;
import com.visco.backend.models.dtos.UpdateSupplierCategoryRequest;
import com.visco.backend.services.SupplierCategoryService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supplier-categories")
@RequiredArgsConstructor
@Tag(
    name = "Supplier Categories",
    description = "Supplier categorization management endpoints"
)
public class SupplierCategoryController {

    private final SupplierCategoryService categoryService;

    @PostMapping
    @Operation(
        summary = "Create supplier category",
        description = "Creates a new supplier category"
    )
    public ResponseEntity<SupplierCategoryDTO> createCategory(
        @Valid @RequestBody CreateSupplierCategoryRequest request
    ) {
        SupplierCategoryDTO created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(
        summary = "List all supplier categories",
        description = "Returns a paginated list of all supplier categories"
    )
    public ResponseEntity<Page<SupplierCategoryDTO>> getAllCategories(Pageable pageable) {
        return ResponseEntity.ok(categoryService.getAllCategories(pageable));
    }

    @GetMapping("/active")
    @Operation(
        summary = "List active supplier categories",
        description = "Returns a paginated list of active supplier categories"
    )
    public ResponseEntity<Page<SupplierCategoryDTO>> getActiveCategories(
        Pageable pageable
    ) {
        return ResponseEntity.ok(categoryService.getActiveCategories(pageable));
    }

    @GetMapping("/inactive")
    @Operation(
        summary = "List inactive supplier categories",
        description = "Returns a paginated list of inactive supplier categories"
    )
    public ResponseEntity<Page<SupplierCategoryDTO>> getInactiveCategories(
        Pageable pageable
    ) {
        return ResponseEntity.ok(categoryService.getInactiveCategories(pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get supplier category by ID",
        description = "Returns a specific supplier category"
    )
    public ResponseEntity<SupplierCategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update supplier category",
        description = "Updates an existing supplier category"
    )
    public ResponseEntity<SupplierCategoryDTO> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody UpdateSupplierCategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deactivate supplier category",
        description = "Soft-deactivates a supplier category (preserves linked suppliers)"
    )
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @Operation(
        summary = "Activate supplier category",
        description = "Reactivates a soft-deactivated supplier category"
    )
    public ResponseEntity<Void> activateCategory(@PathVariable Long id) {
        categoryService.activateCategory(id);
        return ResponseEntity.noContent().build();
    }
}
