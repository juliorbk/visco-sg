package com.visco.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visco.backend.models.dtos.CreateRequisitionRequest;
import com.visco.backend.models.dtos.RequisitionItemRequest;
import com.visco.backend.models.dtos.RequisitionResponse;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.Requisition;
import com.visco.backend.models.entities.RequisitionItem;
import com.visco.backend.models.entities.RequisitionStatus;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.RequisitionRepository;
import com.visco.backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequisitionServiceTest {

    @Mock private RequisitionRepository requisitionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CostCenterRepository costCenterRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private RequisitionService requisitionService;

    @Captor private ArgumentCaptor<Requisition> requisitionCaptor;

    private static final Long REQUISITION_ID = 1L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long COST_CENTER_ID = 1L;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final int QUANTITY = 10;

    // ── Helpers ──────────────────────────────────────────────────────

    private User buildUser(UserRole role) {
        return User.builder()
                .id(USER_ID)
                .name("Test User")
                .email("user@test.com")
                .password("encoded")
                .role(role)
                .active(true)
                .build();
    }

    private CostCenter buildCostCenter() {
        return CostCenter.builder()
                .id(COST_CENTER_ID)
                .code("CC-001")
                .fullDescription("Test Cost Center")
                .active(true)
                .build();
    }

    private Product buildProduct() {
        return Product.builder()
                .id(PRODUCT_ID)
                .internalCode("IC-001")
                .sku("SKU-001")
                .name("Test Product")
                .sapCode("SAP-001")
                .uom(Uom.EA)
                .reorderPoint(new BigDecimal("5"))
                .active(true)
                .build();
    }

    private Requisition buildRequisition(RequisitionStatus status) {
        Product product = buildProduct();
        RequisitionItem item = RequisitionItem.builder()
                .id(1L)
                .product(product)
                .quantity(QUANTITY)
                .notes("Test note")
                .build();
        Requisition req = Requisition.builder()
                .id(REQUISITION_ID)
                .requisitionNumber("REQ-001")
                .description("Test Requisition")
                .requestedBy(buildUser(UserRole.USER))
                .costCenter(buildCostCenter())
                .status(status)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        item.setRequisition(req);
        req.getItems().add(item);
        return req;
    }

    private CreateRequisitionRequest buildCreateRequest() {
        return new CreateRequisitionRequest(
                "REQ-001",
                "Test Requisition",
                USER_ID,
                COST_CENTER_ID,
                List.of(new RequisitionItemRequest(PRODUCT_ID, QUANTITY, "Test note"))
        );
    }

    // ── createRequisition ───────────────────────────────────────────

    @Test
    void shouldCreateRequisition_whenAllDataIsValid() {
        User user = buildUser(UserRole.USER);
        CostCenter cc = buildCostCenter();
        Product product = buildProduct();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(costCenterRepository.findById(COST_CENTER_ID)).thenReturn(Optional.of(cc));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(requisitionRepository.save(any())).thenAnswer(inv -> {
            Requisition r = inv.getArgument(0);
            r.setId(REQUISITION_ID);
            return r;
        });

        RequisitionResponse response = requisitionService.createRequisition(buildCreateRequest());

        assertThat(response).isNotNull();
        assertThat(response.requisitionNumber()).isEqualTo("REQ-001");
        assertThat(response.status()).isEqualTo(RequisitionStatus.PENDING);
        verify(requisitionRepository).save(any());
    }

    @Test
    void shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.createRequisition(buildCreateRequest()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenCostCenterNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser(UserRole.USER)));
        when(costCenterRepository.findById(COST_CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.createRequisition(buildCreateRequest()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Area not found");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenProductNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser(UserRole.USER)));
        when(costCenterRepository.findById(COST_CENTER_ID)).thenReturn(Optional.of(buildCostCenter()));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of());

        assertThatThrownBy(() -> requisitionService.createRequisition(buildCreateRequest()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    // ── submitForApproval ───────────────────────────────────────────

    @Test
    void shouldSubmitForApproval_whenStatusIsPending() {
        Requisition req = buildRequisition(RequisitionStatus.PENDING);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any())).thenReturn(req);

        RequisitionResponse response = requisitionService.submitForApproval(REQUISITION_ID);

        assertThat(response.status()).isEqualTo(RequisitionStatus.AWAITING_APPROVAL);
        verify(requisitionRepository).save(requisitionCaptor.capture());
        assertThat(requisitionCaptor.getValue().getStatus()).isEqualTo(RequisitionStatus.AWAITING_APPROVAL);
    }

    @Test
    void shouldSubmitForApproval_whenStatusIsDraft() {
        Requisition req = buildRequisition(RequisitionStatus.DRAFT);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any())).thenReturn(req);

        RequisitionResponse response = requisitionService.submitForApproval(REQUISITION_ID);

        assertThat(response.status()).isEqualTo(RequisitionStatus.AWAITING_APPROVAL);
    }

    @Test
    void shouldThrowIllegalStateException_whenStatusIsNotPendingOrDraft() {
        Requisition req = buildRequisition(RequisitionStatus.APPROVED);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.submitForApproval(REQUISITION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING or DRAFT requisitions can be submitted");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenRequisitionNotFoundForSubmit() {
        when(requisitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.submitForApproval(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Requisition not found");
    }

    @Test
    void shouldThrowIllegalStateException_whenStatusIsAwaitingApproval() {
        Requisition req = buildRequisition(RequisitionStatus.AWAITING_APPROVAL);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.submitForApproval(REQUISITION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING or DRAFT requisitions can be submitted");
    }

    // ── approveRequisition ─────────────────────────────────────────

    @Test
    void shouldApproveRequisition_whenStatusIsAwaitingApproval() {
        Requisition req = buildRequisition(RequisitionStatus.AWAITING_APPROVAL);
        User approver = buildUser(UserRole.MANAGER);
        UUID approverId = UUID.randomUUID();
        approver.setId(approverId);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));
        when(userRepository.findById(approverId)).thenReturn(Optional.of(approver));
        when(requisitionRepository.save(any())).thenReturn(req);

        RequisitionResponse response = requisitionService.approveRequisition(REQUISITION_ID, approverId, "Approved");

        assertThat(response.status()).isEqualTo(RequisitionStatus.APPROVED);
        verify(requisitionRepository).save(requisitionCaptor.capture());
        assertThat(requisitionCaptor.getValue().getApprovedBy()).isEqualTo(approver);
        assertThat(requisitionCaptor.getValue().getApprovalNotes()).isEqualTo("Approved");
    }

    @Test
    void shouldThrowIllegalStateException_whenApprovingNonAwaitingRequisition() {
        Requisition req = buildRequisition(RequisitionStatus.PENDING);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.approveRequisition(REQUISITION_ID, UUID.randomUUID(), "Notes"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only requisitions awaiting approval can be approved");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenApproverNotFound() {
        Requisition req = buildRequisition(RequisitionStatus.AWAITING_APPROVAL);
        UUID approverId = UUID.randomUUID();
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));
        when(userRepository.findById(approverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.approveRequisition(REQUISITION_ID, approverId, "Notes"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenRequisitionNotFoundForApprove() {
        when(requisitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.approveRequisition(99L, UUID.randomUUID(), "Notes"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Requisition not found");
    }

    // ── rejectRequisition ──────────────────────────────────────────

    @Test
    void shouldRejectRequisition_whenStatusIsAwaitingApproval() {
        Requisition req = buildRequisition(RequisitionStatus.AWAITING_APPROVAL);
        UUID rejecterId = UUID.randomUUID();
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any())).thenReturn(req);

        RequisitionResponse response = requisitionService.rejectRequisition(REQUISITION_ID, rejecterId, "Not needed");

        assertThat(response.status()).isEqualTo(RequisitionStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Not needed");
    }

    @Test
    void shouldThrowIllegalStateException_whenRejectingNonAwaiting() {
        Requisition req = buildRequisition(RequisitionStatus.APPROVED);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.rejectRequisition(REQUISITION_ID, UUID.randomUUID(), "Bad"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only requisitions awaiting approval can be rejected");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenRequisitionNotFoundForReject() {
        when(requisitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.rejectRequisition(99L, UUID.randomUUID(), "Bad"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Requisition not found");
    }

    @Test
    void shouldRejectRequisition_withoutRejecter_whenNull() {
        Requisition req = buildRequisition(RequisitionStatus.AWAITING_APPROVAL);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any())).thenReturn(req);

        RequisitionResponse response = requisitionService.rejectRequisition(REQUISITION_ID, null, "Reason");

        assertThat(response.status()).isEqualTo(RequisitionStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Reason");
    }

    // ── cancelRequisition ──────────────────────────────────────────

    @Test
    void shouldCancelRequisition_whenStatusIsPending() {
        Requisition req = buildRequisition(RequisitionStatus.PENDING);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any())).thenReturn(req);

        RequisitionResponse response = requisitionService.cancelRequisition(REQUISITION_ID);

        assertThat(response.status()).isEqualTo(RequisitionStatus.CANCELLED);
    }

    @Test
    void shouldCancelRequisition_whenStatusIsAwaitingApproval() {
        Requisition req = buildRequisition(RequisitionStatus.AWAITING_APPROVAL);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any())).thenReturn(req);

        RequisitionResponse response = requisitionService.cancelRequisition(REQUISITION_ID);

        assertThat(response.status()).isEqualTo(RequisitionStatus.CANCELLED);
    }

    @Test
    void shouldThrowIllegalStateException_whenCancellingConvertedRequisition() {
        Requisition req = buildRequisition(RequisitionStatus.CONVERTED);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.cancelRequisition(REQUISITION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a " + RequisitionStatus.CONVERTED);
    }

    @Test
    void shouldThrowIllegalStateException_whenCancellingAlreadyCancelledRequisition() {
        Requisition req = buildRequisition(RequisitionStatus.CANCELLED);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.cancelRequisition(REQUISITION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a " + RequisitionStatus.CANCELLED);
    }

    @Test
    void shouldThrowEntityNotFoundException_whenRequisitionNotFoundForCancel() {
        when(requisitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.cancelRequisition(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Requisition not found");
    }

    // ── markAsConverted ────────────────────────────────────────────

    @Test
    void shouldMarkAsConverted_whenStatusIsApproved() {
        Requisition req = buildRequisition(RequisitionStatus.APPROVED);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any())).thenReturn(req);

        RequisitionResponse response = requisitionService.markAsConverted(REQUISITION_ID);

        assertThat(response.status()).isEqualTo(RequisitionStatus.CONVERTED);
        verify(requisitionRepository).save(requisitionCaptor.capture());
        assertThat(requisitionCaptor.getValue().getStatus()).isEqualTo(RequisitionStatus.CONVERTED);
    }

    @Test
    void shouldThrowIllegalStateException_whenConvertingNonApprovedRequisition() {
        Requisition req = buildRequisition(RequisitionStatus.AWAITING_APPROVAL);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.markAsConverted(REQUISITION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only approved requisitions can be converted to PO");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenRequisitionNotFoundForConvert() {
        when(requisitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.markAsConverted(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Requisition not found");
    }

    @Test
    void shouldThrowIllegalStateException_whenConvertingAlreadyConvertedRequisition() {
        Requisition req = buildRequisition(RequisitionStatus.CONVERTED);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.markAsConverted(REQUISITION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only approved requisitions can be converted to PO");
    }

    // ── getRequisitionById ─────────────────────────────────────────

    @Test
    void shouldGetRequisitionById_whenExists() {
        Requisition req = buildRequisition(RequisitionStatus.PENDING);
        when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(Optional.of(req));

        RequisitionResponse response = requisitionService.getRequisitionById(REQUISITION_ID);

        assertThat(response).isNotNull();
        assertThat(response.requisitionNumber()).isEqualTo("REQ-001");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenRequisitionNotFoundForGet() {
        when(requisitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.getRequisitionById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Requisition not found");
    }
}
