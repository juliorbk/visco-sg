package com.visco.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.visco.backend.models.entities.RequestingArea;

@Repository
public interface RequestingAreaRepository
		extends JpaRepository<RequestingArea, Long> {
	Optional<RequestingArea> findByName(String name);

	Optional<RequestingArea> findByCostCenter(String costCenter);
}
