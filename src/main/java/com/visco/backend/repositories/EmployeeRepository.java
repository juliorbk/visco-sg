package com.visco.backend.repositories;

import com.visco.backend.models.entities.Employee;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
  @Query(
    "SELECT e FROM Employee e LEFT JOIN FETCH e.costCenter ORDER BY e.fullName ASC"
  )
  Page<Employee> findAllWithFetch(Pageable pageable);

  Optional<Employee> findByDocumentNumber(String documentNumber);

  @Query(
    "SELECT e FROM Employee e LEFT JOIN FETCH e.costCenter WHERE e.costCenter.id = :costCenterId"
  )
  Page<Employee> findByCostCenterIdWithFetch(
    @Param("costCenterId") Long costCenterId,
    Pageable pageable
  );
}
