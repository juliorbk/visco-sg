package com.visco.backend.repositories;

import com.visco.backend.models.entities.SupplierCategory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// Repository for supplier category lookups and existence checks.
public interface SupplierCategoryRepository extends JpaRepository<SupplierCategory, Long> {

    // Checks whether a category with the given name already exists.
    boolean existsByName(String name);

    // Finds a supplier category by its unique name.
    Optional<SupplierCategory> findByName(String name);

    // Returns all active supplier categories.
    Page<SupplierCategory> findByActiveTrue(Pageable pageable);

    // Returns all inactive supplier categories.
    Page<SupplierCategory> findByActiveFalse(Pageable pageable);
}
