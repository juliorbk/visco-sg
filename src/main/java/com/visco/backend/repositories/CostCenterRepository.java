package com.visco.backend.repositories;

import com.visco.backend.models.entities.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {}
