package com.visco.backend.services;

import com.visco.backend.models.dtos.EmployeeRequestDto;
import com.visco.backend.models.dtos.EmployeeResponseDto;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.Employee;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for employee operations.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

  private final EmployeeRepository employeeRepository;
  private final CostCenterRepository costCenterRepository;

  /**
   * Retrieves a paginated list of all employees.
   *
   * @param pageable pagination information
   * @return page of employee DTOs
   */
  @Transactional(readOnly = true)
  public Page<EmployeeResponseDto> getAllEmployees(Pageable pageable) {
    return employeeRepository
      .findAllWithFetch(pageable)
      .map(EmployeeResponseDto::fromEntity);
  }

  /**
   * Retrieves employees filtered by cost center.
   *
   * @param pageable     pagination information
   * @param costCenterId the cost center ID
   * @return page of employee DTOs
   */
  @Transactional(readOnly = true)
  public Page<EmployeeResponseDto> getEmployeesByCostCenter(
    Pageable pageable,
    Long costCenterId
  ) {
    return employeeRepository
      .findByCostCenterIdWithFetch(costCenterId, pageable)
      .map(EmployeeResponseDto::fromEntity);
  }

  /**
   * Retrieves an employee by their document number.
   *
   * @param document the employee document number
   * @return the employee DTO
   */
  @Transactional(readOnly = true)
  public EmployeeResponseDto getEmployeeByDocument(String document) {
    return employeeRepository
      .findByDocumentNumber(document)
      .map(EmployeeResponseDto::fromEntity)
      .orElseThrow(() ->
        new EntityNotFoundException("Employee not found: " + document)
      );
  }

  /**
   * Creates a new employee with optional cost center assignment.
   *
   * @param request the employee creation request
   * @return the created employee DTO
   */
  @Transactional
  public EmployeeResponseDto createEmployee(EmployeeRequestDto request) {
    Employee.EmployeeBuilder builder = Employee.builder()
      .fullName(request.fullName())
      .documentNumber(request.documentNumber());

    if (request.costCenterId() != null) {
      CostCenter cc = costCenterRepository
        .findById(request.costCenterId())
        .orElseThrow(() ->
          new EntityNotFoundException(
            "Cost center not found: " + request.costCenterId()
          )
        );
      builder.costCenter(cc);
    }

    if (request.isActive() != null) {
      builder.active(request.isActive());
    }

    return EmployeeResponseDto.fromEntity(
      employeeRepository.save(builder.build())
    );
  }

  /**
   * Updates an existing employee's details and cost center.
   *
   * @param document the employee document number
   * @param request  the update request
   * @return the updated employee DTO
   */
  @Transactional
  public EmployeeResponseDto updateEmployee(
    String document,
    EmployeeRequestDto request
  ) {
    Employee employee = employeeRepository
      .findByDocumentNumber(document)
      .orElseThrow(() ->
        new EntityNotFoundException("Employee not found: " + document)
      );

    employee.setFullName(request.fullName());
    employee.setDocumentNumber(request.documentNumber());

    if (request.costCenterId() != null) {
      CostCenter cc = costCenterRepository
        .findById(request.costCenterId())
        .orElseThrow(() ->
          new EntityNotFoundException(
            "Cost center not found: " + request.costCenterId()
          )
        );
      employee.setCostCenter(cc);
    } else {
      employee.setCostCenter(null);
    }

    if (request.isActive() != null) {
      employee.setActive(request.isActive());
    }

    return EmployeeResponseDto.fromEntity(employeeRepository.save(employee));
  }

  /**
   * Deactivates an employee by their document number.
   *
   * @param document the employee document number
   */
  @Transactional
  public void deactivateEmployee(String document) {
    Employee employee = employeeRepository
      .findByDocumentNumber(document)
      .orElseThrow(() ->
        new EntityNotFoundException("Employee not found: " + document)
      );
    employee.setActive(false);
    employeeRepository.save(employee);
  }

  /**
   * Activates a previously deactivated employee.
   *
   * @param document the employee document number
   */
  @Transactional
  public void activateEmployee(String document) {
    Employee employee = employeeRepository
      .findByDocumentNumber(document)
      .orElseThrow(() ->
        new EntityNotFoundException("Employee not found: " + document)
      );
    employee.setActive(true);
    employeeRepository.save(employee);
  }
}
