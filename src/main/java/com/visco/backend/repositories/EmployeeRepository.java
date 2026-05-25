package com.visco.backend.repositories;

import com.visco.backend.models.entities.Employee;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
  Page<Employee> findAllByOrderByFullNameAsc(Pageable pageable);

  Optional<Employee> findByDocumentNumber(String documentNumber);
}
