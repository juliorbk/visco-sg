package com.visco.backend.services;

import com.visco.backend.models.dtos.GeneralManagementSimpleDto;
import com.visco.backend.repositories.GeneralManagementRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for general management (high-level administrative
 * divisions) operations.
 */
@Service
@RequiredArgsConstructor
public class GeneralManagementService {

  private final GeneralManagementRepository generalManagementRepository;

  /**
   * Retrieves all general management entries ordered by description.
   *
   * @return list of general management DTOs
   */
  @Transactional(readOnly = true)
  public List<GeneralManagementSimpleDto> getAll() {
    return generalManagementRepository
      .findAllByOrderByDescriptionAsc()
      .stream()
      .map(gm -> new GeneralManagementSimpleDto(gm.getId(), gm.getCode(), gm.getDescription()))
      .toList();
  }

  /**
   * Retrieves a general management entry by its ID.
   *
   * @param id the general management ID
   * @return the general management DTO
   */
  @Transactional(readOnly = true)
  public GeneralManagementSimpleDto getById(Long id) {
    return generalManagementRepository
      .findById(id)
      .map(gm -> new GeneralManagementSimpleDto(gm.getId(), gm.getCode(), gm.getDescription()))
      .orElseThrow(() -> new EntityNotFoundException("General management not found: " + id));
  }
}
