package com.visco.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByProductIdOrderByCreatedAtAsc(Long productId);

    List<InventoryMovement> findByProductIdAndCreatedAtBetweenOrderByCreatedAtAsc(
        Long productId, LocalDateTime startDate, LocalDateTime endDate
    );

    List<InventoryMovement> findByProductIdAndTypeOrderByCreatedAtAsc(
        Long productId, MovementType type
    );

    @Query("""
        SELECT m FROM InventoryMovement m
        WHERE (:productId IS NULL OR m.product.id = :productId)
          AND (:locationId IS NULL OR m.fromLocation.id = :locationId OR m.toLocation.id = :locationId)
          AND (:type IS NULL OR m.type = :type)
          AND (:startDate IS NULL OR m.createdAt >= :startDate)
          AND (:endDate IS NULL OR m.createdAt <= :endDate)
        ORDER BY m.createdAt ASC
        """)
    List<InventoryMovement> findMovementsWithFilters(
        @Param("productId") Long productId,
        @Param("locationId") Long locationId,
        @Param("type") MovementType type,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT COALESCE(SUM(m.quantity), 0)
        FROM InventoryMovement m
        WHERE m.product.id = :productId
          AND m.createdAt <= :untilDate
    """)
    BigDecimal calculateRunningBalanceUntil(
        @Param("productId") Long productId,
        @Param("untilDate") LocalDateTime untilDate
    );
}
