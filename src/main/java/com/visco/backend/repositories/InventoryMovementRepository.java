package com.visco.backend.repositories;

import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
// Repository for inventory movements with filtered queries and streaming support.
public interface InventoryMovementRepository
  extends
    JpaRepository<InventoryMovement, Long>,
    JpaSpecificationExecutor<InventoryMovement> {
  // Finds movements for a product ordered by date, for balance calculation.
  List<InventoryMovement> findByProductIdOrderByCreatedAtAsc(Long productId, Pageable pageable);

  // Finds movements for a product within a date range, for period-based queries.
  List<InventoryMovement> findByProductIdAndCreatedAtBetweenOrderByCreatedAtAsc(
    Long productId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Pageable pageable
  );

  // Finds movements for a product filtered by movement type.
  List<InventoryMovement> findByProductIdAndTypeOrderByCreatedAtAsc(
    Long productId,
    MovementType type,
    Pageable pageable
  );

  // Calculates the cumulative balance for a product up to a given date.
    @Query(
      """
          SELECT COALESCE(SUM(
            CASE
              WHEN m.type = 'OUTPUT' OR m.type = 'DISPATCH' THEN -m.quantity
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
