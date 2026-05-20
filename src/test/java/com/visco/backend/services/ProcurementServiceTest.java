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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcurementServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private RequisitionRepository requisitionRepository;

    @Mock
    private GoodReceiptRepository goodReceiptRepository;

    @InjectMocks
    private ProcurementService procurementService;

    private Supplier testSupplier;
    private User testUser;
    private Warehouse testWarehouse;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder().id(1L).name("Test Supplier").build();
        testUser = User.builder().id(UUID.randomUUID()).name("Test User").email("test@example.com").role(UserRole.USER).active(true).build();
        testWarehouse = Warehouse.builder().id(1L).name("Test Warehouse").build();
        testProduct = Product.builder().id(1L).name("Test Product").sku("SKU-001").uom(Uom.UN).build();
    }

    @Test
    void createPurchaseOrder_Success() {
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                "PO-001", "Description", testUser.getId(), testWarehouse.getId(),
                testSupplier.getId(), PaymentMethod.BANK_TRANSFER, PurchaseOrderType.MATERIALS, 7, null,
                List.of(new PurchaseOrderItemRequest(1L, 100, BigDecimal.valueOf(50)))
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(productRepository.findAllById(any())).thenReturn(List.of(testProduct));

        PurchaseOrder savedOrder = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.PENDING)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();
        PurchaseOrderItem item = PurchaseOrderItem.builder().product(testProduct).quantity(100).unitPrice(BigDecimal.valueOf(50)).build();
        savedOrder.getItems().add(item);

        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(savedOrder);

        PurchaseOrderResponse result = procurementService.createPurchaseOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).isEqualTo("PO-001");
        verify(warehouseService).addPendingStockByWarehouse(anyLong(), anyLong(), any(BigDecimal.class));
    }

    @Test
    void createPurchaseOrder_FailsWhenSupplierNotFound() {
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                "PO-001", "Description", testUser.getId(), testWarehouse.getId(),
                999L, PaymentMethod.BANK_TRANSFER, PurchaseOrderType.MATERIALS, 7, null,
                List.of(new PurchaseOrderItemRequest(1L, 100, BigDecimal.valueOf(50)))
        );

        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> procurementService.createPurchaseOrder(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getOrderById_Success() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.PENDING)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        PurchaseOrderResponse result = procurementService.getOrderById(1L);

        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).isEqualTo("PO-001");
    }

    @Test
    void getOrderById_FailsWhenNotFound() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> procurementService.getOrderById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getAllOrders_ReturnsPage() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.PENDING)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();

        Page<PurchaseOrder> page = new PageImpl<>(List.of(order));
        when(purchaseOrderRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<PurchaseOrderResponse> result = procurementService.getAllOrders(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void submitOrderForApproval_Success() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.PENDING)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        PurchaseOrderResponse result = procurementService.submitOrderForApproval(1L);

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.AWAITING_APPROVAL);
    }

    @Test
    void submitOrderForApproval_FailsWhenNotPending() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).status(PurchaseOrderStatus.APPROVED).build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> procurementService.submitOrderForApproval(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending");
    }

    @Test
    void markAsApproved_Success() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.AWAITING_APPROVAL)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        PurchaseOrderResponse result = procurementService.markAsApproved(1L, testUser.getId(), "Approved");

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
    }

    @Test
    void rejectPurchaseOrder_Success() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.AWAITING_APPROVAL)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        PurchaseOrderResponse result = procurementService.rejectPurchaseOrder(1L, testUser.getId(), "Rejected reason");

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.REJECTED);
    }

    @Test
    void markAsSentToSupplier_Success() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.APPROVED)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        PurchaseOrderResponse result = procurementService.markAsSentToSupplier(1L);

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.IN_TRANSIT);
    }

    @Test
    void cancelOrder_Success() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.PENDING)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(goodReceiptRepository.findByPurchaseOrderId(1L)).thenReturn(Collections.emptyList());
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        PurchaseOrderResponse result = procurementService.cancelOrder(1L, "Cancelled");

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_FailsWhenAlreadyDelivered() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).status(PurchaseOrderStatus.DELIVERED)
                .destinationWarehouse(testWarehouse).build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> procurementService.cancelOrder(1L, "Cancelled"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markAsApprovedByEmail_Success() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.AWAITING_APPROVAL)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        PurchaseOrderResponse result = procurementService.markAsApprovedByEmail(1L, "test@example.com", "Approved");

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
    }

    @Test
    void createPurchaseOrder_WithApprovedRequisition_Success() {
        Requisition requisition = Requisition.builder()
                .id(1L).status(RequisitionStatus.APPROVED).build();
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                "PO-001", "Description", testUser.getId(), testWarehouse.getId(),
                testSupplier.getId(), PaymentMethod.BANK_TRANSFER, PurchaseOrderType.MATERIALS, 7, 1L,
                List.of(new PurchaseOrderItemRequest(1L, 100, BigDecimal.valueOf(50)))
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(requisition));
        when(productRepository.findAllById(any())).thenReturn(List.of(testProduct));

        PurchaseOrder savedOrder = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.PENDING)
                .supplier(testSupplier).createdBy(testUser).destinationWarehouse(testWarehouse)
                .createdAt(LocalDateTime.now()).build();
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(savedOrder);

        PurchaseOrderResponse result = procurementService.createPurchaseOrder(request);

        assertThat(result).isNotNull();
        assertThat(requisition.getStatus()).isEqualTo(RequisitionStatus.CONVERTED);
    }

    @Test
    void createPurchaseOrder_FailsWithNonApprovedRequisition() {
        Requisition requisition = Requisition.builder()
                .id(1L).status(RequisitionStatus.PENDING).build();
        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                "PO-001", "Description", testUser.getId(), testWarehouse.getId(),
                testSupplier.getId(), PaymentMethod.BANK_TRANSFER, PurchaseOrderType.MATERIALS, 7, 1L,
                List.of(new PurchaseOrderItemRequest(1L, 100, BigDecimal.valueOf(50)))
        );

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(requisition));

        assertThatThrownBy(() -> procurementService.createPurchaseOrder(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved requisitions");
    }
}
