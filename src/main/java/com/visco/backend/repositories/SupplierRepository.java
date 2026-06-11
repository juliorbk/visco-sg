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
// Repository for supplier management with search, filtering, and order-count projections.
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
	// Finds all suppliers with a count query for pagination.
	@Query(value = "SELECT s FROM Supplier s",
		   countQuery = "SELECT COUNT(s) FROM Supplier s")
	Page<Supplier> findAllWithFetch(Pageable pageable);

	@Query(
		value = """
		SELECT s FROM Supplier s
		WHERE (CAST(:search AS text) IS NULL
			OR LOWER(s.name) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
			OR LOWER(s.email) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
		""",
		countQuery = """
		SELECT COUNT(s) FROM Supplier s
		WHERE (CAST(:search AS text) IS NULL
			OR LOWER(s.name) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
			OR LOWER(s.email) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
		"""
	)
	Page<Supplier> findAllWithSearch(@Param("search") String search, Pageable pageable);

	// Finds active suppliers with a count query.
	@Query(value = "SELECT s FROM Supplier s WHERE s.active = true",
		   countQuery = "SELECT COUNT(s) FROM Supplier s WHERE s.active = true")
	Page<Supplier> findByActiveTrueWithFetch(Pageable pageable);

	// Finds inactive suppliers with a count query.
	@Query(value = "SELECT s FROM Supplier s WHERE s.active = false",
		   countQuery = "SELECT COUNT(s) FROM Supplier s WHERE s.active = false")
	Page<Supplier> findByActiveFalseWithFetch(Pageable pageable);

	// Finds suppliers by their currency.
	Page<Supplier> findByCurrency(Currency currency, Pageable pageable);

	// Finds suppliers by currency (collections loaded lazily via batch fetch).
	@Query(value = "SELECT s FROM Supplier s WHERE s.currency = :currency",
		   countQuery = "SELECT COUNT(s) FROM Supplier s WHERE s.currency = :currency")
	Page<Supplier> findByCurrencyWithFetch(@Param("currency") Currency currency, Pageable pageable);

	// Finds suppliers by category (collections loaded lazily via batch fetch).
	@Query(value = "SELECT s FROM Supplier s WHERE s.category.id = :categoryId",
		   countQuery = "SELECT COUNT(s) FROM Supplier s WHERE s.category.id = :categoryId")
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

	// Checks whether a supplier with the given name already exists.
	Boolean existsByName(String name);
}
