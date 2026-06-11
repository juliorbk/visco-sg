package com.visco.backend.repositories;

import com.visco.backend.models.entities.Location;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
// Repository for warehouse locations with code-based lookups.
public interface LocationRepository extends JpaRepository<Location, Long> {
  // Paginated locations for a warehouse with the warehouse eagerly loaded.
  @Query(
    value = "SELECT l FROM Location l JOIN FETCH l.warehouse WHERE l.warehouse.id = :warehouseId",
    countQuery = "SELECT COUNT(l) FROM Location l WHERE l.warehouse.id = :warehouseId"
  )
  Page<Location> findByWarehouseId(
    @Param("warehouseId") Long warehouseId,
    Pageable pageable
  );

  // Returns all active locations for a warehouse (e.g. for dropdowns).
  @Query(
    "SELECT l FROM Location l JOIN FETCH l.warehouse WHERE l.warehouse.id = :warehouseId AND l.active = true"
  )
  List<Location> findByWarehouseIdAndActiveTrue(
    @Param("warehouseId") Long warehouseId
  );

  // Finds a location within a warehouse by its code.
  Optional<Location> findByWarehouseIdAndCode(Long warehouseId, String code);
  // Checks whether a location code already exists within a warehouse.
  boolean existsByWarehouseIdAndCode(Long warehouseId, String code);

  @Query(
    value = "SELECT l FROM Location l JOIN FETCH l.warehouse WHERE l.warehouse.id = :warehouseId" +
      " AND (CAST(:search AS text) IS NULL OR LOWER(l.code) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
    countQuery = "SELECT COUNT(l) FROM Location l WHERE l.warehouse.id = :warehouseId" +
      " AND (CAST(:search AS text) IS NULL OR LOWER(l.code) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))"
  )
  Page<Location> findByWarehouseIdWithSearch(
    @Param("warehouseId") Long warehouseId,
    @Param("search") String search,
    Pageable pageable
  );
}
