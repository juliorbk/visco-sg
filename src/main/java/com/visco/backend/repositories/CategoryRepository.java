package com.visco.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.visco.backend.models.entities.Category;

// Repository for product category lookups.
public interface CategoryRepository extends JpaRepository<Category, Long> {
	// Finds a category by its unique name.
	Optional<Category> findByName(String name);
}
