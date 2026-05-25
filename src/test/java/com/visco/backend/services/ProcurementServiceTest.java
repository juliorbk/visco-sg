package com.visco.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemRequest;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;
import com.visco.backend.models.entities.Requisition;
import com.visco.backend.models.entities.RequisitionItem;
import com.visco.backend.models.entities.RequisitionStatus;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.RequisitionRepository;
import com.visco.backend.repositories.SupplierRepository;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.repositories.WarehouseRepository;
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

  @Captor
  private ArgumentCaptor<PurchaseOrder> orderCaptor;

  private static final Long SUPPLIER_ID = 1L;
  private static final Long WAREHOUSE_ID = 1L;
  private static final Long PRODUCT_ID = 1L;
  private static final Long REQUISITION_ID = 1L;
  private static final UUID USER_ID = UUID.randomUUID();
  private static final BigDecimal UNIT_PRICE = new BigDecimal("100.00");
  private static final int QUANTITY = 10;

  // ── Helpers ──────────────────────────────────────────────────────

  private User buildUser(UserRole role) {
    return User.builder()
      .id(USER_ID)
      .name("Test User")
      .email("user@test.com")
      .password("encodedPass")
      .role(role)
      .costCenter(
        CostCenter.builder().id(1L).fullDescription("Test Area").build()
      )
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

  private Supplier buildSupplier() {
    return Supplier.builder()
      .id(SUPPLIER_ID)
      .name("Test Supplier")
      .email("supplier@test.com")
      .address("123 Test St")
      .currency(Currency.USD)
      .active(true)
      .build();
  }

  private PurchaseOrder buildPurchaseOrder(PurchaseOrderStatus status) {
    Product product = buildProduct();
    Supplier supplier = buildSupplier();
    User user = buildUser(UserRole.PROCUREMENT);
    Warehouse warehouse = buildWarehouse();
    PurchaseOrderItem item = PurchaseOrderItem.builder()
      .id(1L)
      .product(product)
      .quantity(QUANTITY)
      .unitPrice(UNIT_PRICE)
      .build();
    PurchaseOrder order = PurchaseOrder.builder()
      .id(1L)
      .orderNumber("PO-001")
      .description("Test PO")
      .status(status)
      .supplier(supplier)
      .createdBy(user)
      .destinationWarehouse(warehouse)
      .paymentMethod(PaymentMethod.BANK_TRANSFER)
      .type(PurchaseOrderType.MATERIALS)
      .leadTime(30)
      .createdAt(LocalDateTime.now())
      .items(new ArrayList<>())
      .build();
    item.setPurchaseOrder(order);
    order.getItems().add(item);
    return order;
  }

  private Requisition buildRequisition(RequisitionStatus status) {
    Product product = buildProduct();
    RequisitionItem item = RequisitionItem.builder()
      .id(1L)
      .product(product)
      .quantity(QUANTITY)
      .build();
    Requisition req = Requisition.builder()
      .id(REQUISITION_ID)
      .requisitionNumber("REQ-001")
      .description("Test Requisition")
      .requestedBy(buildUser(UserRole.USER))
      .costCenter(CostCenter.builder().id(1L).fullDescription("Area").build())
      .status(status)
      .createdAt(LocalDateTime.now())
      .items(new ArrayList<>())
      .build();
    item.setRequisition(req);
    req.getItems().add(item);
    return req;
  }

  private Warehouse buildWarehouse() {
    return Warehouse.builder()
      .id(WAREHOUSE_ID)
      .name("Main Warehouse")
      .physicalAddress("456 Warehouse Ave")
      .description("Primary storage")
      .active(true)
      .build();
  }

  private StockLevel buildStockLevel(
    BigDecimal currentStock,
    BigDecimal pendingStock
  ) {
    return StockLevel.builder()
      .id(1L)
      .product(buildProduct())
      .warehouse(buildWarehouse())
      .currentStock(currentStock)
      .pendingStock(pendingStock)
      .build();
  }

  private CreatePurchaseOrderRequest buildCreateRequest(Long requisitionId) {
    return new CreatePurchaseOrderRequest(
      "PO-001",
      "Test PO Description",
      SUPPLIER_ID,
      WAREHOUSE_ID,
      PaymentMethod.BANK_TRANSFER,
      PurchaseOrderType.MATERIALS,
      USER_ID,
      requisitionId,
      30,
      List.of(new PurchaseOrderItemRequest(PRODUCT_ID, QUANTITY, UNIT_PRICE))
    );
  }

  // ── createPurchaseOrder ──────────────────────────────────────────

  @Test
  void shouldCreatePurchaseOrder_whenAllDataIsValid() {
    var supplier = buildSupplier();
    var user = buildUser(UserRole.PROCUREMENT);
    var warehouse = buildWarehouse();
    var product = buildProduct();

    when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(
      Optional.of(supplier)
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(warehouse)
    );
    when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(
      List.of(product)
    );

    when(purchaseOrderRepository.save(any())).thenAnswer(invocation -> {
      PurchaseOrder po = invocation.getArgument(0);
      po.setId(1L);
      return po;
    });

    PurchaseOrderResponse response = procurementService.createPurchaseOrder(
      buildCreateRequest(null)
    );

    assertThat(response).isNotNull();
    assertThat(response.orderNumber()).isEqualTo("PO-001");
    assertThat(response.status()).isEqualTo(PurchaseOrderStatus.PENDING);

    verify(warehouseService).addPendingStockByWarehouse(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(QUANTITY)
    );
    verify(purchaseOrderRepository).save(any());
  }

  @Test
  void shouldCreatePurchaseOrder_whenRequisitionIsProvidedAndApproved() {
    var supplier = buildSupplier();
    var user = buildUser(UserRole.PROCUREMENT);
    var warehouse = buildWarehouse();
    var product = buildProduct();
    var requisition = buildRequisition(RequisitionStatus.APPROVED);

    when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(
      Optional.of(supplier)
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(warehouse)
    );
    when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(
      List.of(product)
    );
    when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(
      Optional.of(requisition)
    );
    when(purchaseOrderRepository.save(any())).thenAnswer(invocation -> {
      PurchaseOrder po = invocation.getArgument(0);
      po.setId(1L);
      return po;
    });

    PurchaseOrderResponse response = procurementService.createPurchaseOrder(
      buildCreateRequest(REQUISITION_ID)
    );

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo(PurchaseOrderStatus.PENDING);
    assertThat(requisition.getStatus()).isEqualTo(RequisitionStatus.CONVERTED);

    verify(requisitionRepository).save(requisition);
    verify(purchaseOrderRepository).save(
      argThat(po -> po.getRequisition() != null)
    );
  }

  @Test
  void shouldThrowEntityNotFoundException_whenSupplierNotFound() {
    when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      procurementService.createPurchaseOrder(buildCreateRequest(null))
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Supplier not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenUserNotFound() {
    when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(
      Optional.of(buildSupplier())
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      procurementService.createPurchaseOrder(buildCreateRequest(null))
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("User not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenWarehouseNotFound() {
    when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(
      Optional.of(buildSupplier())
    );
    when(userRepository.findById(USER_ID)).thenReturn(
      Optional.of(buildUser(UserRole.PROCUREMENT))
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      procurementService.createPurchaseOrder(buildCreateRequest(null))
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Warehouse not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenProductNotFound() {
    when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(
      Optional.of(buildSupplier())
    );
    when(userRepository.findById(USER_ID)).thenReturn(
      Optional.of(buildUser(UserRole.PROCUREMENT))
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(buildWarehouse())
    );
    when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(
      List.of()
    );

    assertThatThrownBy(() ->
      procurementService.createPurchaseOrder(buildCreateRequest(null))
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Product not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenRequisitionNotFound() {
    when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(
      Optional.of(buildSupplier())
    );
    when(userRepository.findById(USER_ID)).thenReturn(
      Optional.of(buildUser(UserRole.PROCUREMENT))
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(buildWarehouse())
    );
    when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      procurementService.createPurchaseOrder(buildCreateRequest(REQUISITION_ID))
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Requisition not found");
  }

  @Test
  void shouldThrowIllegalStateException_whenRequisitionIsNotApproved() {
    when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(
      Optional.of(buildSupplier())
    );
    when(userRepository.findById(USER_ID)).thenReturn(
      Optional.of(buildUser(UserRole.PROCUREMENT))
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(buildWarehouse())
    );
    when(requisitionRepository.findById(REQUISITION_ID)).thenReturn(
      Optional.of(buildRequisition(RequisitionStatus.PENDING))
    );

    assertThatThrownBy(() ->
      procurementService.createPurchaseOrder(buildCreateRequest(REQUISITION_ID))
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(
        "Only approved requisitions can be converted to PO"
      );
  }

  // ── submitOrderForApproval ──────────────────────────────────────

  @Test
  void shouldSubmitOrderForApproval_whenStatusIsPending() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.PENDING);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    PurchaseOrderResponse response = procurementService.submitOrderForApproval(
      1L
    );

    assertThat(response.status()).isEqualTo(
      PurchaseOrderStatus.AWAITING_APPROVAL
    );
    verify(purchaseOrderRepository).save(orderCaptor.capture());
    assertThat(orderCaptor.getValue().getStatus()).isEqualTo(
      PurchaseOrderStatus.AWAITING_APPROVAL
    );
  }

  @Test
  void shouldThrowIllegalStateException_whenSubmittingNonPendingOrderForApproval() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> procurementService.submitOrderForApproval(1L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(
        "Only pending orders can be submitted for approval"
      );
  }

  @Test
  void shouldThrowEntityNotFoundException_whenSubmittingNonexistentOrderForApproval() {
    when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> procurementService.submitOrderForApproval(99L))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Purchase order not found");
  }

  @Test
  void shouldThrowIllegalStateException_whenSubmittingAwaitingApprovalOrder() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.AWAITING_APPROVAL);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> procurementService.submitOrderForApproval(1L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(
        "Only pending orders can be submitted for approval"
      );
  }

  // ── markAsApproved ─────────────────────────────────────────────

  @Test
  void shouldMarkAsApproved_whenStatusIsAwaitingApproval() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.AWAITING_APPROVAL);
    var approver = buildUser(UserRole.MANAGER);
    UUID approverId = UUID.randomUUID();
    approver.setId(approverId);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(userRepository.findById(approverId)).thenReturn(Optional.of(approver));
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    PurchaseOrderResponse response = procurementService.markAsApproved(
      1L,
      approverId,
      "Approved"
    );

    assertThat(response.status()).isEqualTo(PurchaseOrderStatus.APPROVED);
    verify(purchaseOrderRepository).save(orderCaptor.capture());
    assertThat(orderCaptor.getValue().getApprovedBy()).isEqualTo(approver);
    assertThat(orderCaptor.getValue().getApprovalNotes()).isEqualTo("Approved");
  }

  @Test
  void shouldThrowIllegalStateException_whenApprovingNonAwaitingOrder() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.PENDING);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() ->
      procurementService.markAsApproved(1L, UUID.randomUUID(), "Notes")
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Only orders awaiting approval can be approved");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenApproverUserNotFound() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.AWAITING_APPROVAL);
    UUID approverId = UUID.randomUUID();
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(userRepository.findById(approverId)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      procurementService.markAsApproved(1L, approverId, "Notes")
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("User not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenOrderNotFoundForApproval() {
    when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      procurementService.markAsApproved(99L, UUID.randomUUID(), "Notes")
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Purchase order not found");
  }

  // ── markAsApprovedByEmail ──────────────────────────────────────

  @Test
  void shouldMarkAsApprovedByEmail_whenApproverEmailIsValid() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.AWAITING_APPROVAL);
    var approver = buildUser(UserRole.MANAGER);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(userRepository.findByEmail("manager@test.com")).thenReturn(
      Optional.of(approver)
    );
    when(userRepository.findById(approver.getId())).thenReturn(
      Optional.of(approver)
    );
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    PurchaseOrderResponse response = procurementService.markAsApprovedByEmail(
      1L,
      "manager@test.com",
      "Approved"
    );

    assertThat(response.status()).isEqualTo(PurchaseOrderStatus.APPROVED);
  }

  @Test
  void shouldThrowUsernameNotFoundException_whenApproverEmailNotFound() {
    when(userRepository.findByEmail("unknown@test.com")).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      procurementService.markAsApprovedByEmail(1L, "unknown@test.com", "Notes")
    )
      .isInstanceOf(
        org.springframework.security.core.userdetails
          .UsernameNotFoundException.class
      )
      .hasMessageContaining("Approver not found");
  }

  // ── rejectPurchaseOrder ────────────────────────────────────────

  @Test
  void shouldThrowIllegalStateException_whenRejectingNonAwaitingOrder() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    var user = buildUser(UserRole.PROCUREMENT);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(userRepository.findByEmail("bad")).thenReturn(Optional.of(user));

    assertThatThrownBy(() ->
      procurementService.rejectPurchaseOrder(1L, "bad", "Bad")
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Only orders awaiting approval can be rejected");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenRejectingNonexistentOrder() {
    var user = buildUser(UserRole.PROCUREMENT);
    when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());
    when(userRepository.findByEmail("bad")).thenReturn(Optional.of(user));

    assertThatThrownBy(() ->
      procurementService.rejectPurchaseOrder(99L, "bad", "Bad")
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Purchase order not found");
  }

  @Test
  void shouldRejectPurchaseOrder_withoutRejecterUser_whenNull() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.AWAITING_APPROVAL);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    PurchaseOrderResponse response = procurementService.rejectPurchaseOrder(
      1L,
      null,
      "Reason"
    );

    assertThat(response.status()).isEqualTo(PurchaseOrderStatus.REJECTED);
    assertThat(response.rejectionReason()).isEqualTo("Reason");
  }

  // ── markAsSentToSupplier ───────────────────────────────────────

  @Test
  void shouldMarkAsSentToSupplier_whenStatusIsApproved() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    PurchaseOrderResponse response = procurementService.markAsSentToSupplier(
      1L
    );

    assertThat(response.status()).isEqualTo(PurchaseOrderStatus.IN_TRANSIT);
  }

  @Test
  void shouldThrowIllegalStateException_whenSendingNonApprovedOrder() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.PENDING);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> procurementService.markAsSentToSupplier(1L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Only approved orders can be sent to supplier");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenSendingNonexistentOrder() {
    when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> procurementService.markAsSentToSupplier(99L))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Purchase order not found");
  }

  @Test
  void shouldThrowIllegalStateException_whenSendingAwaitingApprovalOrder() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.AWAITING_APPROVAL);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> procurementService.markAsSentToSupplier(1L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Only approved orders can be sent to supplier");
  }

  // ── cancelOrder ────────────────────────────────────────────────

  @Test
  void shouldCancelOrder_whenStatusIsPending() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.PENDING);
    Warehouse wh = order.getDestinationWarehouse();
    wh.setId(WAREHOUSE_ID);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(goodReceiptRepository.findByPurchaseOrderId(1L)).thenReturn(List.of());
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    PurchaseOrderResponse response = procurementService.cancelOrder(
      1L,
      "Cancelled by user"
    );

    assertThat(response.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    assertThat(response.rejectionReason()).isEqualTo("Cancelled by user");
    verify(purchaseOrderRepository).save(orderCaptor.capture());
    assertThat(orderCaptor.getValue().getStatus()).isEqualTo(
      PurchaseOrderStatus.CANCELLED
    );
  }

  @Test
  void shouldCancelOrderAndSubtractPendingStock_whenUnreceivedItemsExist() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse wh = order.getDestinationWarehouse();
    wh.setId(WAREHOUSE_ID);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(goodReceiptRepository.findByPurchaseOrderId(1L)).thenReturn(List.of());
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    procurementService.cancelOrder(1L, "Cancelled");

    assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    verify(warehouseService).substractPendingStock(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(QUANTITY)
    );
  }

  @Test
  void shouldCancelOrderAndNotSubtractPendingForReceivedItems() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse wh = order.getDestinationWarehouse();
    wh.setId(WAREHOUSE_ID);
    GoodReceipt receipt = GoodReceipt.builder()
      .id(1L)
      .purchaseOrder(order)
      .build();
    GoodReceiptItem receiptItem = GoodReceiptItem.builder()
      .id(1L)
      .goodReceipt(receipt)
      .product(buildProduct())
      .expectedQuantity(BigDecimal.valueOf(QUANTITY))
      .receivedQuantity(BigDecimal.valueOf(QUANTITY))
      .build();
    receipt.setItems(List.of(receiptItem));
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(goodReceiptRepository.findByPurchaseOrderId(1L)).thenReturn(
      List.of(receipt)
    );
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    procurementService.cancelOrder(1L, "Cancelled");

    assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    verify(warehouseService, never()).substractPendingStock(
      any(),
      any(),
      any()
    );
  }

  @Test
  void shouldThrowIllegalStateException_whenCancellingDeliveredOrder() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> procurementService.cancelOrder(1L, "Cancel"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot cancel an order with status");
  }

  @Test
  void shouldThrowIllegalStateException_whenCancellingAlreadyCancelledOrder() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.CANCELLED);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> procurementService.cancelOrder(1L, "Cancel again"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot cancel an order with status");
  }

  @Test
  void shouldThrowIllegalStateException_whenCancellingRejectedOrder() {
    var order = buildPurchaseOrder(PurchaseOrderStatus.REJECTED);
    when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> procurementService.cancelOrder(1L, "Cancel"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot cancel an order with status");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenCancellingNonexistentOrder() {
    when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> procurementService.cancelOrder(99L, "Cancel"))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Purchase order not found");
  }
}
