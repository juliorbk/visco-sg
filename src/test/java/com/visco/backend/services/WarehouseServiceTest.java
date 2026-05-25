package com.visco.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visco.backend.models.dtos.AdjustStockRequest;
import com.visco.backend.models.dtos.CreateWarehouseRequest;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.ReceiveGoodsRequest.ReceiveItem;
import com.visco.backend.models.dtos.TransferStockRequest;
import com.visco.backend.models.dtos.WarehouseDTO;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.MovementType;
import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.InventoryMovementRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.repositories.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
class WarehouseServiceTest {

  @Mock
  private PurchaseOrderRepository purchaseOrderRepository;

  @Mock
  private GoodReceiptRepository goodReceiptRepository;

  @Mock
  private StockLevelRepository stockLevelRepository;

  @Mock
  private WarehouseRepository warehouseRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private InventoryMovementRepository inventoryMovementRepository;

  @InjectMocks
  private WarehouseService warehouseService;

  @Captor
  private ArgumentCaptor<StockLevel> stockLevelCaptor;

  @Captor
  private ArgumentCaptor<InventoryMovement> movementCaptor;

  @Captor
  private ArgumentCaptor<GoodReceipt> receiptCaptor;

  private static final Long PRODUCT_ID = 1L;
  private static final Long WAREHOUSE_ID = 1L;
  private static final Long WAREHOUSE_ID_2 = 2L;
  private static final Long ORDER_ID = 1L;
  private static final UUID USER_ID = UUID.randomUUID();
  private static final BigDecimal UNIT_PRICE = new BigDecimal("100.00");
  private static final int QUANTITY = 10;

  // ── Helpers ──────────────────────────────────────────────────────

  private User buildUser(UserRole role) {
    return User.builder()
      .id(USER_ID)
      .name("Test User")
      .email("user@test.com")
      .password("encoded")
      .role(role)
      .costCenter(CostCenter.builder().id(1L).fullDescription("Area").build())
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
      .id(1L)
      .name("Test Supplier")
      .email("supplier@test.com")
      .address("123 St")
      .currency(Currency.USD)
      .active(true)
      .build();
  }

  private Warehouse buildWarehouse() {
    return Warehouse.builder()
      .id(WAREHOUSE_ID)
      .name("Main Warehouse")
      .physicalAddress("456 Ave")
      .description("Primary")
      .active(true)
      .build();
  }

  private Warehouse buildWarehouse2() {
    return Warehouse.builder()
      .id(WAREHOUSE_ID_2)
      .name("Secondary Warehouse")
      .physicalAddress("789 Blvd")
      .description("Secondary")
      .active(true)
      .build();
  }

  private PurchaseOrder buildPurchaseOrder(PurchaseOrderStatus status) {
    Product product = buildProduct();
    User user = buildUser(UserRole.PROCUREMENT);
    Warehouse wh = buildWarehouse();
    PurchaseOrderItem item = PurchaseOrderItem.builder()
      .id(1L)
      .product(product)
      .quantity(QUANTITY)
      .unitPrice(UNIT_PRICE)
      .build();
    PurchaseOrder order = PurchaseOrder.builder()
      .id(ORDER_ID)
      .orderNumber("PO-001")
      .description("Test PO")
      .status(status)
      .supplier(buildSupplier())
      .createdBy(user)
      .destinationWarehouse(wh)
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

  private ReceiveGoodsRequest buildReceiveRequest(
    Long warehouseId,
    int qty,
    UUID receivedById
  ) {
    return new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(qty))),
      "Receiving goods",
      warehouseId,
      receivedById
    );
  }

  // ── createWarehouse ──────────────────────────────────────────────

  @Test
  void shouldCreateWarehouse_whenDataIsValid() {
    User user = buildUser(UserRole.WAREHOUSEMAN);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(warehouseRepository.save(any())).thenAnswer(inv -> {
      Warehouse w = inv.getArgument(0);
      w.setId(1L);
      return w;
    });

    var request = new CreateWarehouseRequest(
      "New WH",
      "Addr",
      "Desc",
      USER_ID,
      "SAP01"
    );
    WarehouseDTO result = warehouseService.createWarehouse(request);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("New WH");
    verify(warehouseRepository).save(any());
  }

  @Test
  void shouldThrowEntityNotFoundException_whenResponsibleUserNotFound() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    var request = new CreateWarehouseRequest(
      "New WH",
      "Addr",
      "Desc",
      USER_ID,
      "SAP01"
    );
    assertThatThrownBy(() -> warehouseService.createWarehouse(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("User not found");
  }

  // ── receiveGoods ────────────────────────────────────────────────

  @Test
  void shouldReceiveGoodsAndMarkDelivered_whenAllItemsFullyReceived() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    User user = buildUser(UserRole.WAREHOUSEMAN);
    StockLevel existingStock = buildStockLevel(
      BigDecimal.ZERO,
      BigDecimal.valueOf(QUANTITY)
    );

    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(dest)
    );
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(
      List.of()
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(existingStock));
    when(purchaseOrderRepository.save(any())).thenReturn(order);
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });

    GoodReceiptResponse response = warehouseService.receiveGoods(
      ORDER_ID,
      buildReceiveRequest(WAREHOUSE_ID, QUANTITY, USER_ID)
    );

    assertThat(response).isNotNull();
    assertThat(response.updatedStatus()).isEqualTo(
      PurchaseOrderStatus.DELIVERED
    );
    assertThat(existingStock.getCurrentStock()).isEqualByComparingTo(
      BigDecimal.valueOf(QUANTITY)
    );
    assertThat(existingStock.getPendingStock()).isEqualByComparingTo("0");

    verify(goodReceiptRepository).save(any());
    verify(purchaseOrderRepository).save(
      argThat(o -> o.getStatus() == PurchaseOrderStatus.DELIVERED)
    );
    verify(inventoryMovementRepository).save(movementCaptor.capture());
    assertThat(movementCaptor.getValue().getType()).isEqualTo(
      MovementType.INPUT
    );
  }

  @Test
  void shouldReceiveGoodsAndMarkPartiallyDelivered_whenNotAllItemsReceived() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    User user = buildUser(UserRole.WAREHOUSEMAN);
    StockLevel existingStock = buildStockLevel(
      BigDecimal.ZERO,
      BigDecimal.valueOf(QUANTITY)
    );

    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(dest)
    );
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(
      List.of()
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(existingStock));
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    GoodReceiptResponse response = warehouseService.receiveGoods(
      ORDER_ID,
      buildReceiveRequest(WAREHOUSE_ID, 3, USER_ID)
    );

    assertThat(response.updatedStatus()).isEqualTo(
      PurchaseOrderStatus.PARTIALLY_DELIVERED
    );
    assertThat(existingStock.getCurrentStock()).isEqualByComparingTo("3");
    verify(purchaseOrderRepository).save(
      argThat(o -> o.getStatus() == PurchaseOrderStatus.PARTIALLY_DELIVERED)
    );
  }

  @Test
  void shouldReceiveGoodsAndMarkDelivered_whenCumulativeReceiptsCompleteOrder() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    User user = buildUser(UserRole.WAREHOUSEMAN);
    StockLevel existingStock = buildStockLevel(
      BigDecimal.ZERO,
      BigDecimal.valueOf(QUANTITY)
    );

    GoodReceipt prevReceipt = GoodReceipt.builder()
      .id(1L)
      .purchaseOrder(order)
      .build();
    GoodReceiptItem prevItem = GoodReceiptItem.builder()
      .id(1L)
      .goodReceipt(prevReceipt)
      .product(buildProduct())
      .expectedQuantity(BigDecimal.valueOf(QUANTITY))
      .receivedQuantity(BigDecimal.valueOf(7))
      .build();
    prevReceipt.setItems(List.of(prevItem));

    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(dest)
    );
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(
      List.of(prevReceipt)
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(existingStock));
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(2L);
      return gr;
    });
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    GoodReceiptResponse response = warehouseService.receiveGoods(
      ORDER_ID,
      buildReceiveRequest(WAREHOUSE_ID, 3, USER_ID)
    );

    assertThat(response.updatedStatus()).isEqualTo(
      PurchaseOrderStatus.DELIVERED
    );
    assertThat(existingStock.getCurrentStock()).isEqualByComparingTo("3");
    verify(purchaseOrderRepository).save(
      argThat(o -> o.getStatus() == PurchaseOrderStatus.DELIVERED)
    );
  }

  @Test
  void shouldThrowEntityNotFoundException_whenPurchaseOrderNotFound() {
    when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(
        99L,
        buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID)
      )
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Purchase order not found");
  }

  @Test
  void shouldThrowIllegalStateException_whenOrderIsDelivered() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(
        ORDER_ID,
        buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID)
      )
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot receive goods");
  }

  @Test
  void shouldThrowIllegalStateException_whenOrderIsCancelled() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.CANCELLED);
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(
        ORDER_ID,
        buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID)
      )
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot receive goods");
  }

  @Test
  void shouldThrowIllegalStateException_whenOrderIsRejected() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.REJECTED);
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(
        ORDER_ID,
        buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID)
      )
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot receive goods");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenReceiveWarehouseNotFound() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(
        ORDER_ID,
        buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID)
      )
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Warehouse not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenProductNotInPurchaseOrder() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(dest)
    );
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(
      List.of()
    );
    when(userRepository.findById(USER_ID)).thenReturn(
      Optional.of(buildUser(UserRole.WAREHOUSEMAN))
    );
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });

    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(99L, BigDecimal.valueOf(5))),
      "Notes",
      WAREHOUSE_ID,
      USER_ID
    );

    assertThatThrownBy(() -> warehouseService.receiveGoods(ORDER_ID, request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Product not found in order");
  }

  @Test
  void shouldReceiveGoods_withReceivedByFallbackToOrderCreator() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    User creator = order.getCreatedBy();
    StockLevel existingStock = buildStockLevel(
      BigDecimal.ZERO,
      BigDecimal.valueOf(QUANTITY)
    );

    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(dest)
    );
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(
      List.of()
    );
    when(userRepository.findById(creator.getId())).thenReturn(
      Optional.of(creator)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(existingStock));
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(QUANTITY))),
      "Notes",
      WAREHOUSE_ID,
      null
    );

    GoodReceiptResponse response = warehouseService.receiveGoods(
      ORDER_ID,
      request
    );

    assertThat(response).isNotNull();
    assertThat(response.updatedStatus()).isEqualTo(
      PurchaseOrderStatus.DELIVERED
    );
  }

  @Test
  void shouldCreateNewStockLevel_whenNoneExists() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(
      Optional.of(order)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(dest)
    );
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(
      List.of()
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.empty());
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });
    when(stockLevelRepository.save(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    GoodReceiptResponse response = warehouseService.receiveGoods(
      ORDER_ID,
      buildReceiveRequest(WAREHOUSE_ID, QUANTITY, USER_ID)
    );

    assertThat(response).isNotNull();
    verify(stockLevelRepository, times(2)).save(stockLevelCaptor.capture());
    StockLevel saved = stockLevelCaptor.getAllValues().get(1);
    assertThat(saved.getCurrentStock()).isEqualByComparingTo(
      BigDecimal.valueOf(QUANTITY)
    );
  }

  // ── determineIfFullyReceived ─────────────────────────────────────

  @Test
  void shouldReturnTrue_whenAllItemsFullyReceived() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    var previousReceived = new HashMap<Long, BigDecimal>();
    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(QUANTITY))),
      "Notes",
      WAREHOUSE_ID,
      USER_ID
    );

    boolean result = warehouseService.determineIfFullyReceived(
      order,
      previousReceived,
      request
    );

    assertThat(result).isTrue();
  }

  @Test
  void shouldReturnFalse_whenNotAllItemsFullyReceived() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    var previousReceived = new HashMap<Long, BigDecimal>();
    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(3))),
      "Notes",
      WAREHOUSE_ID,
      USER_ID
    );

    boolean result = warehouseService.determineIfFullyReceived(
      order,
      previousReceived,
      request
    );

    assertThat(result).isFalse();
  }

  @Test
  void shouldReturnTrue_whenCumulativeReceiptsCompleteAllItems() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    var previousReceived = new HashMap<Long, BigDecimal>();
    previousReceived.put(PRODUCT_ID, BigDecimal.valueOf(7));
    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(3))),
      "Notes",
      WAREHOUSE_ID,
      USER_ID
    );

    boolean result = warehouseService.determineIfFullyReceived(
      order,
      previousReceived,
      request
    );

    assertThat(result).isTrue();
  }

  // ── transferStock ───────────────────────────────────────────────

  @Test
  void shouldTransferStock_whenSufficientStockExists() {
    Warehouse from = buildWarehouse();
    Warehouse to = buildWarehouse2();
    Product product = buildProduct();
    User user = buildUser(UserRole.WAREHOUSEMAN);
    StockLevel sourceStock = StockLevel.builder()
      .id(1L)
      .product(product)
      .warehouse(from)
      .currentStock(BigDecimal.valueOf(50))
      .pendingStock(BigDecimal.ZERO)
      .build();
    StockLevel destStock = StockLevel.builder()
      .id(2L)
      .product(product)
      .warehouse(to)
      .currentStock(BigDecimal.valueOf(10))
      .pendingStock(BigDecimal.ZERO)
      .build();

    var request = new TransferStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      WAREHOUSE_ID_2,
      BigDecimal.valueOf(20),
      "Reallocation",
      USER_ID,
      UNIT_PRICE
    );

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(from)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(
      Optional.of(to)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(sourceStock));
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID_2
      )
    ).thenReturn(Optional.of(destStock));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    warehouseService.transferStock(request);

    assertThat(sourceStock.getCurrentStock()).isEqualByComparingTo("30");
    assertThat(destStock.getCurrentStock()).isEqualByComparingTo("30");
    verify(stockLevelRepository).save(sourceStock);
    verify(stockLevelRepository).save(destStock);
    verify(inventoryMovementRepository).save(movementCaptor.capture());
    assertThat(movementCaptor.getValue().getType()).isEqualTo(
      MovementType.TRANSFER
    );
    assertThat(movementCaptor.getValue().getFromWarehouse()).isEqualTo(from);
    assertThat(movementCaptor.getValue().getToWarehouse()).isEqualTo(to);
  }

  @Test
  void shouldCreateNewStockLevelInDestination_whenNoExistingStock() {
    Warehouse from = buildWarehouse();
    Warehouse to = buildWarehouse2();
    Product product = buildProduct();
    User user = buildUser(UserRole.WAREHOUSEMAN);
    StockLevel sourceStock = StockLevel.builder()
      .id(1L)
      .product(product)
      .warehouse(from)
      .currentStock(BigDecimal.valueOf(50))
      .pendingStock(BigDecimal.ZERO)
      .build();

    var request = new TransferStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      WAREHOUSE_ID_2,
      BigDecimal.valueOf(20),
      "Reallocation",
      USER_ID,
      UNIT_PRICE
    );

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(from)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(
      Optional.of(to)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(sourceStock));
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID_2
      )
    ).thenReturn(Optional.empty());
    when(stockLevelRepository.save(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    warehouseService.transferStock(request);

    verify(stockLevelRepository).save(sourceStock);
    // Verify the newly created destination stock
    ArgumentCaptor<StockLevel> captorForDest = ArgumentCaptor.forClass(
      StockLevel.class
    );
    verify(stockLevelRepository, times(3)).save(captorForDest.capture());
    StockLevel newDest = captorForDest
      .getAllValues()
      .stream()
      .filter(sl -> sl.getWarehouse().equals(to))
      .findFirst()
      .orElseThrow();
    assertThat(newDest.getCurrentStock()).isEqualByComparingTo("20");
    assertThat(newDest.getWarehouse()).isEqualTo(to);
  }

  @Test
  void shouldThrowEntityNotFoundException_whenSourceWarehouseNotFound() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.empty()
    );

    var request = new TransferStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      WAREHOUSE_ID_2,
      BigDecimal.TEN,
      "Test",
      USER_ID,
      null
    );

    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Source warehouse not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenDestinationWarehouseNotFound() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(buildWarehouse())
    );
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(
      Optional.empty()
    );

    var request = new TransferStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      WAREHOUSE_ID_2,
      BigDecimal.TEN,
      "Test",
      USER_ID,
      null
    );

    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Destination warehouse not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenSourceStockLevelNotFound() {
    Warehouse from = buildWarehouse();
    Warehouse to = buildWarehouse2();
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(from)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(
      Optional.of(to)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.empty());

    var request = new TransferStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      WAREHOUSE_ID_2,
      BigDecimal.TEN,
      "Test",
      USER_ID,
      null
    );

    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Stock level not found for source warehouse");
  }

  @Test
  void shouldThrowIllegalArgumentException_whenInsufficientStock() {
    Warehouse from = buildWarehouse();
    Warehouse to = buildWarehouse2();
    Product product = buildProduct();
    StockLevel sourceStock = StockLevel.builder()
      .id(1L)
      .product(product)
      .warehouse(from)
      .currentStock(BigDecimal.valueOf(5))
      .pendingStock(BigDecimal.ZERO)
      .build();

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(from)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(
      Optional.of(to)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(sourceStock));

    var request = new TransferStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      WAREHOUSE_ID_2,
      BigDecimal.valueOf(20),
      "Test",
      USER_ID,
      null
    );

    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Insufficient stock");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenUserNotFoundForTransfer() {
    Warehouse from = buildWarehouse();
    Warehouse to = buildWarehouse2();
    Product product = buildProduct();
    StockLevel sourceStock = StockLevel.builder()
      .id(1L)
      .product(product)
      .warehouse(from)
      .currentStock(BigDecimal.valueOf(50))
      .pendingStock(BigDecimal.ZERO)
      .build();

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(from)
    );
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(
      Optional.of(to)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(sourceStock));
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID_2
      )
    ).thenReturn(
      Optional.of(
        StockLevel.builder()
          .id(2L)
          .product(product)
          .warehouse(to)
          .currentStock(BigDecimal.ZERO)
          .pendingStock(BigDecimal.ZERO)
          .build()
      )
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    var request = new TransferStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      WAREHOUSE_ID_2,
      BigDecimal.TEN,
      "Test",
      USER_ID,
      null
    );

    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("User not found");
  }

  // ── adjustStock ─────────────────────────────────────────────────

  @Test
  void shouldAdjustStock_whenDataIsValid() {
    Warehouse wh = buildWarehouse();
    Product product = buildProduct();
    User user = buildUser(UserRole.WAREHOUSEMAN);
    StockLevel stock = StockLevel.builder()
      .id(1L)
      .product(product)
      .warehouse(wh)
      .currentStock(BigDecimal.valueOf(50))
      .pendingStock(BigDecimal.ZERO)
      .build();

    var request = new AdjustStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(80),
      "Correction",
      USER_ID,
      UNIT_PRICE
    );

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(wh)
    );
    when(productRepository.findById(PRODUCT_ID)).thenReturn(
      Optional.of(product)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(stock));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    warehouseService.adjustStock(request);

    assertThat(stock.getCurrentStock()).isEqualByComparingTo("80");
    verify(stockLevelRepository).save(stock);
    verify(inventoryMovementRepository).save(movementCaptor.capture());
    InventoryMovement movement = movementCaptor.getValue();
    assertThat(movement.getType()).isEqualTo(MovementType.ADJUSTMENT);
    assertThat(movement.getQuantity()).isEqualByComparingTo("30");
  }

  @Test
  void shouldAdjustStockDecrease_whenNewStockIsLower() {
    Warehouse wh = buildWarehouse();
    Product product = buildProduct();
    User user = buildUser(UserRole.WAREHOUSEMAN);
    StockLevel stock = StockLevel.builder()
      .id(1L)
      .product(product)
      .warehouse(wh)
      .currentStock(BigDecimal.valueOf(50))
      .pendingStock(BigDecimal.ZERO)
      .build();

    var request = new AdjustStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(30),
      "Reduce",
      USER_ID,
      null
    );

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(wh)
    );
    when(productRepository.findById(PRODUCT_ID)).thenReturn(
      Optional.of(product)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(stock));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    warehouseService.adjustStock(request);

    assertThat(stock.getCurrentStock()).isEqualByComparingTo("30");
    verify(inventoryMovementRepository).save(movementCaptor.capture());
    assertThat(movementCaptor.getValue().getQuantity()).isEqualByComparingTo(
      "-20"
    );
  }

  @Test
  void shouldThrowEntityNotFoundException_whenWarehouseNotFoundForAdjust() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.empty()
    );

    var request = new AdjustStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(80),
      "Test",
      USER_ID,
      null
    );

    assertThatThrownBy(() -> warehouseService.adjustStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Warehouse not found");
  }

  @Test
  void shouldCreateStockLevel_whenNoneExistsForAdjust() {
    Warehouse wh = buildWarehouse();
    Product product = buildProduct();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(wh)
    );
    when(productRepository.findById(PRODUCT_ID)).thenReturn(
      Optional.of(product)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.empty());
    when(stockLevelRepository.save(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    var request = new AdjustStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(80),
      "Test",
      USER_ID,
      null
    );

    warehouseService.adjustStock(request);

    verify(stockLevelRepository, times(2)).save(stockLevelCaptor.capture());
    assertThat(
      stockLevelCaptor.getAllValues().get(1).getCurrentStock()
    ).isEqualByComparingTo("80");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenUserNotFoundForAdjust() {
    Warehouse wh = buildWarehouse();
    Product product = buildProduct();
    StockLevel stock = StockLevel.builder()
      .id(1L)
      .product(product)
      .warehouse(wh)
      .currentStock(BigDecimal.valueOf(50))
      .pendingStock(BigDecimal.ZERO)
      .build();

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(wh)
    );
    when(productRepository.findById(PRODUCT_ID)).thenReturn(
      Optional.of(product)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(stock));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    var request = new AdjustStockRequest(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(80),
      "Test",
      USER_ID,
      null
    );

    assertThatThrownBy(() -> warehouseService.adjustStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("User not found");
  }

  // ── addPendingStockByWarehouse ──────────────────────────────────

  @Test
  void shouldAddPendingStock_whenStockLevelExists() {
    Warehouse wh = buildWarehouse();
    Product product = buildProduct();
    StockLevel stock = buildStockLevel(BigDecimal.ZERO, BigDecimal.ZERO);

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(wh)
    );
    when(productRepository.findById(PRODUCT_ID)).thenReturn(
      Optional.of(product)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(stock));

    warehouseService.addPendingStockByWarehouse(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(10)
    );

    assertThat(stock.getPendingStock()).isEqualByComparingTo("10");
    verify(stockLevelRepository).save(stock);
  }

  @Test
  void shouldAddPendingStockAndCreateNewStockLevel_whenNoneExists() {
    Warehouse wh = buildWarehouse();
    Product product = buildProduct();

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(wh)
    );
    when(productRepository.findById(PRODUCT_ID)).thenReturn(
      Optional.of(product)
    );
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.empty());
    when(stockLevelRepository.save(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );

    warehouseService.addPendingStockByWarehouse(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(10)
    );

    verify(stockLevelRepository, times(2)).save(stockLevelCaptor.capture());
    assertThat(
      stockLevelCaptor.getAllValues().get(1).getPendingStock()
    ).isEqualByComparingTo("10");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenWarehouseNotFoundForPendingStock() {
    when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      warehouseService.addPendingStockByWarehouse(
        PRODUCT_ID,
        99L,
        BigDecimal.TEN
      )
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Warehouse not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenProductNotFoundForPendingStock() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(buildWarehouse())
    );
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      warehouseService.addPendingStockByWarehouse(
        PRODUCT_ID,
        WAREHOUSE_ID,
        BigDecimal.TEN
      )
    )
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Product not found");
  }

  // ── addCurrentStock ─────────────────────────────────────────────

  @Test
  void shouldAddCurrentStock_whenStockLevelExists() {
    StockLevel stock = buildStockLevel(BigDecimal.valueOf(50), BigDecimal.ZERO);
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(stock));

    warehouseService.addCurrentStock(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(10)
    );

    assertThat(stock.getCurrentStock()).isEqualByComparingTo("60");
    verify(stockLevelRepository).save(stock);
  }

  @Test
  void shouldDoNothing_whenStockLevelNotFoundForAddCurrent() {
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.empty());

    warehouseService.addCurrentStock(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(10)
    );

    verify(stockLevelRepository, never()).save(any());
  }

  // ── substractCurrentStock ───────────────────────────────────────

  @Test
  void shouldSubstractCurrentStock_whenSufficientStock() {
    StockLevel stock = buildStockLevel(BigDecimal.valueOf(50), BigDecimal.ZERO);
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(stock));

    warehouseService.substractCurrentStock(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(20)
    );

    assertThat(stock.getCurrentStock()).isEqualByComparingTo("30");
    verify(stockLevelRepository).save(stock);
  }

  @Test
  void shouldThrowIllegalArgumentException_whenInsufficientStockForSubstract() {
    StockLevel stock = buildStockLevel(BigDecimal.valueOf(5), BigDecimal.ZERO);
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(stock));

    assertThatThrownBy(() ->
      warehouseService.substractCurrentStock(
        PRODUCT_ID,
        WAREHOUSE_ID,
        BigDecimal.valueOf(20)
      )
    )
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Insufficient stock");
  }

  @Test
  void shouldDoNothing_whenStockLevelNotFoundForSubstract() {
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.empty());

    warehouseService.substractCurrentStock(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(10)
    );

    verify(stockLevelRepository, never()).save(any());
  }

  // ── substractPendingStock ───────────────────────────────────────

  @Test
  void shouldSubstractPendingStock_whenSufficient() {
    StockLevel stock = buildStockLevel(BigDecimal.ZERO, BigDecimal.valueOf(20));
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(stock));

    warehouseService.substractPendingStock(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(10)
    );

    assertThat(stock.getPendingStock()).isEqualByComparingTo("10");
    verify(stockLevelRepository).save(stock);
  }

  @Test
  void shouldClampPendingStockToZero_whenSubstractWouldMakeNegative() {
    StockLevel stock = buildStockLevel(BigDecimal.ZERO, BigDecimal.valueOf(5));
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.of(stock));

    warehouseService.substractPendingStock(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(20)
    );

    assertThat(stock.getPendingStock()).isEqualByComparingTo("0");
    verify(stockLevelRepository).save(stock);
  }

  @Test
  void shouldDoNothing_whenStockLevelNotFoundForSubstractPending() {
    when(
      stockLevelRepository.findByProductIdAndWarehouseId(
        PRODUCT_ID,
        WAREHOUSE_ID
      )
    ).thenReturn(Optional.empty());

    warehouseService.substractPendingStock(
      PRODUCT_ID,
      WAREHOUSE_ID,
      BigDecimal.valueOf(10)
    );

    verify(stockLevelRepository, never()).save(any());
  }

  // ── getWarehouse ────────────────────────────────────────────────

  @Test
  void shouldGetWarehouse_whenExists() {
    Warehouse wh = buildWarehouse();
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(
      Optional.of(wh)
    );

    WarehouseDTO result = warehouseService.getWarehouse(WAREHOUSE_ID);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Main Warehouse");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenWarehouseNotFound() {
    when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> warehouseService.getWarehouse(99L))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Warehouse not found");
  }

  // ── getReceiptById ──────────────────────────────────────────────

  @Test
  void shouldGetReceiptById_whenExists() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
    GoodReceipt receipt = GoodReceipt.builder()
      .id(1L)
      .receiptNumber("VIS-1-12345-abcdef")
      .purchaseOrder(order)
      .receivedAt(LocalDateTime.now())
      .build();
    GoodReceiptItem item = GoodReceiptItem.builder()
      .id(1L)
      .goodReceipt(receipt)
      .product(buildProduct())
      .expectedQuantity(BigDecimal.TEN)
      .receivedQuantity(BigDecimal.TEN)
      .build();
    receipt.setItems(List.of(item));

    when(goodReceiptRepository.findById(1L)).thenReturn(Optional.of(receipt));

    GoodReceiptResponse result = warehouseService.getReceiptById(1L);

    assertThat(result).isNotNull();
    assertThat(result.receiptNumber()).isEqualTo("VIS-1-12345-abcdef");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenReceiptNotFound() {
    when(goodReceiptRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> warehouseService.getReceiptById(99L))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Receipt not found");
  }
}
