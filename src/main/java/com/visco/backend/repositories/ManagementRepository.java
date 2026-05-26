package com.visco.backend.repositories;

import com.visco.backend.models.entities.Management;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagementRepository extends JpaRepository<Management, Long> {
    List<Management> findAllByOrderByDescriptionAsc();
}
