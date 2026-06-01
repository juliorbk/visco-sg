package com.visco.backend.repositories;

import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryMovementRepository
  extends JpaRepository<InventoryMovement, Long>
{
  List<InventoryMovement> findByProductIdOrderByCreatedAtAsc(Long productId, Pageable pageable);

  List<InventoryMovement> findByProductIdAndCreatedAtBetweenOrderByCreatedAtAsc(
    Long productId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Pageable pageable
  );

  List<InventoryMovement> findByProductIdAndTypeOrderByCreatedAtAsc(
    Long productId,
    MovementType type,
    Pageable pageable
  );

  @Query(
    """
    SELECT m FROM InventoryMovement m
    JOIN FETCH m.product
    LEFT JOIN FETCH m.fromWarehouse
    LEFT JOIN FETCH m.toWarehouse
    LEFT JOIN FETCH m.createdBy
    WHERE (:productId IS NULL OR m.product.id = :productId)
      AND (:warehouseId IS NULL OR m.fromWarehouse.id = :warehouseId OR m.toWarehouse.id = :warehouseId)
      AND (:type IS NULL OR m.type = :type)
      AND (CAST(:startDate AS timestamp) IS NULL OR m.createdAt >= :startDate)
      AND (CAST(:endDate AS timestamp) IS NULL OR m.createdAt <= :endDate)
    ORDER BY m.createdAt ASC
    """
  )
  Page<InventoryMovement> findMovementsWithFilters(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId,
    @Param("type") MovementType type,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate,
    Pageable pageable
  );

  @Query(
    """
        SELECT COALESCE(SUM(
          CASE
            WHEN m.type = 'OUTPUT' THEN -m.quantity
            WHEN m.type = 'TRANSFER' THEN 0
            ELSE m.quantity
          END
        ), 0)
        FROM InventoryMovement m
        WHERE m.product.id = :productId
          AND m.createdAt <= :untilDate
    """
  )
  BigDecimal calculateRunningBalanceUntil(
    @Param("productId") Long productId,
    @Param("untilDate") LocalDateTime untilDate
  );
}
