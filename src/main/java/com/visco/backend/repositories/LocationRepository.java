package com.visco.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.visco.backend.models.entities.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
	Optional<Location> findFirstByWarehouseId(Long warehouseId);
}
