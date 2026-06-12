package com.visco.backend.repositories;

import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.StockLevel;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecification {

  private ProductSpecification() {}

  public static Specification<Product> byNameStartingWith(String name) {
    return (root, query, cb) -> {
      if (name == null || name.isBlank()) {
        return cb.conjunction();
      }
      Expression<String> unaccented = cb.function(
        "unaccent",
        String.class,
        root.get("name")
      );
      return cb.like(cb.lower(unaccented), name.toLowerCase() + "%");
    };
  }

  public static Specification<Product> bySapCode(String sapCode) {
    return (root, query, cb) -> {
      if (sapCode == null || sapCode.isBlank()) {
        return cb.conjunction();
      }
      return cb.equal(root.get("sapCode"), sapCode);
    };
  }

  public static Specification<Product> bySku(String sku) {
    return (root, query, cb) -> {
      if (sku == null || sku.isBlank()) {
        return cb.conjunction();
      }
      return cb.equal(root.get("sku"), sku);
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

  /**
   * Free-text search across name, sapCode and sku (accent-insensitive,
   * partial match). Used by report filtering where the user types a
   * generic term and the system matches it in any of the three fields.
   * For the main product listing endpoint prefer the specific
   * {@link #byNameStartingWith}, {@link #bySapCode} and
   * {@link #bySku} methods, which target a single indexed column.
   */
  public static Specification<Product> freeSearchAcrossFields(String term) {
    return (root, query, cb) -> {
      if (term == null || term.isBlank()) {
        return cb.conjunction();
      }
      String pattern = "%" + term.toLowerCase() + "%";
      Expression<String> nameUnaccented = cb.function(
        "unaccent",
        String.class,
        root.get("name")
      );
      Expression<String> sapUnaccented = cb.function(
        "unaccent",
        String.class,
        root.get("sapCode")
      );
      Expression<String> skuUnaccented = cb.function(
        "unaccent",
        String.class,
        root.get("sku")
      );
      return cb.or(
        cb.like(cb.lower(nameUnaccented), pattern),
        cb.like(cb.lower(sapUnaccented), pattern),
        cb.like(cb.lower(skuUnaccented), pattern)
      );
    };
  }
}

