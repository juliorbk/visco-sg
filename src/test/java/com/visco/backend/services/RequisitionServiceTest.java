package com.visco.backend.services;

import com.visco.backend.models.dtos.*;
import com.visco.backend.models.entities.*;
import com.visco.backend.repositories.*;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequisitionServiceTest {

    @Mock
    private RequisitionRepository requisitionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CostCenterRepository costCenterRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RequisitionService requisitionService;

    private User testUser;
    private CostCenter testCostCenter;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID()).name("Test User").email("test@example.com")
                .role(UserRole.USER).active(true).build();
        testCostCenter = CostCenter.builder().id(1L).fullDescription("Test Cost Center").build();
        testProduct = Product.builder().id(1L).name("Test Product").sku("SKU-001").uom(Uom.UN).build();
    }

    @Test
    void createRequisition_Success() {
        CreateRequisitionRequest request = new CreateRequisitionRequest(
                "REQ-001", "Description", testUser.getId(), 1L,
                List.of(new RequisitionItemRequest(1L, 10, "Notes"))
        );

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(testCostCenter));
        when(productRepository.findAllById(any())).thenReturn(List.of(testProduct));

        Requisition saved = Requisition.builder()
                .id(1L).requisitionNumber("REQ-001").description("Description")
                .status(RequisitionStatus.PENDING).requestedBy(testUser).costCenter(testCostCenter)
                .createdAt(LocalDateTime.now()).build();
        RequisitionItem item = RequisitionItem.builder().product(testProduct).quantity(10).notes("Notes").build();
        saved.getItems().add(item);

        when(requisitionRepository.save(any(Requisition.class))).thenReturn(saved);

        RequisitionResponse result = requisitionService.createRequisition(request);

        assertThat(result).isNotNull();
        assertThat(result.requisitionNumber()).isEqualTo("REQ-001");
    }

    @Test
    void createRequisition_FailsWhenUserNotFound() {
        UUID unknownId = UUID.randomUUID();
        CreateRequisitionRequest request = new CreateRequisitionRequest(
                "REQ-001", "Description", unknownId, 1L,
                List.of(new RequisitionItemRequest(1L, 10, "Notes"))
        );

        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.createRequisition(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createRequisition_FailsWhenCostCenterNotFound() {
        CreateRequisitionRequest request = new CreateRequisitionRequest(
                "REQ-001", "Description", testUser.getId(), 999L,
                List.of(new RequisitionItemRequest(1L, 10, "Notes"))
        );

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(costCenterRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requisitionService.createRequisition(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void submitForApproval_Success() {
        Requisition req = Requisition.builder()
                .id(1L).requisitionNumber("REQ-001").status(RequisitionStatus.PENDING)
                .requestedBy(testUser).costCenter(testCostCenter).build();

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any(Requisition.class))).thenReturn(req);

        RequisitionResponse result = requisitionService.submitForApproval(1L);

        assertThat(result).isNotNull();
        assertThat(req.getStatus()).isEqualTo(RequisitionStatus.AWAITING_APPROVAL);
    }

    @Test
    void submitForApproval_FailsWhenNotPendingOrDraft() {
        Requisition req = Requisition.builder()
                .id(1L).status(RequisitionStatus.APPROVED).build();

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.submitForApproval(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING or DRAFT");
    }

    @Test
    void approveRequisition_Success() {
        Requisition req = Requisition.builder()
                .id(1L).requisitionNumber("REQ-001").status(RequisitionStatus.AWAITING_APPROVAL)
                .requestedBy(testUser).costCenter(testCostCenter).build();

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(req));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(requisitionRepository.save(any(Requisition.class))).thenReturn(req);

        RequisitionResponse result = requisitionService.approveRequisition(1L, testUser.getId(), "Approved");

        assertThat(result).isNotNull();
        assertThat(req.getStatus()).isEqualTo(RequisitionStatus.APPROVED);
    }

    @Test
    void rejectRequisition_Success() {
        Requisition req = Requisition.builder()
                .id(1L).requisitionNumber("REQ-001").status(RequisitionStatus.AWAITING_APPROVAL)
                .requestedBy(testUser).costCenter(testCostCenter).build();

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any(Requisition.class))).thenReturn(req);

        RequisitionResponse result = requisitionService.rejectRequisition(1L, testUser.getId(), "Rejected");

        assertThat(result).isNotNull();
        assertThat(req.getStatus()).isEqualTo(RequisitionStatus.REJECTED);
    }

    @Test
    void cancelRequisition_Success() {
        Requisition req = Requisition.builder()
                .id(1L).requisitionNumber("REQ-001").status(RequisitionStatus.PENDING)
                .requestedBy(testUser).costCenter(testCostCenter).build();

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any(Requisition.class))).thenReturn(req);

        RequisitionResponse result = requisitionService.cancelRequisition(1L);

        assertThat(result).isNotNull();
        assertThat(req.getStatus()).isEqualTo(RequisitionStatus.CANCELLED);
    }

    @Test
    void cancelRequisition_FailsWhenAlreadyConverted() {
        Requisition req = Requisition.builder()
                .id(1L).status(RequisitionStatus.CONVERTED).build();

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.cancelRequisition(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getAllRequisitions_ReturnsPage() {
        Requisition req = Requisition.builder()
                .id(1L).requisitionNumber("REQ-001").status(RequisitionStatus.PENDING)
                .requestedBy(testUser).costCenter(testCostCenter).build();

        Page<Requisition> page = new PageImpl<>(List.of(req));
        when(requisitionRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<RequisitionResponse> result = requisitionService.getAllRequisitions(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getRequisitionsByStatus_ReturnsPage() {
        Requisition req = Requisition.builder()
                .id(1L).requisitionNumber("REQ-001").status(RequisitionStatus.PENDING)
                .requestedBy(testUser).costCenter(testCostCenter).build();

        Page<Requisition> page = new PageImpl<>(List.of(req));
        when(requisitionRepository.findByStatus(any(RequisitionStatus.class), any(PageRequest.class))).thenReturn(page);

        Page<RequisitionResponse> result = requisitionService.getRequisitionsByStatus(RequisitionStatus.PENDING, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
    }

    @Test
    void getRequisitionById_Success() {
        Requisition req = Requisition.builder()
                .id(1L).requisitionNumber("REQ-001").status(RequisitionStatus.PENDING)
                .requestedBy(testUser).costCenter(testCostCenter).build();

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(req));

        RequisitionResponse result = requisitionService.getRequisitionById(1L);

        assertThat(result).isNotNull();
        assertThat(result.requisitionNumber()).isEqualTo("REQ-001");
    }

    @Test
    void markAsConverted_Success() {
        Requisition req = Requisition.builder()
                .id(1L).requisitionNumber("REQ-001").status(RequisitionStatus.APPROVED)
                .requestedBy(testUser).costCenter(testCostCenter).build();

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(req));
        when(requisitionRepository.save(any(Requisition.class))).thenReturn(req);

        RequisitionResponse result = requisitionService.markAsConverted(1L);

        assertThat(result).isNotNull();
        assertThat(req.getStatus()).isEqualTo(RequisitionStatus.CONVERTED);
    }

    @Test
    void markAsConverted_FailsWhenNotApproved() {
        Requisition req = Requisition.builder()
                .id(1L).status(RequisitionStatus.PENDING).build();

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> requisitionService.markAsConverted(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved");
    }
}
