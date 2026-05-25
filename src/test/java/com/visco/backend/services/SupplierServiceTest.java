package com.visco.backend.services;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.visco.backend.models.dtos.CreateSupplierRequest;
import com.visco.backend.models.dtos.SupplierDTO;
import com.visco.backend.models.dtos.UpdateSupplierRequest;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.LegalRepresentative;
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

    @Captor
    private ArgumentCaptor<Supplier> supplierCaptor;

    private static final Long SUPPLIER_ID = 1L;

    // ── Helpers ──────────────────────────────────────────────────────

    private Supplier buildSupplier(boolean active) {
        Set<String> phones = new HashSet<>();
        phones.add("+584141234567");
        Set<LegalRepresentative> reps = new HashSet<>();
        reps.add(LegalRepresentative.builder().id(1L).fullName("John Doe").build());
        return Supplier.builder()
            .id(SUPPLIER_ID)
            .name("Test Supplier")
            .email("supplier@test.com")
            .address("123 Test St")
            .description("A test supplier")
            .phoneNumbers(phones)
            .representatives(reps)
            .currency(Currency.USD)
            .active(active)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // ── createSupplier ─────────────────────────────────────────────

    @Test
    void shouldCreateSupplier_whenNameIsUnique() {
        CreateSupplierRequest request = new CreateSupplierRequest(
            "New Supplier",
            "456 New St",
            "new@test.com",
            Set.of("+58415000000"),
            "New supplier description",
            Currency.VED,
            Set.of("Jane Doe")
        );

        when(supplierRepository.existsByName("New Supplier")).thenReturn(false);
        when(supplierRepository.save(any())).thenAnswer((inv) -> {
            Supplier s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });

        SupplierDTO result = supplierService.createSupplier(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Supplier");
        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getActive()).isTrue();
    }

    @Test
    void shouldCreateSupplier_withoutRepresentatives_whenEmpty() {
        CreateSupplierRequest request = new CreateSupplierRequest(
            "New Supplier",
            "456 New St",
            "new@test.com",
            Set.of("+58415000000"),
            "Desc",
            Currency.VED,
            Set.of()
        );

        when(supplierRepository.existsByName("New Supplier")).thenReturn(false);
        when(supplierRepository.save(any())).thenAnswer((inv) -> {
            Supplier s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });

        SupplierDTO result = supplierService.createSupplier(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Supplier");
    }

    @Test
    void shouldThrowIllegalStateException_whenSupplierNameAlreadyExists() {
        when(supplierRepository.existsByName("Test Supplier")).thenReturn(true);

        CreateSupplierRequest request = new CreateSupplierRequest(
            "Test Supplier",
            "123 St",
            "dup@test.com",
            Set.of("+58414000000"),
            "Dup",
            Currency.USD,
            Set.of()
        );

        assertThatThrownBy(() -> supplierService.createSupplier(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already exists");
    }

    // ── updateSupplier ─────────────────────────────────────────────

    @Test
    void shouldUpdateSupplier_whenExists() {
        Supplier existing = buildSupplier(true);
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any())).thenReturn(existing);

        UpdateSupplierRequest request = new UpdateSupplierRequest(
            "Updated Name",
            "Updated Address",
            "updated@test.com",
            Set.of("+58415999999"),
            "Updated description",
            Currency.EUR,
            Set.of(1L)
        );

        SupplierDTO result = supplierService.updateSupplier(SUPPLIER_ID, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getCurrency()).isEqualTo(Currency.EUR);
    }

    @Test
    void shouldUpdateSupplier_andFilterRepresentativesById() {
        Supplier existing = buildSupplier(true);
        LegalRepresentative rep2 = LegalRepresentative.builder()
            .id(2L)
            .fullName("Jane Smith")
            .build();
        existing.getRepresentatives().add(rep2);

        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any())).thenReturn(existing);

        UpdateSupplierRequest request = new UpdateSupplierRequest(
            "Updated",
            "Addr",
            "email@test.com",
            Set.of("+58415000000"),
            "Desc",
            Currency.USD,
            Set.of(1L)
        );

        supplierService.updateSupplier(SUPPLIER_ID, request);

        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getRepresentatives())
            .hasSize(1)
            .allMatch((r) -> r.getId().equals(1L));
    }

    @Test
    void shouldThrowEntityNotFoundException_whenUpdatingNonexistentSupplier() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateSupplierRequest request = new UpdateSupplierRequest(
            "Name",
            "Addr",
            "e@test.com",
            Set.of("+58415000000"),
            "Desc",
            Currency.USD,
            Set.of()
        );

        assertThatThrownBy(() -> supplierService.updateSupplier(99L, request))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Supplier not found");
    }

    // ── deactivateSupplier ──────────────────────────────────────────

    @Test
    void shouldDeactivateSupplier_whenActive() {
        Supplier supplier = buildSupplier(true);
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));

        supplierService.deactivateSupplier(SUPPLIER_ID);

        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getActive()).isFalse();
    }

    @Test
    void shouldThrowIllegalStateException_whenDeactivatingAlreadyInactive() {
        Supplier supplier = buildSupplier(false);
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));

        assertThatThrownBy(() -> supplierService.deactivateSupplier(SUPPLIER_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already inactive");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenDeactivatingNonexistent() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deactivateSupplier(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Supplier not found with id");
    }

    // ── activateSupplier ────────────────────────────────────────────

    @Test
    void shouldActivateSupplier_whenInactive() {
        Supplier supplier = buildSupplier(false);
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));

        supplierService.activateSupplier(SUPPLIER_ID);

        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getActive()).isTrue();
    }

    @Test
    void shouldThrowIllegalStateException_whenActivatingAlreadyActive() {
        Supplier supplier = buildSupplier(true);
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));

        assertThatThrownBy(() -> supplierService.activateSupplier(SUPPLIER_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already active");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenActivatingNonexistent() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.activateSupplier(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Supplier not found with id");
    }

    // ── getSupplierById ────────────────────────────────────────────

    @Test
    void shouldGetSupplierById_whenExists() {
        Supplier supplier = buildSupplier(true);
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));

        SupplierDTO result = supplierService.getSupplierById(SUPPLIER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Supplier");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenGetByIdNotFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Supplier not found");
    }

    // ── deleteSupplier (soft-delete) ────────────────────────────────

    @Test
    void shouldSoftDeleteSupplier_whenExists() {
        Supplier supplier = buildSupplier(true);
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));

        supplierService.deleteSupplier(SUPPLIER_ID);

        verify(supplierRepository).save(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getActive()).isFalse();
        assertThat(supplierCaptor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void shouldThrowEntityNotFoundException_whenSoftDeleteNotFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deleteSupplier(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Supplier not found");
    }
}
