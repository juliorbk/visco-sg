package com.visco.backend.repositories;

import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.MovementType;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class InventoryMovementSpecification {

  private InventoryMovementSpecification() {}

  public static Specification<InventoryMovement> hasProductId(Long productId) {
    return (root, query, cb) -> {
      if (productId == null) {
        return cb.conjunction();
      }
      return cb.equal(root.get("product").get("id"), productId);
    };
  }

  public static Specification<InventoryMovement> touchesWarehouse(Long warehouseId) {
    return (root, query, cb) -> {
      if (warehouseId == null) {
        return cb.conjunction();
      }
      return cb.or(
        cb.equal(root.get("fromWarehouse").get("id"), warehouseId),
        cb.equal(root.get("toWarehouse").get("id"), warehouseId)
      );
    };
  }

  public static Specification<InventoryMovement> hasType(MovementType type) {
    return (root, query, cb) -> {
      if (type == null) {
        return cb.conjunction();
      }
      return cb.equal(root.get("type"), type);
    };
  }

  public static Specification<InventoryMovement> createdAtBetween(
    LocalDateTime startDate,
    LocalDateTime endDate
  ) {
    return (root, query, cb) -> {
      if (startDate == null && endDate == null) {
        return cb.conjunction();
      }
      if (startDate != null && endDate != null) {
        return cb.between(root.get("createdAt"), startDate, endDate);
      }
      if (startDate != null) {
        return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
      }
      return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
    };
  }

  public static Specification<InventoryMovement> withAssociations() {
    return (root, query, cb) -> {
      if (
        query != null &&
        Long.class != query.getResultType() &&
        long.class != query.getResultType()
      ) {
        root.fetch("product", JoinType.INNER);
        root.fetch("fromWarehouse", JoinType.LEFT);
        root.fetch("toWarehouse", JoinType.LEFT);
        root.fetch("createdBy", JoinType.LEFT);
        query.distinct(true);
      }
      return cb.conjunction();
    };
  }
}
