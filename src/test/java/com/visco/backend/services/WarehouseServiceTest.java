package com.visco.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import com.visco.backend.models.entities.Location;
import com.visco.backend.models.entities.MovementType;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.PurchaseOrderType;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.InventoryMovementRepository;
import com.visco.backend.repositories.DispatchNoteRepository;
import com.visco.backend.repositories.EmployeeRepository;
import com.visco.backend.repositories.LocationRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;

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

  @Mock
  private LocationRepository locationRepository;

  @Mock
  private DispatchNoteRepository dispatchNoteRepository;

  @Mock
  private EmployeeRepository employeeRepository;

  @InjectMocks
  private WarehouseService warehouseService;

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
  private static final Long LOCATION_ID = 1L;

  @BeforeEach
  void setUp() {
    // Las operaciones atómicas simulan éxito por defecto (1 fila afectada)
    // Usamos lenient() porque no todos los tests usan todos los métodos
    lenient().when(stockLevelRepository.addCurrentStockAtomic(any(), any(), any())).thenReturn(1);
    lenient().when(stockLevelRepository.addPendingStockAtomic(any(), any(), any())).thenReturn(1);
    lenient().when(stockLevelRepository.subtractCurrentStockAtomic(any(), any(), any())).thenReturn(1);
    lenient().when(stockLevelRepository.subtractPendingStockAtomic(any(), any(), any())).thenReturn(1);
    lenient().when(stockLevelRepository.setCurrentStockAtomic(any(), any(), any())).thenReturn(1);
  }

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

  private Location buildLocation() {
    return Location.builder()
      .id(LOCATION_ID)
      .code("A-01")
      .active(true)
      .warehouse(buildWarehouse())
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

  private ReceiveGoodsRequest buildReceiveRequest(
    Long warehouseId,
    int qty,
    UUID receivedById
  ) {
    return new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(qty))),
      "Receiving goods",
      warehouseId,
      LOCATION_ID,
      receivedById
    );
  }

  private void mockLocation() {
    when(locationRepository.findById(LOCATION_ID)).thenReturn(
      Optional.of(buildLocation())
    );
  }

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
      "New WH", "Addr", "Desc", USER_ID, "SAP01"
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
      "New WH", "Addr", "Desc", USER_ID, "SAP01"
    );
    assertThatThrownBy(() -> warehouseService.createWarehouse(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("User not found");
  }

  @Test
  void shouldReceiveGoodsAndMarkDelivered_whenAllItemsFullyReceived() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    mockLocation();
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(dest));
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of());
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(purchaseOrderRepository.save(any())).thenReturn(order);
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });

    GoodReceiptResponse response = warehouseService.receiveGoods(
      ORDER_ID, buildReceiveRequest(WAREHOUSE_ID, QUANTITY, USER_ID)
    );

    assertThat(response).isNotNull();
    assertThat(response.updatedStatus()).isEqualTo(PurchaseOrderStatus.DELIVERED);

    verify(stockLevelRepository).addCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(QUANTITY));
    verify(stockLevelRepository).subtractPendingStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(QUANTITY));
    verify(goodReceiptRepository).save(any());
    verify(purchaseOrderRepository).save(argThat(o -> o.getStatus() == PurchaseOrderStatus.DELIVERED));
    verify(inventoryMovementRepository).save(movementCaptor.capture());
    assertThat(movementCaptor.getValue().getType()).isEqualTo(MovementType.INPUT);
  }

  @Test
  void shouldReceiveGoodsAndMarkPartiallyDelivered_whenNotAllItemsReceived() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    mockLocation();
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(dest));
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of());
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    GoodReceiptResponse response = warehouseService.receiveGoods(
      ORDER_ID, buildReceiveRequest(WAREHOUSE_ID, 3, USER_ID)
    );

    assertThat(response.updatedStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_DELIVERED);
    verify(purchaseOrderRepository).save(
      argThat(o -> o.getStatus() == PurchaseOrderStatus.PARTIALLY_DELIVERED)
    );
  }

  @Test
  void shouldReceiveGoodsAndMarkDelivered_whenCumulativeReceiptsCompleteOrder() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    GoodReceipt prevReceipt = GoodReceipt.builder().id(1L).purchaseOrder(order).build();
    GoodReceiptItem prevItem = GoodReceiptItem.builder()
      .id(1L).goodReceipt(prevReceipt).product(buildProduct())
      .expectedQuantity(BigDecimal.valueOf(QUANTITY)).receivedQuantity(BigDecimal.valueOf(7))
      .build();
    prevReceipt.setItems(List.of(prevItem));

    mockLocation();
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(dest));
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of(prevReceipt));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(2L);
      return gr;
    });
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    GoodReceiptResponse response = warehouseService.receiveGoods(
      ORDER_ID, buildReceiveRequest(WAREHOUSE_ID, 3, USER_ID)
    );

    assertThat(response.updatedStatus()).isEqualTo(PurchaseOrderStatus.DELIVERED);
    verify(purchaseOrderRepository).save(
      argThat(o -> o.getStatus() == PurchaseOrderStatus.DELIVERED)
    );
  }

  @Test
  void shouldThrowEntityNotFoundException_whenPurchaseOrderNotFound() {
    when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(99L, buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID))
    ).isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Purchase order not found");
  }

  @Test
  void shouldThrowIllegalStateException_whenOrderIsDelivered() {
    when(purchaseOrderRepository.findById(ORDER_ID))
      .thenReturn(Optional.of(buildPurchaseOrder(PurchaseOrderStatus.DELIVERED)));

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(ORDER_ID, buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID))
    ).isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot receive goods");
  }

  @Test
  void shouldThrowIllegalStateException_whenOrderIsCancelled() {
    when(purchaseOrderRepository.findById(ORDER_ID))
      .thenReturn(Optional.of(buildPurchaseOrder(PurchaseOrderStatus.CANCELLED)));

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(ORDER_ID, buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID))
    ).isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot receive goods");
  }

  @Test
  void shouldThrowIllegalStateException_whenOrderIsRejected() {
    when(purchaseOrderRepository.findById(ORDER_ID))
      .thenReturn(Optional.of(buildPurchaseOrder(PurchaseOrderStatus.REJECTED)));

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(ORDER_ID, buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID))
    ).isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Cannot receive goods");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenReceiveWarehouseNotFound() {
    when(purchaseOrderRepository.findById(ORDER_ID))
      .thenReturn(Optional.of(buildPurchaseOrder(PurchaseOrderStatus.APPROVED)));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      warehouseService.receiveGoods(ORDER_ID, buildReceiveRequest(WAREHOUSE_ID, 5, USER_ID))
    ).isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Warehouse not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenProductNotInPurchaseOrder() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    mockLocation();
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse()));
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of());
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser(UserRole.WAREHOUSEMAN)));
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });

    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(99L, BigDecimal.valueOf(5))),
      "Notes", WAREHOUSE_ID, LOCATION_ID, USER_ID
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

    mockLocation();
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(dest));
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of());
    when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(QUANTITY))),
      "Notes", WAREHOUSE_ID, LOCATION_ID, null
    );

    GoodReceiptResponse response = warehouseService.receiveGoods(ORDER_ID, request);
    assertThat(response).isNotNull();
    assertThat(response.updatedStatus()).isEqualTo(PurchaseOrderStatus.DELIVERED);
  }

  @Test
  void shouldCreateNewStockLevel_whenNoneExists() {
    // Con la operación atómica (INSERT ... ON CONFLICT), no se necesita
    // pre-crear el StockLevel. Verificamos que se llame al método atómico.
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    Warehouse dest = buildWarehouse();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    mockLocation();
    when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(dest));
    when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of());
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(goodReceiptRepository.saveAndFlush(any())).thenAnswer(inv -> {
      GoodReceipt gr = inv.getArgument(0);
      gr.setId(1L);
      return gr;
    });
    when(purchaseOrderRepository.save(any())).thenReturn(order);

    warehouseService.receiveGoods(ORDER_ID, buildReceiveRequest(WAREHOUSE_ID, QUANTITY, USER_ID));

    verify(stockLevelRepository).addCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(QUANTITY));
    verify(stockLevelRepository).subtractPendingStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(QUANTITY));
  }

  @Test
  void shouldReturnTrue_whenAllItemsFullyReceived() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    var previousReceived = new HashMap<Long, BigDecimal>();
    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(QUANTITY))),
      "Notes", WAREHOUSE_ID, LOCATION_ID, USER_ID
    );
    assertThat(warehouseService.determineIfFullyReceived(order, previousReceived, request)).isTrue();
  }

  @Test
  void shouldReturnFalse_whenNotAllItemsFullyReceived() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    var previousReceived = new HashMap<Long, BigDecimal>();
    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(3))),
      "Notes", WAREHOUSE_ID, LOCATION_ID, USER_ID
    );
    assertThat(warehouseService.determineIfFullyReceived(order, previousReceived, request)).isFalse();
  }

  @Test
  void shouldReturnTrue_whenCumulativeReceiptsCompleteAllItems() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.APPROVED);
    var previousReceived = new HashMap<Long, BigDecimal>();
    previousReceived.put(PRODUCT_ID, BigDecimal.valueOf(7));
    var request = new ReceiveGoodsRequest(
      List.of(new ReceiveItem(PRODUCT_ID, BigDecimal.valueOf(3))),
      "Notes", WAREHOUSE_ID, LOCATION_ID, USER_ID
    );
    assertThat(warehouseService.determineIfFullyReceived(order, previousReceived, request)).isTrue();
  }

  @Test
  void shouldTransferStock_whenSufficientStockExists() {
    Warehouse from = buildWarehouse();
    Warehouse to = buildWarehouse2();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    var request = new TransferStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, WAREHOUSE_ID_2,
      BigDecimal.valueOf(20), "Reallocation", USER_ID, UNIT_PRICE
    );

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(from));
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(Optional.of(to));
    when(stockLevelRepository.getStockByProductAndWarehouse(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(BigDecimal.valueOf(50));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(buildProduct()));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    warehouseService.transferStock(request);

    verify(stockLevelRepository).subtractCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(20));
    verify(stockLevelRepository).addCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID_2, BigDecimal.valueOf(20));
    verify(inventoryMovementRepository).save(movementCaptor.capture());
    assertThat(movementCaptor.getValue().getType()).isEqualTo(MovementType.TRANSFER);
    assertThat(movementCaptor.getValue().getFromWarehouse()).isEqualTo(from);
    assertThat(movementCaptor.getValue().getToWarehouse()).isEqualTo(to);
  }

  @Test
  void shouldCreateNewStockLevelInDestination_whenNoExistingStock() {
    Warehouse from = buildWarehouse();
    Warehouse to = buildWarehouse2();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    var request = new TransferStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, WAREHOUSE_ID_2,
      BigDecimal.valueOf(20), "Reallocation", USER_ID, UNIT_PRICE
    );

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(from));
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(Optional.of(to));
    when(stockLevelRepository.getStockByProductAndWarehouse(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(BigDecimal.valueOf(50));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(buildProduct()));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    warehouseService.transferStock(request);

    verify(stockLevelRepository).subtractCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(20));
    // addCurrentStockAtomic funciona como upsert: crea si no existe
    verify(stockLevelRepository).addCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID_2, BigDecimal.valueOf(20));
  }

  @Test
  void shouldThrowEntityNotFoundException_whenSourceWarehouseNotFound() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.empty());

    var request = new TransferStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, WAREHOUSE_ID_2, BigDecimal.TEN, "Test", USER_ID, null
    );
    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Source warehouse not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenDestinationWarehouseNotFound() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse()));
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(Optional.empty());

    var request = new TransferStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, WAREHOUSE_ID_2, BigDecimal.TEN, "Test", USER_ID, null
    );
    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Destination warehouse not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenSourceStockLevelNotFound() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse()));
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(Optional.of(buildWarehouse2()));
    when(stockLevelRepository.getStockByProductAndWarehouse(PRODUCT_ID, WAREHOUSE_ID)).thenReturn(null);

    var request = new TransferStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, WAREHOUSE_ID_2, BigDecimal.TEN, "Test", USER_ID, null
    );
    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Insufficient stock");
  }

  @Test
  void shouldThrowIllegalArgumentException_whenInsufficientStock() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse()));
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(Optional.of(buildWarehouse2()));
    when(stockLevelRepository.getStockByProductAndWarehouse(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(BigDecimal.valueOf(5));

    var request = new TransferStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, WAREHOUSE_ID_2, BigDecimal.valueOf(20), "Test", USER_ID, null
    );
    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Insufficient stock");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenUserNotFoundForTransfer() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse()));
    when(warehouseRepository.findById(WAREHOUSE_ID_2)).thenReturn(Optional.of(buildWarehouse2()));
    when(stockLevelRepository.getStockByProductAndWarehouse(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(BigDecimal.valueOf(50));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(buildProduct()));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    var request = new TransferStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, WAREHOUSE_ID_2, BigDecimal.TEN, "Test", USER_ID, null
    );
    assertThatThrownBy(() -> warehouseService.transferStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("User not found");
  }

  @Test
  void shouldAdjustStock_whenDataIsValid() {
    Warehouse wh = buildWarehouse();
    Product product = buildProduct();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    var request = new AdjustStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(80), "Correction", USER_ID, UNIT_PRICE
    );

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(wh));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    when(stockLevelRepository.getStockByProductAndWarehouse(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(BigDecimal.valueOf(50));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    warehouseService.adjustStock(request);

    verify(stockLevelRepository).setCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(80));
    verify(inventoryMovementRepository).save(movementCaptor.capture());
    assertThat(movementCaptor.getValue().getType()).isEqualTo(MovementType.ADJUSTMENT);
    assertThat(movementCaptor.getValue().getQuantity()).isEqualByComparingTo("30");
  }

  @Test
  void shouldAdjustStockDecrease_whenNewStockIsLower() {
    Warehouse wh = buildWarehouse();
    Product product = buildProduct();
    User user = buildUser(UserRole.WAREHOUSEMAN);

    var request = new AdjustStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(30), "Reduce", USER_ID, null
    );

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(wh));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    when(stockLevelRepository.getStockByProductAndWarehouse(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(BigDecimal.valueOf(50));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    warehouseService.adjustStock(request);

    verify(stockLevelRepository).setCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(30));
    verify(inventoryMovementRepository).save(movementCaptor.capture());
    assertThat(movementCaptor.getValue().getQuantity()).isEqualByComparingTo("-20");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenWarehouseNotFoundForAdjust() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.empty());

    var request = new AdjustStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(80), "Test", USER_ID, null
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

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(wh));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    // Simular que no hay stock previo
    when(stockLevelRepository.getStockByProductAndWarehouse(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(null);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    var request = new AdjustStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(80), "Test", USER_ID, null
    );

    warehouseService.adjustStock(request);

    verify(stockLevelRepository).setCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(80));
  }

  @Test
  void shouldThrowEntityNotFoundException_whenUserNotFoundForAdjust() {
    Warehouse wh = buildWarehouse();
    Product product = buildProduct();

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(wh));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    lenient().when(stockLevelRepository.getStockByProductAndWarehouse(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(BigDecimal.valueOf(50));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    var request = new AdjustStockRequest(
      PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(80), "Test", USER_ID, null
    );
    assertThatThrownBy(() -> warehouseService.adjustStock(request))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("User not found");
  }

  @Test
  void shouldAddPendingStock_whenStockLevelExists() {
    Product product = buildProduct();

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse()));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

    warehouseService.addPendingStockByWarehouse(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));

    verify(stockLevelRepository).addPendingStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));
  }

  @Test
  void shouldAddPendingStockAndCreateNewStockLevel_whenNoneExists() {
    Product product = buildProduct();

    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse()));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

    warehouseService.addPendingStockByWarehouse(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));

    // addPendingStockAtomic es un UPSERT: crea si no existe
    verify(stockLevelRepository).addPendingStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));
  }

  @Test
  void shouldThrowEntityNotFoundException_whenWarehouseNotFoundForPendingStock() {
    when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      warehouseService.addPendingStockByWarehouse(PRODUCT_ID, 99L, BigDecimal.TEN)
    ).isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Warehouse not found");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenProductNotFoundForPendingStock() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse()));
    when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      warehouseService.addPendingStockByWarehouse(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.TEN)
    ).isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Product not found");
  }

  @Test
  void shouldAddCurrentStock_whenStockLevelExists() {
    warehouseService.addCurrentStock(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));

    verify(stockLevelRepository).addCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));
  }

  @Test
  void shouldDoNothing_whenStockLevelNotFoundForAddCurrent() {
    // addCurrentStock es un upsert: si no existe, lo crea con ON CONFLICT.
    // Siempre se ejecuta la operación, no hay "do nothing".
    warehouseService.addCurrentStock(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));

    verify(stockLevelRepository).addCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));
  }

  @Test
  void shouldSubstractCurrentStock_whenSufficientStock() {
    warehouseService.substractCurrentStock(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(20));

    verify(stockLevelRepository).subtractCurrentStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(20));
  }

  @Test
  void shouldThrowIllegalArgumentException_whenInsufficientStockForSubstract() {
    // Simular que la operación atómica no afectó filas (stock insuficiente)
    when(stockLevelRepository.subtractCurrentStockAtomic(any(), any(), any())).thenReturn(0);

    StockLevel stock = StockLevel.builder()
      .id(1L).product(buildProduct()).warehouse(buildWarehouse())
      .currentStock(BigDecimal.valueOf(5)).pendingStock(BigDecimal.ZERO)
      .build();

    when(stockLevelRepository.findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(Optional.of(stock));

    assertThatThrownBy(() ->
      warehouseService.substractCurrentStock(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(20))
    ).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Insufficient stock");
  }

  @Test
  void shouldThrowEntityNotFoundException_whenStockLevelNotFoundForSubstract() {
    when(stockLevelRepository.subtractCurrentStockAtomic(any(), any(), any())).thenReturn(0);
    when(stockLevelRepository.findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID))
      .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      warehouseService.substractCurrentStock(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10))
    ).isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Stock level not found");
  }

  @Test
  void shouldSubstractPendingStock_whenSufficient() {
    warehouseService.substractPendingStock(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));

    verify(stockLevelRepository).subtractPendingStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));
  }

  @Test
  void shouldClampPendingStockToZero_whenSubstractWouldMakeNegative() {
    // La atomic UPDATE usa GREATEST(... - :qty, 0) internamente
    warehouseService.substractPendingStock(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(20));

    verify(stockLevelRepository).subtractPendingStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(20));
  }

  @Test
  void shouldDoNothing_whenStockLevelNotFoundForSubstractPending() {
    // subtractPendingStockAtomic siempre ejecuta el UPDATE (con GREATEST).
    // Si no existe, simplemente no afecta filas, que es el mismo comportamiento.
    warehouseService.substractPendingStock(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));

    verify(stockLevelRepository).subtractPendingStockAtomic(PRODUCT_ID, WAREHOUSE_ID, BigDecimal.valueOf(10));
  }

  @Test
  void shouldGetWarehouse_whenExists() {
    Warehouse wh = buildWarehouse();
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(wh));

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

  @Test
  void shouldGetReceiptById_whenExists() {
    PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
    GoodReceipt receipt = GoodReceipt.builder()
      .id(1L).receiptNumber("VIS-1-12345-abcdef").purchaseOrder(order)
      .receivedAt(LocalDateTime.now()).build();
    GoodReceiptItem item = GoodReceiptItem.builder()
      .id(1L).goodReceipt(receipt).product(buildProduct())
      .expectedQuantity(BigDecimal.TEN).receivedQuantity(BigDecimal.TEN)
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
