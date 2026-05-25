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

@Service
@RequiredArgsConstructor
public class CostCenterService {

  private final CostCenterRepository costCenterRepository;

  @Transactional(readOnly = true)
  public Page<CostCenterResponseDto> getCostCenters(Pageable pageable) {
    return costCenterRepository
      .findAllByOrderByFullDescriptionAsc(pageable)
      .map(CostCenterResponseDto::fromEntity);
  }

  @Transactional(readOnly = true)
  public List<CostCenterResponseDto> getAllCostCenters() {
    return costCenterRepository
      .findAllByOrderByFullDescriptionAsc()
      .stream()
      .map(CostCenterResponseDto::fromEntity)
      .toList();
  }

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
