package com.visco.backend.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.Supplier;
import org.springframework.data.repository.query.Param;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

  Page<Supplier> findAllWithFetch(Pageable pageable);

  @Query(
    value = """
    SELECT s FROM Supplier s
    WHERE (:search IS NULL
      OR LOWER(s.name) LIKE CONCAT('%', LOWER(:search), '%')
      OR LOWER(s.email) LIKE CONCAT('%', LOWER(:search), '%'))
    """,
    countQuery = """
    SELECT COUNT(s) FROM Supplier s
    WHERE (:search IS NULL
      OR LOWER(s.name) LIKE CONCAT('%', LOWER(:search), '%')
      OR LOWER(s.email) LIKE CONCAT('%', LOWER(:search), '%'))
    """
  )
  Page<Supplier> findAllWithSearch(@Param("search") String search, Pageable pageable);

  Page<Supplier> findByActiveTrueWithFetch(Pageable pageable);

  Page<Supplier> findByActiveFalseWithFetch(Pageable pageable);

  Page<Supplier> findByCurrency(Currency currency, Pageable pageable);

  @Query(
    value = "SELECT s FROM Supplier s WHERE s.currency = :currency",
    countQuery = "SELECT COUNT(s) FROM Supplier s WHERE s.currency = :currency"
  )
  Page<Supplier> findByCurrencyWithFetch(@Param("currency") Currency currency, Pageable pageable);

  @Query(
    value = "SELECT s FROM Supplier s WHERE s.category.id = :categoryId",
    countQuery = "SELECT COUNT(s) FROM Supplier s WHERE s.category.id = :categoryId"
  )
  Page<Supplier> findByCategoryIdWithFetch(@Param("categoryId") Long categoryId, Pageable pageable);

  @Query("SELECT s.id as supplierId, s.name as supplierName, COUNT(o) as orderCount " +
      "FROM PurchaseOrder o JOIN o.supplier s " +
      "GROUP BY s.id, s.name ORDER BY orderCount DESC")
  List<SupplierOrderCountProjection> findSuppliersByOrderCount(Pageable pageable);

  interface SupplierOrderCountProjection {
    Long getSupplierId();
    String getSupplierName();
    Long getOrderCount();
  }

  Boolean existsByName(String name);
}
