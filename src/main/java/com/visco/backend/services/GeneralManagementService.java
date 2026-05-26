package com.visco.backend.services;

import com.visco.backend.models.dtos.GeneralManagementSimpleDto;
import com.visco.backend.repositories.GeneralManagementRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GeneralManagementService {

  private final GeneralManagementRepository generalManagementRepository;

  @Transactional(readOnly = true)
  public List<GeneralManagementSimpleDto> getAll() {
    return generalManagementRepository
      .findAllByOrderByDescriptionAsc()
      .stream()
      .map(gm -> new GeneralManagementSimpleDto(gm.getId(), gm.getCode(), gm.getDescription()))
      .toList();
  }

  @Transactional(readOnly = true)
  public GeneralManagementSimpleDto getById(Long id) {
    return generalManagementRepository
      .findById(id)
      .map(gm -> new GeneralManagementSimpleDto(gm.getId(), gm.getCode(), gm.getDescription()))
      .orElseThrow(() -> new EntityNotFoundException("General management not found: " + id));
  }
}
