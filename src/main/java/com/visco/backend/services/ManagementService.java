package com.visco.backend.services;

import com.visco.backend.models.dtos.ManagementDTO;
import com.visco.backend.repositories.ManagementRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagementService {

  private final ManagementRepository managementRepository;

  @Transactional(readOnly = true)
  public List<ManagementDTO> getAll() {
    return managementRepository
      .findAllByOrderByDescriptionAsc()
      .stream()
      .map(m -> new ManagementDTO(m.getId(), m.getCode(), m.getDescription(), m.getGeneralManagement().getId()))
      .toList();
  }

  @Transactional(readOnly = true)
  public ManagementDTO getById(Long id) {
    return managementRepository
      .findByIdWithGeneralManagement(id)
      .map(m -> new ManagementDTO(m.getId(), m.getCode(), m.getDescription(), m.getGeneralManagement().getId()))
      .orElseThrow(() -> new EntityNotFoundException("Management not found: " + id));
  }
}
