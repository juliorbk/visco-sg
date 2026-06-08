package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateSupplierCategoryRequest;
import com.visco.backend.models.dtos.SupplierCategoryDTO;
import com.visco.backend.models.dtos.UpdateSupplierCategoryRequest;
import com.visco.backend.models.entities.SupplierCategory;
import com.visco.backend.repositories.SupplierCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for supplier category operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SupplierCategoryService {

    private final SupplierCategoryRepository categoryRepository;

    /**
     * Creates a new supplier category with a unique name.
     *
     * @param request the category creation request
     * @return the created supplier category DTO
     */
    @Transactional
    public SupplierCategoryDTO createCategory(CreateSupplierCategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new IllegalStateException(
                "Supplier category with name: " + name + " already exists"
            );
        }
        SupplierCategory category = SupplierCategory.builder()
            .name(name)
            .description(request.description())
            .active(true)
            .build();
        return SupplierCategoryDTO.fromCategory(categoryRepository.save(category));
    }

    /**
     * Retrieves a paginated list of all supplier categories.
     *
     * @param pageable pagination information
     * @return page of supplier category DTOs
     */
    @Transactional(readOnly = true)
    public Page<SupplierCategoryDTO> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(SupplierCategoryDTO::fromCategory);
    }

    /**
     * Retrieves only active supplier categories.
     *
     * @param pageable pagination information
     * @return page of active supplier category DTOs
     */
    @Transactional(readOnly = true)
    public Page<SupplierCategoryDTO> getActiveCategories(Pageable pageable) {
        return categoryRepository
            .findByActiveTrue(pageable)
            .map(SupplierCategoryDTO::fromCategory);
    }

    /**
     * Retrieves only inactive supplier categories.
     *
     * @param pageable pagination information
     * @return page of inactive supplier category DTOs
     */
    @Transactional(readOnly = true)
    public Page<SupplierCategoryDTO> getInactiveCategories(Pageable pageable) {
        return categoryRepository
            .findByActiveFalse(pageable)
            .map(SupplierCategoryDTO::fromCategory);
    }

    /**
     * Retrieves a supplier category by its ID.
     *
     * @param id the supplier category ID
     * @return the supplier category DTO
     */
    @Transactional(readOnly = true)
    public SupplierCategoryDTO getCategoryById(Long id) {
        return SupplierCategoryDTO.fromCategory(
            categoryRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Supplier category not found: " + id)
            )
        );
    }

    /**
     * Updates an existing supplier category's name, description, and active status.
     *
     * @param id      the supplier category ID
     * @param request the update request
     * @return the updated supplier category DTO
     */
    @Transactional
    public SupplierCategoryDTO updateCategory(Long id, UpdateSupplierCategoryRequest request) {
        SupplierCategory existing = categoryRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Supplier category not found: " + id)
        );

        String newName = request.name().trim();
        if (
            !existing.getName().equalsIgnoreCase(newName) &&
            categoryRepository.existsByName(newName)
        ) {
            throw new IllegalStateException(
                "Supplier category with name: " + newName + " already exists"
            );
        }

        existing.setName(newName);
        if (request.description() != null) {
            existing.setDescription(request.description());
        }
        if (request.active() != null) {
            existing.setActive(request.active());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return SupplierCategoryDTO.fromCategory(categoryRepository.save(existing));
    }

    /**
     * Deactivates an active supplier category.
     *
     * @param id the supplier category ID
     */
    @Transactional
    public void deactivateCategory(Long id) {
        SupplierCategory category = categoryRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Supplier category not found: " + id)
        );
        if (Boolean.FALSE.equals(category.getActive())) {
            throw new IllegalStateException(
                "Supplier category is already inactive: " + id
            );
        }
        category.setActive(false);
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);
        log.info("Deactivated supplier category with id: {}", id);
    }

    /**
     * Activates an inactive supplier category.
     *
     * @param id the supplier category ID
     */
    @Transactional
    public void activateCategory(Long id) {
        SupplierCategory category = categoryRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Supplier category not found: " + id)
        );
        if (Boolean.TRUE.equals(category.getActive())) {
            throw new IllegalStateException(
                "Supplier category is already active: " + id
            );
        }
        category.setActive(true);
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);
        log.info("Activated supplier category with id: {}", id);
    }
}
