package com.visco.backend.repositories;

import com.visco.backend.models.entities.CostCenter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {
    @Query("SELECT cc FROM CostCenter cc LEFT JOIN FETCH cc.management mgmt LEFT JOIN FETCH mgmt.generalManagement ORDER BY cc.fullDescription ASC")
    Page<CostCenter> findAllWithFetch(Pageable pageable);

    // Sin paginar — para dropdowns
    @Query("SELECT cc FROM CostCenter cc LEFT JOIN FETCH cc.management mgmt LEFT JOIN FETCH mgmt.generalManagement ORDER BY cc.fullDescription ASC")
    List<CostCenter> findAllByOrderByFullDescriptionAsc();
}
