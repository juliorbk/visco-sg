package com.visco.backend.repositories;

import com.visco.backend.models.entities.Management;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Repository for Management entities with general management eager loading.
public interface ManagementRepository extends JpaRepository<Management, Long> {
    // Returns all managements sorted by description with general management eagerly loaded.
    @Query("SELECT m FROM Management m LEFT JOIN FETCH m.generalManagement ORDER BY m.description ASC")
    List<Management> findAllByOrderByDescriptionAsc();

    // Finds a management by ID with its parent general management eagerly loaded.
    @Query("SELECT m FROM Management m LEFT JOIN FETCH m.generalManagement WHERE m.id = :id")
    Optional<Management> findByIdWithGeneralManagement(Long id);
}
