package com.visco.backend.repositories;

import com.visco.backend.models.entities.SupplierCategory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierCategoryRepository extends JpaRepository<SupplierCategory, Long> {

    boolean existsByName(String name);

    Optional<SupplierCategory> findByName(String name);

    Page<SupplierCategory> findByActiveTrue(Pageable pageable);

    Page<SupplierCategory> findByActiveFalse(Pageable pageable);
}
