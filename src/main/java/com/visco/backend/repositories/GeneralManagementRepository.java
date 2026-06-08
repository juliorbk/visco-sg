package com.visco.backend.repositories;

import com.visco.backend.models.entities.GeneralManagement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for top-level General Management entities.
public interface GeneralManagementRepository extends JpaRepository<GeneralManagement, Long> {
    // Returns all general managements sorted alphabetically by description.
    List<GeneralManagement> findAllByOrderByDescriptionAsc();
}
