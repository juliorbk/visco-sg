package com.visco.backend.repositories;

import com.visco.backend.models.entities.CostCenter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {
    // Paginado — ordenado por descripción completa A→Z
    Page<CostCenter> findAllByOrderByFullDescriptionAsc(Pageable pageable);

    // Sin paginar — para dropdowns
    List<CostCenter> findAllByOrderByFullDescriptionAsc();
}
