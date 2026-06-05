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

@Service
@Slf4j
@RequiredArgsConstructor
public class SupplierCategoryService {

    private final SupplierCategoryRepository categoryRepository;

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

    @Transactional(readOnly = true)
    public Page<SupplierCategoryDTO> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(SupplierCategoryDTO::fromCategory);
    }

    @Transactional(readOnly = true)
    public Page<SupplierCategoryDTO> getActiveCategories(Pageable pageable) {
        return categoryRepository
            .findByActiveTrue(pageable)
            .map(SupplierCategoryDTO::fromCategory);
    }

    @Transactional(readOnly = true)
    public Page<SupplierCategoryDTO> getInactiveCategories(Pageable pageable) {
        return categoryRepository
            .findByActiveFalse(pageable)
            .map(SupplierCategoryDTO::fromCategory);
    }

    @Transactional(readOnly = true)
    public SupplierCategoryDTO getCategoryById(Long id) {
        return SupplierCategoryDTO.fromCategory(
            categoryRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Supplier category not found: " + id)
            )
        );
    }

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
