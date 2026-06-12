package com.visco.backend.repositories;

import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.StockLevel;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecification {

  private ProductSpecification() {}

  public static Specification<Product> search(String search) {
    return (root, query, cb) -> {
      if (search == null || search.isBlank()) {
        return cb.conjunction();
      }
      String pattern = "%" + search.toLowerCase() + "%";
      return cb.or(
        cb.like(cb.lower(root.get("name")), pattern),
        cb.like(cb.lower(root.get("sku")), pattern),
        cb.like(cb.lower(root.get("internalCode")), pattern)
      );
    };
  }

  public static Specification<Product> hasCategory(Long categoryId) {
    return (root, query, cb) -> {
      if (categoryId == null) {
        return cb.conjunction();
      }
      return cb.equal(root.get("category").get("id"), categoryId);
    };
  }

  public static Specification<Product> hasStock() {
    return (root, query, cb) -> {
      Subquery<Long> subquery = query.subquery(Long.class);
      jakarta.persistence.criteria.Root<StockLevel> sl = subquery.from(
        StockLevel.class
      );
      subquery
        .select(sl.get("id"))
        .where(
          cb.equal(sl.get("product").get("id"), root.get("id")),
          cb.greaterThan(sl.get("currentStock"), 0)
        );
      return cb.exists(subquery);
    };
  }
}
