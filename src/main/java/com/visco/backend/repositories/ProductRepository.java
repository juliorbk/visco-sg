package com.visco.backend.repositories;

import com.visco.backend.models.entities.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByInternalCode(String internalCode);
}
