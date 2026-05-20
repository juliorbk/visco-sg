package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateSupplierRequest;
import com.visco.backend.models.dtos.SupplierDTO;
import com.visco.backend.models.dtos.UpdateSupplierRequest;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.SupplierRepository;
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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private PurchaseOrderRepository orderRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder()
                .id(1L).name("Test Supplier").email("supplier@test.com")
                .active(true).currency(Currency.USD).build();
    }

    @Test
    void createSupplier_Success() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "Test Supplier", "Address", "supplier@test.com",
                Set.of("123456"), "Description", Currency.USD, "SAP001", null
        );

        when(supplierRepository.existsByName("Test Supplier")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        SupplierDTO result = supplierService.createSupplier(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Supplier");
    }

    @Test
    void createSupplier_FailsWhenNameExists() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "Test Supplier", "Address", "supplier@test.com",
                Set.of("123456"), "Description", Currency.USD, "SAP001", null
        );

        when(supplierRepository.existsByName("Test Supplier")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getAllSuppliers_ReturnsPage() {
        Page<Supplier> page = new PageImpl<>(Collections.singletonList(testSupplier));
        when(supplierRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<SupplierDTO> result = supplierService.getAllSuppliers(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSupplierById_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        SupplierDTO result = supplierService.getSupplierById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Supplier");
    }

    @Test
    void getSupplierById_FailsWhenNotFound() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateSupplier_Success() {
        UpdateSupplierRequest request = new UpdateSupplierRequest(
                "Updated Supplier", "Updated Address", "updated@test.com",
                null, "Updated Description", "SAP002", Currency.EUR, null
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        SupplierDTO result = supplierService.updateSupplier(1L, request);

        assertThat(result).isNotNull();
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void updateSupplier_FailsWhenNotFound() {
        UpdateSupplierRequest request = new UpdateSupplierRequest(
                "Updated", "Address", "updated@test.com", null, "Description", null, null, null
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.updateSupplier(1L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deactivateSupplier_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        supplierService.deactivateSupplier(1L);

        assertThat(testSupplier.getActive()).isFalse();
    }

    @Test
    void deactivateSupplier_FailsWhenAlreadyInactive() {
        testSupplier.setActive(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        assertThatThrownBy(() -> supplierService.deactivateSupplier(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already inactive");
    }

    @Test
    void deactivateSupplier_FailsWhenNotFound() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deactivateSupplier(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void activateSupplier_Success() {
        testSupplier.setActive(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        supplierService.activateSupplier(1L);

        assertThat(testSupplier.getActive()).isTrue();
    }

    @Test
    void activateSupplier_FailsWhenAlreadyActive() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        assertThatThrownBy(() -> supplierService.activateSupplier(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void getActiveSuppliers_ReturnsPage() {
        Page<Supplier> page = new PageImpl<>(Collections.singletonList(testSupplier));
        when(supplierRepository.findByActiveTrue(any(PageRequest.class))).thenReturn(page);

        Page<SupplierDTO> result = supplierService.getActiveSuppliers(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
    }

    @Test
    void getInactiveSuppliers_ReturnsPage() {
        Page<Supplier> page = new PageImpl<>(Collections.emptyList());
        when(supplierRepository.findByActiveFalse(any(PageRequest.class))).thenReturn(page);

        Page<SupplierDTO> result = supplierService.getInactiveSuppliers(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
    }

    @Test
    void getSupplierPerformance_ReturnsList() {
        when(orderRepository.getSupplierPerformance(any())).thenReturn(Collections.emptyList());

        var result = supplierService.getSupplierPerformance(6);

        assertThat(result).isNotNull();
    }

    @Test
    void getSupplierPerformanceChart_ReturnsList() {
        when(orderRepository.getMonthlySupplierStats(any())).thenReturn(Collections.emptyList());

        var result = supplierService.getSupplierPerformanceChart(6);

        assertThat(result).isNotNull();
    }
}
