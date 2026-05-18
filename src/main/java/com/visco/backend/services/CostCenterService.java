package com.visco.backend.services;

import com.visco.backend.models.dtos.CostCenterDTO;
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
    public Page<CostCenterDTO> getCostCenters(Pageable pageable) {
        return costCenterRepository
            .findAllByOrderByFullDescriptionAsc(pageable)
            .map(CostCenterDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CostCenterDTO> getAllCostCenters() {
        return costCenterRepository
            .findAllByOrderByFullDescriptionAsc()
            .stream()
            .map(CostCenterDTO::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public CostCenterDTO getCostCenterById(Long id) {
        return costCenterRepository
            .findById(id)
            .map(CostCenterDTO::fromEntity)
            .orElseThrow(() -> new EntityNotFoundException("Cost center not found: " + id));
    }
}
