package com.visco.backend.repositories;

import com.visco.backend.models.entities.GeneralManagement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneralManagementRepository extends JpaRepository<GeneralManagement, Long> {
    List<GeneralManagement> findAllByOrderByDescriptionAsc();
}
