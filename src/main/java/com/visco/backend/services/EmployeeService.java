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
      .findAllByOrderByFullNameAsc(pageable)
      .map(EmployeeResponseDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public Page<EmployeeResponseDto> getEmployeesByCostCenter(
    Pageable pageable,
    Long costCenterId
  ) {
    return employeeRepository
      .findByCostCenterId(pageable, costCenterId)
      .map(EmployeeResponseDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public EmployeeResponseDto getEmployeeById(Long id) {
    return employeeRepository
      .findById(id)
      .map(EmployeeResponseDto::fromEntity)
      .orElseThrow(() ->
        new EntityNotFoundException("Employee not found: " + id)
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
    Long id,
    EmployeeRequestDto request
  ) {
    Employee employee = employeeRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Employee not found: " + id)
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
  public void deactivateEmployee(Long id) {
    Employee employee = employeeRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Employee not found: " + id)
      );
    employee.setActive(false);
    employeeRepository.save(employee);
  }

  @Transactional
  public void activateEmployee(Long id) {
    Employee employee = employeeRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Employee not found: " + id)
      );
    employee.setActive(true);
    employeeRepository.save(employee);
  }
}
