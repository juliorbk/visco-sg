package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.RequisitionRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.SupplierRepository;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.repositories.WarehouseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ProcurementService that exercise the stock-reverting
 * branches of the lifecycle: PO creation adds pending stock; PO
 * cancellation must revert ONLY the unreceived pending stock and MUST
 * NOT touch current_stock (the received goods are physically in the
 * warehouse — they are not "uncancelled" by a status change).
 */
@ExtendWith(MockitoExtension.class)
class ProcurementServiceStockTest {

  @Mock private PurchaseOrderRepository purchaseOrderRepository;
  @Mock private WarehouseRepository warehouseRepository;
  @Mock private SupplierRepository supplierRepository;
  @Mock private ProductRepository productRepository;
  @Mock private UserRepository userRepository;
  @Mock private RequisitionRepository requisitionRepository;
  @Mock private GoodReceiptRepository goodReceiptRepository;
  @Mock private StockLevelRepository stockLevelRepository;
  @Mock private WarehouseService warehouseService;

  @InjectMocks private ProcurementService procurementService;

  private Warehouse destWarehouse;
  private Product product;
  private User createdBy;
  private PurchaseOrder order;
  private PurchaseOrderItem poItem;

  @BeforeEach
  void setUp() {
    destWarehouse = Warehouse.builder()
      .id(10L)
      .name("Main Warehouse")
      .physicalAddress("Calle 1")
      .description("Main")
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
      .name("Buyer")
      .email("buyer@example.com")
      .password("x")
      .active(true)
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
      .items(new ArrayList<>(List.of(poItem)))
      .build();
    poItem.setPurchaseOrder(order);
  }

  // ─────────────────────────────────────────────────────────────
  // cancelOrderById — no debe tocar current_stock, solo el pending
  // ─────────────────────────────────────────────────────────────

  @Test
  void cancelOrderById_revertsOnlyPendingStock_doesNotTouchCurrentStock() {
    // 4 of 10 received. Pending to revert = 6. Current stock must NOT be touched.
    order.setStatus(PurchaseOrderStatus.PARTIALLY_DELIVERED);
    GoodReceiptRepository.ReceivedQuantityProjection projection =
      receipt(product.getId(), BigDecimal.valueOf(4));
    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(java.util.Optional.of(order));
    when(stockLevelRepository.getPendingStock(product.getId(), destWarehouse.getId()))
      .thenReturn(BigDecimal.valueOf(6));
    when(goodReceiptRepository.getTotalReceivedByOrder(order.getId()))
      .thenReturn(List.of(projection));
    when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    procurementService.cancelOrder(order.getId(), "buyer changed mind");

    verify(warehouseService, atLeastOnce()).substractPendingStock(
      product.getId(),
      destWarehouse.getId(),
      BigDecimal.valueOf(6)
    );
    // BUG FIX: current_stock must NOT be touched on cancellation.
    verify(warehouseService, never()).substractCurrentStock(
      anyLong(), anyLong(), any());
  }

  @Test
  void cancelOrderById_revertsFullPendingWhenNothingReceived() {
    order.setStatus(PurchaseOrderStatus.APPROVED);
    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(java.util.Optional.of(order));
    when(stockLevelRepository.getPendingStock(product.getId(), destWarehouse.getId()))
      .thenReturn(BigDecimal.valueOf(10));
    when(goodReceiptRepository.getTotalReceivedByOrder(order.getId()))
      .thenReturn(List.of());
    when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    procurementService.cancelOrder(order.getId(), "cancelled before delivery");

    verify(warehouseService, atLeastOnce()).substractPendingStock(
      product.getId(),
      destWarehouse.getId(),
      BigDecimal.valueOf(10)
    );
    verify(warehouseService, never()).substractCurrentStock(
      anyLong(), anyLong(), any());
  }

  @Test
  void cancelOrderById_revertsNothingWhenAlreadyFullyDelivered() {
    order.setStatus(PurchaseOrderStatus.PARTIALLY_DELIVERED);
    GoodReceiptRepository.ReceivedQuantityProjection projection =
      receipt(product.getId(), BigDecimal.valueOf(10));
    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(java.util.Optional.of(order));
    when(goodReceiptRepository.getTotalReceivedByOrder(order.getId()))
      .thenReturn(List.of(projection));
    when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    procurementService.cancelOrder(order.getId(), "post-delivery cancellation");

    // Nothing left to revert: 0 pending, received stock is physical → don't touch it.
    verify(warehouseService, never()).substractPendingStock(
      anyLong(), anyLong(), any());
    verify(warehouseService, never()).substractCurrentStock(
      anyLong(), anyLong(), any());
  }

  @Test
  void cancelOrderById_throwsWhenInsufficientPendingStock() {
    order.setStatus(PurchaseOrderStatus.IN_TRANSIT);
    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(java.util.Optional.of(order));
    // PO says 10 pending, but the ledger only has 5 → cannot revert 10.
    when(stockLevelRepository.getPendingStock(product.getId(), destWarehouse.getId()))
      .thenReturn(BigDecimal.valueOf(5));
    when(goodReceiptRepository.getTotalReceivedByOrder(order.getId()))
      .thenReturn(List.of());

    assertThrows(IllegalStateException.class,
      () -> procurementService.cancelOrder(order.getId(), "cancelled"));

    verify(warehouseService, never()).substractPendingStock(
      anyLong(), anyLong(), any());
  }

  @Test
  void cancelOrderById_marksOrderCancelled() {
    order.setStatus(PurchaseOrderStatus.APPROVED);
    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(java.util.Optional.of(order));
    when(stockLevelRepository.getPendingStock(product.getId(), destWarehouse.getId()))
      .thenReturn(BigDecimal.valueOf(10));
    when(goodReceiptRepository.getTotalReceivedByOrder(order.getId()))
      .thenReturn(List.of());
    when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var response = procurementService.cancelOrder(order.getId(), "changed mind");

    assertEquals(PurchaseOrderStatus.CANCELLED, response.status());
  }

  @Test
  void cancelOrderById_rejectsAlreadyCancelledOrder() {
    order.setStatus(PurchaseOrderStatus.CANCELLED);
    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(java.util.Optional.of(order));
    assertThrows(IllegalStateException.class,
      () -> procurementService.cancelOrder(order.getId(), "x"));
  }

  @Test
  void cancelOrderById_rejectsDeliveredOrder() {
    order.setStatus(PurchaseOrderStatus.DELIVERED);
    when(purchaseOrderRepository.findByIdDetailed(order.getId()))
      .thenReturn(java.util.Optional.of(order));
    assertThrows(IllegalStateException.class,
      () -> procurementService.cancelOrder(order.getId(), "x"));
  }

  // ─────────────────────────────────────────────────────────────
  // createPurchaseOrder — agrega pending stock
  // ─────────────────────────────────────────────────────────────

  @Test
  void createPurchaseOrder_addsPendingStockForEachItem() {
    var request = new com.visco.backend.models.dtos.CreatePurchaseOrderRequest(
      "PO-9999",
      "Description",
      1L, // supplierId
      destWarehouse.getId(),
      PaymentMethod.CASH,
      PurchaseOrderType.MATERIALS,
      createdBy.getId(),
      null, // requisitionId
      null, // leadTime
      null, // shipConditions
      null, // incoterm
      List.of(new com.visco.backend.models.dtos.PurchaseOrderItemRequest(
        product.getId(), BigDecimal.valueOf(7), BigDecimal.valueOf(2)
      ))
    );

    when(supplierRepository.findById(1L)).thenReturn(java.util.Optional.of(
      com.visco.backend.models.entities.Supplier.builder()
        .id(1L).name("S1").address("a").email("s1@x").description("d")
        .currency(com.visco.backend.models.entities.Currency.USD)
        .active(true).build()
    ));
    when(userRepository.findById(createdBy.getId())).thenReturn(java.util.Optional.of(createdBy));
    when(warehouseRepository.findById(destWarehouse.getId()))
      .thenReturn(java.util.Optional.of(destWarehouse));
    when(productRepository.findAllById(List.of(product.getId())))
      .thenReturn(List.of(product));
    when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    procurementService.createPurchaseOrder(request);

    verify(warehouseService, atLeastOnce()).addPendingStockByWarehouse(
      product.getId(), destWarehouse.getId(), BigDecimal.valueOf(7));
  }

  // ─────────────────────────────────────────────────────────────
  // helpers
  // ─────────────────────────────────────────────────────────────

  private GoodReceiptRepository.ReceivedQuantityProjection receipt(
    Long productId,
    BigDecimal totalReceived
  ) {
    GoodReceiptRepository.ReceivedQuantityProjection p =
      org.mockito.Mockito.mock(GoodReceiptRepository.ReceivedQuantityProjection.class);
    when(p.getProductId()).thenReturn(productId);
    when(p.getTotalReceived()).thenReturn(totalReceived);
    return p;
  }
}
