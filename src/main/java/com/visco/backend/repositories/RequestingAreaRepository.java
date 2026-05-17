package com.visco.backend.repositories;

import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.RequestingArea;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestingAreaRepository extends JpaRepository<RequestingArea, Long> {
    Optional<RequestingArea> findByName(String name);

    Optional<RequestingArea> findByCostCenter(CostCenter costCenter);
}
