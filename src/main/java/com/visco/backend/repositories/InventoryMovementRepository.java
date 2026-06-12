package com.visco.backend.repositories;

import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.QueryHint;

@Repository
// Repository for inventory movements with filtered queries and streaming support.
public interface InventoryMovementRepository
  extends JpaRepository<InventoryMovement, Long>
{
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

  @Query(
    value = """
    SELECT m FROM InventoryMovement m
    JOIN FETCH m.product
    LEFT JOIN FETCH m.fromWarehouse
    LEFT JOIN FETCH m.toWarehouse
    LEFT JOIN FETCH m.createdBy
    WHERE (:productId IS NULL OR m.product.id = :productId)
      AND (:warehouseId IS NULL OR m.fromWarehouse.id = :warehouseId OR m.toWarehouse.id = :warehouseId)
      AND (:type IS NULL OR m.type = :type)
      AND (:startDate IS NULL OR m.createdAt >= :startDate)
      AND (:endDate IS NULL OR m.createdAt <= :endDate)
    ORDER BY m.createdAt ASC
    """,
    countQuery = """
    SELECT COUNT(m) FROM InventoryMovement m
    WHERE (:productId IS NULL OR m.product.id = :productId)
      AND (:warehouseId IS NULL OR m.fromWarehouse.id = :warehouseId OR m.toWarehouse.id = :warehouseId)
      AND (:type IS NULL OR m.type = :type)
      AND (:startDate IS NULL OR m.createdAt >= :startDate)
      AND (:endDate IS NULL OR m.createdAt <= :endDate)
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

  // Streaming version of findMovementsWithFilters for large export operations.
  @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "-2147483648"))
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
      AND (:startDate IS NULL OR m.createdAt >= :startDate)
      AND (:endDate IS NULL OR m.createdAt <= :endDate)
    ORDER BY m.createdAt ASC
    """
  )
  Stream<InventoryMovement> streamMovementsWithFilters(
    @Param("productId") Long productId,
    @Param("warehouseId") Long warehouseId,
    @Param("type") MovementType type,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
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
