package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visco.backend.models.dtos.AdjustStockRequest;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.TransferStockRequest;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.Location;
import com.visco.backend.models.entities.MovementType;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;
import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.CostCenterRepository;
import com.visco.backend.repositories.DispatchNoteRepository;
import com.visco.backend.repositories.EmployeeRepository;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.InventoryMovementRepository;
import com.visco.backend.repositories.LocationRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.repositories.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for stock operations in WarehouseService. Focused on the
 * regression scenarios found during the stock review:
 *
 * <ul>
 *   <li>processReceiptItem: pending stock must be subtracted BEFORE current
 *       stock is added so that orphan rows (no pre-existing StockLevel) do
 *       not clobber pending_stock to 0 via the UPSERT branch.
 *   <li>transferStock: the product must be validated BEFORE stock is
 *       mutated, otherwise a missing product would leave the source
 *       debited without crediting the destination.
 *   <li>adjustStock: writes the new absolute value and records a movement
 *       with the signed difference.
 *   <li>substractCurrentStock: surfaces a clear error when stock is
 *       insufficient.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class WarehouseServiceStockTest {

  @Mock private PurchaseOrderRepository purchaseOrderRepository;
  @Mock private GoodReceiptRepository goodReceiptRepository;
  @Mock private StockLevelRepository stockLevelRepository;
  @Mock private WarehouseRepository warehouseRepository;
  @Mock private UserRepository userRepository;
  @Mock private ProductRepository productRepository;
  @Mock private InventoryMovementRepository inventoryMovementRepository;
  @Mock private DispatchNoteRepository dispatchNoteRepository;
  @Mock private EmployeeRepository employeeRepository;
  @Mock private CostCenterRepository costCenterRepository;
  @Mock private LocationRepository locationRepository;

  @InjectMocks private WarehouseService warehouseService;

  private Warehouse destWarehouse;
  private Warehouse otherWarehouse;
  private Product product;
  private PurchaseOrder order;
  private PurchaseOrderItem poItem;
  private User createdBy;
  private Location location;

  @BeforeEach
  void setUp() throws Exception {
    destWarehouse = Warehouse.builder()
      .id(10L)
      .name("Main Warehouse")
      .physicalAddress("Calle 1")
      .description("Main")
      .active(true)
      .build();

    otherWarehouse = Warehouse.builder()
      .id(20L)
      .name("Other Warehouse")
      .physicalAddress("Calle 2")
      .description("Secondary")
      .active(true)
      .build();

    product = Product.builder()
      .id(100L)
      .internalCode("IC-100")
      .sku("SKU-100")
      .name("Test Product")
      .description("Test")
      .sapCode("SAP-100")
      .uom(Uom.EA)
      .reorderPoint(BigDecimal.ZERO)
      .maxStock(BigDecimal.valueOf(1000))
      .active(true)
      .build();

    createdBy = User.builder()
      .id(UUID.randomUUID())
      .name("Receiver")
      .email("rec@example.com")
      .password("x")
      .active(true)
      .build();

    location = Location.builder()
      .id(500L)
      .code("A1")
      .warehouse(destWarehouse)
      .build();

    poItem = PurchaseOrderItem.builder()
      .id(1L)
      .product(product)
      .quantity(BigDecimal.valueOf(10))
      .unitPrice(BigDecimal.valueOf(5))
      .build();

    order = PurchaseOrder.builder()
      .id(99L)
      .orderNumber("PO-0001")
      .description("Test PO")
      .createdBy(createdBy)
      .destinationWarehouse(destWarehouse)
      .status(PurchaseOrderStatus.IN_TRANSIT)
      .paymentMethod(PaymentMethod.CASH)
      .type(PurchaseOrderType.MATERIALS)
      .createdAt(LocalDateTime.now())
      .items(new java.util.ArrayList<>(List.of(poItem)))
      .build();
    poItem.setPurchaseOrder(order);
  }

  // ─────────────────────────────────────────────────────────────
  // processReceiptItem — pendiente se resta ANTES de acreditar current
  // ─────────────────────────────────────────────────────────────

  @Test
  void receiveGoods_subtractsPendingBeforeAddingCurrent() {
    ReceiveGoodsRequest.ReceiveItem receiveItem = new ReceiveGoodsRequest.ReceiveItem(
      product.getId(),
      BigDecimal.valueOf(10),
      location.getId()
    );
    ReceiveGoodsRequest request = new ReceiveGoodsRequest(
      List.of(receiveItem),
      "ok",
      destWarehouse.getId(),
      location.getId(),
      createdBy.getId()
    );

    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(Optional.of(order));
    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(Optional.of(destWarehouse));
    when(locationRepository.findById(location.getId()))
      .thenReturn(Optional.of(location));
    when(userRepository.findById(createdBy.getId()))
      .thenReturn(Optional.of(createdBy));
    when(goodReceiptRepository.getNextReceiptSequence()).thenReturn(1L);
    when(goodReceiptRepository.getTotalReceivedByOrder(order.getId()))
      .thenReturn(List.of());

    warehouseService.receiveGoods(order.getId(), request);

    InOrder inOrder = inOrder(stockLevelRepository);
    inOrder.verify(stockLevelRepository)
      .subtractPendingStockAtomic(product.getId(), destWarehouse.getId(), BigDecimal.valueOf(10));
    inOrder.verify(stockLevelRepository)
      .addCurrentStockAtomic(product.getId(), destWarehouse.getId(), BigDecimal.valueOf(10));
  }

  @Test
  void receiveGoods_recordsInputMovementForReceivedQuantity() {
    ReceiveGoodsRequest.ReceiveItem receiveItem = new ReceiveGoodsRequest.ReceiveItem(
      product.getId(),
      BigDecimal.valueOf(7),
      location.getId()
    );
    ReceiveGoodsRequest request = new ReceiveGoodsRequest(
      List.of(receiveItem),
      null,
      destWarehouse.getId(),
      location.getId(),
      null
    );

    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(Optional.of(order));
    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(Optional.of(destWarehouse));
    when(locationRepository.findById(location.getId()))
      .thenReturn(Optional.of(location));
    when(userRepository.findById(createdBy.getId()))
      .thenReturn(Optional.of(createdBy));
    when(goodReceiptRepository.getNextReceiptSequence()).thenReturn(42L);
    when(goodReceiptRepository.getTotalReceivedByOrder(order.getId()))
      .thenReturn(List.of());

    warehouseService.receiveGoods(order.getId(), request);

    ArgumentCaptor<InventoryMovement> movementCaptor =
      ArgumentCaptor.forClass(InventoryMovement.class);
    verify(inventoryMovementRepository, atLeastOnce()).save(movementCaptor.capture());
    InventoryMovement movement = movementCaptor.getValue();
    assertEquals(MovementType.INPUT, movement.getType());
    assertEquals(BigDecimal.valueOf(7), movement.getQuantity());
    assertEquals(destWarehouse.getId(),
      movement.getToWarehouse() != null ? movement.getToWarehouse().getId() : null);
  }

  @Test
  void receiveGoods_marksOrderPartiallyDeliveredWhenSomeItemsRemain() {
    // Receive 6 of 10 → pending left, order should be PARTIALLY_DELIVERED
    ReceiveGoodsRequest.ReceiveItem receiveItem = new ReceiveGoodsRequest.ReceiveItem(
      product.getId(),
      BigDecimal.valueOf(6),
      location.getId()
    );
    ReceiveGoodsRequest request = new ReceiveGoodsRequest(
      List.of(receiveItem),
      null,
      destWarehouse.getId(),
      location.getId(),
      null
    );

    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(Optional.of(order));
    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(Optional.of(destWarehouse));
    when(locationRepository.findById(location.getId()))
      .thenReturn(Optional.of(location));
    when(userRepository.findById(createdBy.getId()))
      .thenReturn(Optional.of(createdBy));
    when(goodReceiptRepository.getNextReceiptSequence()).thenReturn(2L);
    when(goodReceiptRepository.getTotalReceivedByOrder(order.getId()))
      .thenReturn(List.of());

    warehouseService.receiveGoods(order.getId(), request);

    ArgumentCaptor<PurchaseOrder> orderCaptor =
      ArgumentCaptor.forClass(PurchaseOrder.class);
    verify(purchaseOrderRepository).save(orderCaptor.capture());
    assertEquals(PurchaseOrderStatus.PARTIALLY_DELIVERED, orderCaptor.getValue().getStatus());
  }

  @Test
  void receiveGoods_marksOrderDeliveredWhenAllItemsReceived() {
    ReceiveGoodsRequest.ReceiveItem receiveItem = new ReceiveGoodsRequest.ReceiveItem(
      product.getId(),
      BigDecimal.valueOf(10),
      location.getId()
    );
    ReceiveGoodsRequest request = new ReceiveGoodsRequest(
      List.of(receiveItem),
      null,
      destWarehouse.getId(),
      location.getId(),
      null
    );

    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(Optional.of(order));
    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(Optional.of(destWarehouse));
    when(locationRepository.findById(location.getId()))
      .thenReturn(Optional.of(location));
    when(userRepository.findById(createdBy.getId()))
      .thenReturn(Optional.of(createdBy));
    when(goodReceiptRepository.getNextReceiptSequence()).thenReturn(3L);
    when(goodReceiptRepository.getTotalReceivedByOrder(order.getId()))
      .thenReturn(List.of());

    warehouseService.receiveGoods(order.getId(), request);

    ArgumentCaptor<PurchaseOrder> orderCaptor =
      ArgumentCaptor.forClass(PurchaseOrder.class);
    verify(purchaseOrderRepository).save(orderCaptor.capture());
    assertEquals(PurchaseOrderStatus.DELIVERED, orderCaptor.getValue().getStatus());
  }

  @Test
  void receiveGoods_rejectsAlreadyDeliveredOrder() {
    order.setStatus(PurchaseOrderStatus.DELIVERED);
    ReceiveGoodsRequest request = new ReceiveGoodsRequest(
      List.of(new ReceiveGoodsRequest.ReceiveItem(
        product.getId(),
        BigDecimal.ONE,
        location.getId())),
      null,
      destWarehouse.getId(),
      location.getId(),
      null
    );
    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(Optional.of(order));

    assertThrows(IllegalStateException.class,
      () -> warehouseService.receiveGoods(order.getId(), request));
  }

  // ─────────────────────────────────────────────────────────────
  // transferStock — producto se valida ANTES de mover stock
  // ─────────────────────────────────────────────────────────────

  @Test
  void transferStock_validatesProductBeforeMutatingStock() {
    TransferStockRequest request = new TransferStockRequest(
      product.getId(),
      destWarehouse.getId(),
      otherWarehouse.getId(),
      BigDecimal.valueOf(5),
      "move",
      createdBy.getId()
    );

    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(Optional.of(destWarehouse));
    when(warehouseRepository.findById(otherWarehouse.getId()))
      .thenReturn(Optional.of(otherWarehouse));
    // Product is missing — must throw BEFORE any stock mutation.
    when(productRepository.findById(product.getId())).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
      () -> warehouseService.transferStock(request));

    verify(stockLevelRepository, never()).subtractCurrentStockAtomic(anyLong(), anyLong(), any());
    verify(stockLevelRepository, never()).addCurrentStockAtomic(anyLong(), anyLong(), any());
  }

  @Test
  void transferStock_rejectsInsufficientStock() {
    TransferStockRequest request = new TransferStockRequest(
      product.getId(),
      destWarehouse.getId(),
      otherWarehouse.getId(),
      BigDecimal.valueOf(50),
      "move",
      createdBy.getId()
    );

    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(Optional.of(destWarehouse));
    when(warehouseRepository.findById(otherWarehouse.getId()))
      .thenReturn(Optional.of(otherWarehouse));
    when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
    when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
    when(stockLevelRepository.getStockByProductAndWarehouse(
      product.getId(), destWarehouse.getId()))
      .thenReturn(BigDecimal.valueOf(3));

    assertThrows(IllegalArgumentException.class,
      () -> warehouseService.transferStock(request));
    verify(stockLevelRepository, never()).subtractCurrentStockAtomic(anyLong(), anyLong(), any());
    verify(stockLevelRepository, never()).addCurrentStockAtomic(anyLong(), anyLong(), any());
  }

  @Test
  void transferStock_subtractsFromSourceAndAddsToDestination() {
    TransferStockRequest request = new TransferStockRequest(
      product.getId(),
      destWarehouse.getId(),
      otherWarehouse.getId(),
      BigDecimal.valueOf(5),
      "move",
      createdBy.getId()
    );

    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(Optional.of(destWarehouse));
    when(warehouseRepository.findById(otherWarehouse.getId()))
      .thenReturn(Optional.of(otherWarehouse));
    when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
    when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
    when(stockLevelRepository.getStockByProductAndWarehouse(
      product.getId(), destWarehouse.getId()))
      .thenReturn(BigDecimal.valueOf(10));
    when(stockLevelRepository.subtractCurrentStockAtomic(
      product.getId(), destWarehouse.getId(), BigDecimal.valueOf(5)))
      .thenReturn(1);
    when(stockLevelRepository.addCurrentStockAtomic(
      product.getId(), otherWarehouse.getId(), BigDecimal.valueOf(5)))
      .thenReturn(1);

    warehouseService.transferStock(request);

    InOrder inOrder = inOrder(stockLevelRepository);
    inOrder.verify(stockLevelRepository)
      .subtractCurrentStockAtomic(product.getId(), destWarehouse.getId(), BigDecimal.valueOf(5));
    inOrder.verify(stockLevelRepository)
      .addCurrentStockAtomic(product.getId(), otherWarehouse.getId(), BigDecimal.valueOf(5));
  }

  // ─────────────────────────────────────────────────────────────
  // adjustStock — escribe valor absoluto + movimiento con diferencia firmada
  // ─────────────────────────────────────────────────────────────

  @Test
  void adjustStock_writesAbsoluteValueAndRecordsSignedDifference() {
    AdjustStockRequest request = new AdjustStockRequest(
      product.getId(),
      destWarehouse.getId(),
      BigDecimal.valueOf(15),
      "count",
      createdBy.getId(),
      null
    );

    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(Optional.of(destWarehouse));
    when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
    when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
    when(stockLevelRepository.getStockByProductAndWarehouse(
      product.getId(), destWarehouse.getId()))
      .thenReturn(BigDecimal.valueOf(20));

    warehouseService.adjustStock(request);

    verify(stockLevelRepository).setCurrentStockAtomic(
      product.getId(),
      destWarehouse.getId(),
      BigDecimal.valueOf(15)
    );

    ArgumentCaptor<InventoryMovement> movementCaptor =
      ArgumentCaptor.forClass(InventoryMovement.class);
    verify(inventoryMovementRepository).save(movementCaptor.capture());
    InventoryMovement movement = movementCaptor.getValue();
    // 15 - 20 = -5
    assertEquals(0, BigDecimal.valueOf(-5).compareTo(movement.getQuantity()));
    assertEquals(MovementType.ADJUSTMENT, movement.getType());
  }

  // ─────────────────────────────────────────────────────────────
  // substractCurrentStock — surface clear errors
  // ─────────────────────────────────────────────────────────────

  @Test
  void substractCurrentStock_throwsWhenStockLevelDoesNotExist() {
    when(stockLevelRepository.subtractCurrentStockAtomic(
      product.getId(), destWarehouse.getId(), BigDecimal.valueOf(5)))
      .thenReturn(0);
    when(stockLevelRepository.getCurrentStock(
      product.getId(), destWarehouse.getId()))
      .thenReturn(BigDecimal.ZERO);

    assertThrows(EntityNotFoundException.class,
      () -> warehouseService.substractCurrentStock(
        product.getId(), destWarehouse.getId(), BigDecimal.valueOf(5)));
  }

  @Test
  void substractCurrentStock_throwsWhenInsufficientStock() {
    when(stockLevelRepository.subtractCurrentStockAtomic(
      product.getId(), destWarehouse.getId(), BigDecimal.valueOf(5)))
      .thenReturn(0);
    when(stockLevelRepository.getCurrentStock(
      product.getId(), destWarehouse.getId()))
      .thenReturn(BigDecimal.valueOf(2));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> warehouseService.substractCurrentStock(
        product.getId(), destWarehouse.getId(), BigDecimal.valueOf(5)));
    assertTrue(ex.getMessage().contains("Insufficient stock"));
  }

  // ─────────────────────────────────────────────────────────────
  // addPendingStockByWarehouse
  // ─────────────────────────────────────────────────────────────

  @Test
  void addPendingStockByWarehouse_validatesWarehouseAndProduct() {
    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(Optional.of(destWarehouse));
    when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
    when(stockLevelRepository.addPendingStockAtomic(
      product.getId(), destWarehouse.getId(), BigDecimal.valueOf(3)))
      .thenReturn(1);

    warehouseService.addPendingStockByWarehouse(
      product.getId(),
      destWarehouse.getId(),
      BigDecimal.valueOf(3)
    );

    verify(stockLevelRepository).addPendingStockAtomic(
      product.getId(),
      destWarehouse.getId(),
      BigDecimal.valueOf(3)
    );
  }

  @Test
  void addPendingStockByWarehouse_throwsForMissingWarehouse() {
    when(warehouseRepository.findById(destWarehouse.getId())).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class,
      () -> warehouseService.addPendingStockByWarehouse(
        product.getId(), destWarehouse.getId(), BigDecimal.valueOf(3)));
    verify(stockLevelRepository, never()).addPendingStockAtomic(anyLong(), anyLong(), any());
  }
}
