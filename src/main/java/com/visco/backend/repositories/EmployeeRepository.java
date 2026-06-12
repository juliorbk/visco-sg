package com.visco.backend.repositories;

import com.visco.backend.models.entities.Employee;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repository for Employee entities with cost center eager loading.
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
  // Finds all employees with their cost center eagerly loaded, sorted by name.
  @Query(
    value = "SELECT e FROM Employee e LEFT JOIN FETCH e.costCenter ORDER BY e.fullName ASC",
    countQuery = "SELECT COUNT(e) FROM Employee e"
  )
  Page<Employee> findAllWithFetch(Pageable pageable);

  // Finds an employee by their unique document number.
  Optional<Employee> findByDocumentNumber(String documentNumber);

  // Finds employees belonging to a specific cost center with eager loading.
  @Query(
    value = "SELECT e FROM Employee e LEFT JOIN FETCH e.costCenter WHERE e.costCenter.id = :costCenterId",
    countQuery = "SELECT COUNT(e) FROM Employee e WHERE e.costCenter.id = :costCenterId"
  )
  Page<Employee> findByCostCenterIdWithFetch(
    @Param("costCenterId") Long costCenterId,
    Pageable pageable
  );
}
