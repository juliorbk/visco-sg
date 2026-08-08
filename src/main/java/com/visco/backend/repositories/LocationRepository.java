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
public interface LocationRepository extends JpaRepository<Location, Long> {
  @Query(
    value = "SELECT l FROM Location l JOIN FETCH l.warehouse WHERE l.warehouse.id = :warehouseId",
    countQuery = "SELECT COUNT(l) FROM Location l WHERE l.warehouse.id = :warehouseId"
  )
  Page<Location> findByWarehouseId(
    @Param("warehouseId") Long warehouseId,
    Pageable pageable
  );

  @Query(
    "SELECT l FROM Location l JOIN FETCH l.warehouse WHERE l.warehouse.id = :warehouseId AND l.active = true"
  )
  List<Location> findByWarehouseIdAndActiveTrue(
    @Param("warehouseId") Long warehouseId
  );

  @Query(
    "SELECT l FROM Location l JOIN FETCH l.warehouse WHERE l.warehouse.id = :warehouseId"
  )
  List<Location> findAllByWarehouseId(@Param("warehouseId") Long warehouseId);

  Optional<Location> findByWarehouseIdAndCode(Long warehouseId, String code);
  boolean existsByWarehouseIdAndCode(Long warehouseId, String code);

  @Query(
    value = """
    SELECT l.* FROM locations l
    JOIN warehouses w ON w.id = l.warehouse_id
    WHERE l.warehouse_id = :warehouseId
      AND (CAST(:search AS text) IS NULL OR l.location_code ILIKE '%' || :search || '%')
    """,
    countQuery = """
    SELECT COUNT(*) FROM locations l
    WHERE l.warehouse_id = :warehouseId
      AND (CAST(:search AS text) IS NULL OR l.location_code ILIKE '%' || :search || '%')
    """,
    nativeQuery = true
  )
  Page<Location> findByWarehouseIdWithSearch(
    @Param("warehouseId") Long warehouseId,
    @Param("search") String search,
    Pageable pageable
  );
}
