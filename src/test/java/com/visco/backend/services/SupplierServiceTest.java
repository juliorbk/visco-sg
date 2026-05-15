package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.visco.backend.models.dtos.CreateSupplierRequest;
import com.visco.backend.models.dtos.SupplierDTO;
import com.visco.backend.models.dtos.SupplierPerformanceDTO;
import com.visco.backend.models.dtos.SupplierPerformanceMonthlyDTO;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.SupplierRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private PurchaseOrderRepository orderRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier supplier;

    @BeforeEach
    void setUp() {
        supplier = Supplier.builder()
                .id(1L)
                .name("Test Supplier")
                .email("supplier@test.com")
                .address("123 Test St")
                .description("A test supplier")
                .phoneNumbers(new HashSet<>(Set.of("+123456789")))
                .currency(Currency.USD)
                .sapCode("SAP-001")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createSupplier_shouldSucceed() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "Test Supplier", "123 Test St", "supplier@test.com",
                new HashSet<>(Set.of("+123456789")), "A test supplier",
                Currency.USD, "SAP-001", null);

        when(supplierRepository.existsByName("Test Supplier")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        SupplierDTO result = supplierService.createSupplier(request);

        assertNotNull(result);
        assertEquals("Test Supplier", result.getName());
        assertEquals("supplier@test.com", result.getContactEmail());
        assertEquals("USD", result.getCurrency());
    }

    @Test
    void createSupplier_shouldThrow_whenNameExists() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "Test Supplier", "addr", "e@t.com", Set.of("+123"), "desc", Currency.USD, null, null);

        when(supplierRepository.existsByName("Test Supplier")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> supplierService.createSupplier(request));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void getAllSuppliers_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(supplierRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(supplier)));

        Page<SupplierDTO> result = supplierService.getAllSuppliers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void updateSupplier_shouldSucceed() {
        Supplier updatedData = Supplier.builder()
                .name("Updated Supplier")
                .email("updated@test.com")
                .address("456 Updated St")
                .description("Updated description")
                .currency(Currency.EUR)
                .phoneNumbers(new HashSet<>(Set.of("+987654321")))
                .sapCode("SAP-002")
                .build();

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(i -> i.getArgument(0));

        SupplierDTO result = supplierService.updateSupplier(1L, updatedData);

        assertEquals("Updated Supplier", result.getName());
        assertEquals("EUR", result.getCurrency());
        assertEquals("updated@test.com", result.getContactEmail());
    }

    @Test
    void updateSupplier_shouldThrow_whenNotFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> supplierService.updateSupplier(99L, supplier));
    }

    @Test
    void deleteSupplier_shouldSucceed_whenInactive() {
        supplier.setActive(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.deleteSupplier(1L);

        verify(supplierRepository).delete(supplier);
    }

    @Test
    void deleteSupplier_shouldThrow_whenActive() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThrows(IllegalStateException.class, () -> supplierService.deleteSupplier(1L));
        verify(supplierRepository, never()).delete(any());
    }

    @Test
    void deactivateSupplier_shouldSucceed() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        supplierService.deactivateSupplier(1L);

        assertFalse(supplier.getActive());
        verify(supplierRepository).save(supplier);
    }

    @Test
    void deactivateSupplier_shouldThrow_whenAlreadyInactive() {
        supplier.setActive(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThrows(IllegalStateException.class, () -> supplierService.deactivateSupplier(1L));
    }

    @Test
    void activateSupplier_shouldSucceed() {
        supplier.setActive(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        supplierService.activateSupplier(1L);

        assertTrue(supplier.getActive());
        verify(supplierRepository).save(supplier);
    }

    @Test
    void activateSupplier_shouldThrow_whenAlreadyActive() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThrows(IllegalStateException.class, () -> supplierService.activateSupplier(1L));
    }

    @Test
    void getSupplierById_shouldReturnDTO() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        SupplierDTO result = supplierService.getSupplierById(1L);

        assertNotNull(result);
        assertEquals("Test Supplier", result.getName());
    }

    @Test
    void getSupplierById_shouldThrow_whenNotFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> supplierService.getSupplierById(99L));
    }

    @Test
    void getActiveSuppliers_shouldReturnActive() {
        Pageable pageable = PageRequest.of(0, 10);
        when(supplierRepository.findByActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of()));

        Page<SupplierDTO> result = supplierService.getActiveSuppliers(pageable);

        assertNotNull(result);
    }

    @Test
    void getInactiveSuppliers_shouldReturnInactive() {
        Pageable pageable = PageRequest.of(0, 10);
        when(supplierRepository.findByActiveFalse(pageable)).thenReturn(new PageImpl<>(List.of()));

        Page<SupplierDTO> result = supplierService.getInactiveSuppliers(pageable);

        assertNotNull(result);
    }

    @Test
    void getSuppliersByCurrency_shouldReturnFiltered() {
        Pageable pageable = PageRequest.of(0, 10);
        SupplierDTO dto = SupplierDTO.fromSupplier(supplier);
        Page<SupplierDTO> page = new PageImpl<>(List.of(dto));
        when(supplierRepository.findByCurrency(Currency.USD, pageable)).thenReturn(page);

        Page<SupplierDTO> result = supplierService.getSuppliersByCurrency(Currency.USD, pageable);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getSupplierPerformance_shouldReturnAggregatedData() {
        LocalDateTime now = LocalDateTime.now();
        Object month = java.sql.Timestamp.valueOf(now.withDayOfMonth(1).withHour(0).withMinute(0));

        List<PurchaseOrderRepository.SupplierPerformanceProjection> rows = List.of(
                createPerformanceProjection(1L, "Test Supplier", month, 10L, 8L, BigDecimal.valueOf(5000)));

        when(orderRepository.getSupplierPerformance(any(LocalDateTime.class))).thenReturn(rows);

        List<SupplierPerformanceDTO> result = supplierService.getSupplierPerformance(6);

        assertEquals(1, result.size());
        assertEquals("Test Supplier", result.get(0).getSupplierName());
        assertEquals(10L, result.get(0).getTotalOrders());
        assertEquals(8L, result.get(0).getTotalDelivered());
        assertEquals(80.0, result.get(0).getFulfillmentRate(), 0.1);
        assertEquals(BigDecimal.valueOf(5000), result.get(0).getTotalSpend());
    }

    @Test
    void getSupplierPerformanceChart_shouldReturnChartData() {
        LocalDateTime now = LocalDateTime.now();
        Object month = java.sql.Timestamp.valueOf(now.withDayOfMonth(1).withHour(0).withMinute(0));

        List<PurchaseOrderRepository.MonthlySupplierStatsProjection> rows = List.of(
                createMonthlyProjection(month, 1L, 10L, 8L),
                createMonthlyProjection(month, 2L, 5L, 3L));

        when(orderRepository.getMonthlySupplierStats(any(LocalDateTime.class))).thenReturn(rows);

        List<SupplierPerformanceMonthlyDTO> result = supplierService.getSupplierPerformanceChart(6);

        assertFalse(result.isEmpty());
        assertNotNull(result.get(0).getMonth());
    }

    private PurchaseOrderRepository.SupplierPerformanceProjection createPerformanceProjection(
            Long supplierId, String name, Object month, Long total, Long delivered, BigDecimal spend) {
        return new PurchaseOrderRepository.SupplierPerformanceProjection() {
            @Override
            public Long getSupplierId() {
                return supplierId;
            }

            @Override
            public String getSupplierName() {
                return name;
            }

            @Override
            public Object getMonth() {
                return month;
            }

            @Override
            public Long getTotalOrders() {
                return total;
            }

            @Override
            public Long getDeliveredOrders() {
                return delivered;
            }

            @Override
            public BigDecimal getTotalSpend() {
                return spend;
            }
        };
    }

    private PurchaseOrderRepository.MonthlySupplierStatsProjection createMonthlyProjection(
            Object month, Long supplierId, Long total, Long delivered) {
        return new PurchaseOrderRepository.MonthlySupplierStatsProjection() {
            @Override
            public Object getMonth() {
                return month;
            }

            @Override
            public Long getSupplierId() {
                return supplierId;
            }

            @Override
            public Long getTotalOrders() {
                return total;
            }

            @Override
            public Long getDeliveredOrders() {
                return delivered;
            }
        };
    }
}
