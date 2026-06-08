package com.visco.backend.services;

import com.visco.backend.models.dtos.CostCenterResponseDto;
import com.visco.backend.repositories.CostCenterRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for cost center operations.
 */
@Service
@RequiredArgsConstructor
public class CostCenterService {

  private final CostCenterRepository costCenterRepository;

  /**
   * Retrieves a paginated list of cost centers with user count.
   *
   * @param pageable pagination information
   * @return page of cost center DTOs
   */
  @Transactional(readOnly = true)
  public Page<CostCenterResponseDto> getCostCenters(Pageable pageable) {
    return costCenterRepository
      .findAllWithFetch(pageable)
      .map(CostCenterResponseDto::fromEntity);
  }

  /**
   * Retrieves all cost centers ordered by description.
   *
   * @return list of cost center DTOs
   */
  @Transactional(readOnly = true)
  public List<CostCenterResponseDto> getAllCostCenters() {
    return costCenterRepository
      .findAllByOrderByFullDescriptionAsc()
      .stream()
      .map(CostCenterResponseDto::fromEntity)
      .toList();
  }

  /**
   * Retrieves a cost center by its ID.
   *
   * @param id the cost center ID
   * @return the cost center DTO
   */
  @Transactional(readOnly = true)
  public CostCenterResponseDto getCostCenterById(Long id) {
    return costCenterRepository
      .findById(id)
      .map(CostCenterResponseDto::fromEntity)
      .orElseThrow(() ->
        new EntityNotFoundException("Cost center not found: " + id)
      );
  }
}
