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

@Service
@RequiredArgsConstructor
public class EmployeeService {

  private final EmployeeRepository employeeRepository;
  private final CostCenterRepository costCenterRepository;

  @Transactional(readOnly = true)
  public Page<EmployeeResponseDto> getAllEmployees(Pageable pageable) {
    return employeeRepository
      .findAllWithFetch(pageable)
      .map(EmployeeResponseDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public Page<EmployeeResponseDto> getEmployeesByCostCenter(
    Pageable pageable,
    Long costCenterId
  ) {
    return employeeRepository
      .findByCostCenterIdWithFetch(costCenterId, pageable)
      .map(EmployeeResponseDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public EmployeeResponseDto getEmployeeByDocument(String document) {
    return employeeRepository
      .findByDocumentNumber(document)
      .map(EmployeeResponseDto::fromEntity)
      .orElseThrow(() ->
        new EntityNotFoundException("Employee not found: " + document)
      );
  }

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
