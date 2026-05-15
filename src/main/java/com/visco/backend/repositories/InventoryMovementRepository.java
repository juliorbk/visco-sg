package com.visco.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.visco.backend.models.entities.InventoryMovement;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
}
