package com.visco.backend.repositories;

import com.visco.backend.models.entities.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
// Repository for warehouse entities with responsible user eager loading.
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
  // Finds all warehouses with their responsible user eagerly loaded.
  @Query("SELECT w FROM Warehouse w JOIN FETCH w.responsibleUser")
  Page<Warehouse> findAllWithFetch(Pageable pageable);
}
