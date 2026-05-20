package com.visco.backend.services;

import com.visco.backend.models.dtos.CostCenterDTO;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.repositories.CostCenterRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostCenterServiceTest {

    @Mock
    private CostCenterRepository costCenterRepository;

    @InjectMocks
    private CostCenterService costCenterService;

    private CostCenter testCostCenter;

    @BeforeEach
    void setUp() {
        testCostCenter = CostCenter.builder()
                .id(1L).fullDescription("Test Cost Center").build();
    }

    @Test
    void getCostCenters_ReturnsPage() {
        Page<CostCenter> page = new PageImpl<>(List.of(testCostCenter));
        when(costCenterRepository.findAllByOrderByFullDescriptionAsc(any(PageRequest.class))).thenReturn(page);

        Page<CostCenterDTO> result = costCenterService.getCostCenters(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllCostCenters_ReturnsList() {
        when(costCenterRepository.findAllByOrderByFullDescriptionAsc()).thenReturn(List.of(testCostCenter));

        List<CostCenterDTO> result = costCenterService.getAllCostCenters();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    @Test
    void getCostCenterById_Success() {
        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(testCostCenter));

        CostCenterDTO result = costCenterService.getCostCenterById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFullDescription()).isEqualTo("Test Cost Center");
    }

    @Test
    void getCostCenterById_FailsWhenNotFound() {
        when(costCenterRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> costCenterService.getCostCenterById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cost center not found");
    }
}
