package com.visco.backend.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.visco.backend.models.entities.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByInternalCode(String internalCode);

    Optional<Product> findBySku(String sku);

    Optional<Product> findBySapCode(String sapCode);

    // Repository method automatically supports pagination
    Page<Product> findAll(Pageable pageable);

}
