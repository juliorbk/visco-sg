package com.visco.backend.services;

import com.visco.backend.models.dtos.AdjustStockRequest;
import com.visco.backend.models.dtos.CreateWarehouseRequest;
import com.visco.backend.models.dtos.DispatchItemResponse;
import com.visco.backend.models.dtos.DispatchRequest;
import com.visco.backend.models.dtos.DispatchResponse;
import com.visco.backend.models.dtos.GoodReceiptItemResponse;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.InventoryMovementResponse;
import com.visco.backend.models.dtos.ProductOnStock;
import com.visco.backend.models.dtos.ProductStockBreakdown;
import com.visco.backend.models.dtos.PurchaseOrderReceiptSummary;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.TransferStockRequest;
import com.visco.backend.models.dtos.WarehouseDTO;
import com.visco.backend.models.dtos.WarehouseStockSummary;
import com.visco.backend.models.entities.DispatchNote;
import com.visco.backend.models.entities.DispatchNoteItem;
import com.visco.backend.models.entities.Employee;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.MovementType;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.DispatchNoteRepository;
import com.visco.backend.repositories.EmployeeRepository;
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
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseService {

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final GoodReceiptRepository goodReceiptRepository;
  private final StockLevelRepository stockLevelRepository;
  private final WarehouseRepository warehouseRepository;
  private final UserRepository userRepository;
  private final ProductRepository productRepository;
  private final InventoryMovementRepository inventoryMovementRepository;
  private final DispatchNoteRepository dispatchNoteRepository;
  private final EmployeeRepository employeeRepository;

  private final Object stockLevelLock = new Object();

  // ─────────────────────────────────────────────────────────────
  // Warehouse CRUD
  // ─────────────────────────────────────────────────────────────

  @Transactional
  public WarehouseDTO createWarehouse(CreateWarehouseRequest request) {
    User responsible = userRepository
      .findById(request.responsibleUserId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "User not found: " + request.responsibleUserId()
        )
      );

    Warehouse warehouse = Warehouse.builder()
      .name(request.name())
      .physicalAddress(request.physicalAddress())
      .description(request.description())
      .sapCenterCode(request.sapCenterCode())
      .responsibleUser(responsible)
      .active(true)
      .build();

    return WarehouseDTO.fromEntity(warehouseRepository.save(warehouse));
  }

  @Transactional(readOnly = true)
  public Page<WarehouseDTO> getAllWarehouses(Pageable pageable) {
    return warehouseRepository.findAll(pageable).map(WarehouseDTO::fromEntity);
  }

  @Transactional(readOnly = true)
  public WarehouseDTO getWarehouse(Long id) {
    return warehouseRepository
      .findById(id)
      .map(WarehouseDTO::fromEntity)
      .orElseThrow(() ->
        new EntityNotFoundException("Warehouse not found: " + id)
      );
  }

  // ─────────────────────────────────────────────────────────────
  // Goods receiving
  // ─────────────────────────────────────────────────────────────

  @Transactional
  @CacheEvict(value = "dashboard", allEntries = true)
  public GoodReceiptResponse receiveGoods(
    Long orderId,
    ReceiveGoodsRequest request
  ) {
    PurchaseOrder order = purchaseOrderRepository
      .findById(orderId)
      .orElseThrow(() ->
        new EntityNotFoundException("Purchase order not found: " + orderId)
      );

    if (
      order.getStatus() == PurchaseOrderStatus.DELIVERED ||
      order.getStatus() == PurchaseOrderStatus.CANCELLED ||
      order.getStatus() == PurchaseOrderStatus.REJECTED
    ) {
      throw new IllegalStateException(
        "Cannot receive goods for an order with status: " + order.getStatus()
      );
    }

    // FIX #1: El warehouse de destino debe ser el del request (lo que el operador indica),
    // no siempre el de la PO. Validamos que exista antes de usarlo.
    Warehouse destWarehouse = warehouseRepository
      .findById(request.destinationWarehouseId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Warehouse not found: " + request.destinationWarehouseId()
        )
      );

    // Asumiendo que orderId es numérico (ej. 1, 25, 300)
    int currentYear = Year.now().getValue();

    // Resultado para el ID 15: RC-0015/2026
    GoodReceipt receipt = GoodReceipt.builder()
      .receiptNumber("PENDING") // temporal — se sobreescribe tras el save
      .purchaseOrder(order)
      .destinationWarehouseId(destWarehouse.getId())
      .receivedAt(LocalDateTime.now())
      .notes(request.notes())
      .build();

    receipt = goodReceiptRepository.saveAndFlush(receipt); // obtiene el ID real de la BD

    String receiptNumber = String.format(
      "RC-%04d/%d",
      receipt.getId(),
      currentYear
    );
    receipt.setReceiptNumber(receiptNumber);
    Map<Long, BigDecimal> previousReceived = new HashMap<>();
    for (GoodReceipt prev : goodReceiptRepository.findByPurchaseOrderId(
      orderId
    )) {
      for (GoodReceiptItem prevItem : prev.getItems()) {
        previousReceived.merge(
          prevItem.getProduct().getId(),
          prevItem.getReceivedQuantity(),
          BigDecimal::add
        );
      }
    }

    User receivedByUser;
    if (request.receivedById() != null) {
      receivedByUser = userRepository
        .findById(request.receivedById())
        .orElseThrow(() ->
          new EntityNotFoundException(
            "User not found: " + request.receivedById()
          )
        );
    } else {
      receivedByUser = userRepository
        .findById(order.getCreatedBy().getId())
        .orElse(order.getCreatedBy());
    }
    receipt.setReceivedBy(receivedByUser);

    User createdBy = userRepository
      .findById(order.getCreatedBy().getId())
      .orElse(order.getCreatedBy());

    for (ReceiveGoodsRequest.ReceiveItem itemReq : request.items()) {
      PurchaseOrderItem poItem = order
        .getItems()
        .stream()
        .filter(i -> i.getProduct().getId().equals(itemReq.productId()))
        .findFirst()
        .orElseThrow(() ->
          new EntityNotFoundException(
            "Product not found in order: " + itemReq.productId()
          )
        );

      BigDecimal expected = BigDecimal.valueOf(poItem.getQuantity());
      BigDecimal received = itemReq.receivedQuantity();

      GoodReceiptItem item = GoodReceiptItem.builder()
        .goodReceipt(receipt)
        .product(poItem.getProduct())
        .expectedQuantity(expected)
        .receivedQuantity(received)
        .build();

      receipt.getItems().add(item);

      // FIX #2: Usar findOrCreate para evitar
      // constraint violation en columna nullable=false
      StockLevel stockLevel = findOrCreateStockLevel(
        poItem.getProduct(),
        destWarehouse
      );

      stockLevel.setCurrentStock(stockLevel.getCurrentStock().add(received));

      stockLevelRepository.save(stockLevel);

      // Descontar pending stock al recibir mercancía
      substractPendingStock(
        poItem.getProduct().getId(),
        destWarehouse.getId(),
        received
      );

      InventoryMovement movement = InventoryMovement.builder()
        .product(poItem.getProduct())
        .toWarehouse(destWarehouse)
        .quantity(received)
        .type(MovementType.INPUT)
        .reason("Goods receipt - PO: " + order.getOrderNumber())
        .entryUnitPrice(poItem.getUnitPrice())
        .createdAt(LocalDateTime.now())
        .createdBy(createdBy)
        .build();
      inventoryMovementRepository.save(movement);
    }

    boolean allFullyReceived = determineIfFullyReceived(
      order,
      previousReceived,
      request
    );
    order.setStatus(
      allFullyReceived
        ? PurchaseOrderStatus.DELIVERED
        : PurchaseOrderStatus.PARTIALLY_DELIVERED
    );
    receipt.setClosed(allFullyReceived);
    purchaseOrderRepository.save(order);
    goodReceiptRepository.save(receipt);
    return buildReceiptResponse(receipt, order);
  }

  @Transactional(readOnly = true)
  public PurchaseOrderReceiptSummary getReceiptSummaryByOrder(Long orderId) {
    PurchaseOrder order = purchaseOrderRepository
      .findById(orderId)
      .orElseThrow(() ->
        new EntityNotFoundException("Purchase order not found: " + orderId)
      );

    List<GoodReceipt> receipts = goodReceiptRepository.findByPurchaseOrderId(
      orderId
    );

    // Acumular todo lo recibido por producto
    Map<Long, BigDecimal> totalReceived = new HashMap<>();
    for (GoodReceipt receipt : receipts) {
      for (GoodReceiptItem item : receipt.getItems()) {
        totalReceived.merge(
          item.getProduct().getId(),
          item.getReceivedQuantity(),
          BigDecimal::add
        );
      }
    }

    // Construir items con expected vs received vs pending
    List<PurchaseOrderReceiptSummary.ItemSummary> items = order
      .getItems()
      .stream()
      .map(poItem -> {
        BigDecimal ordered = BigDecimal.valueOf(poItem.getQuantity());
        BigDecimal received = totalReceived.getOrDefault(
          poItem.getProduct().getId(),
          BigDecimal.ZERO
        );
        BigDecimal pending = ordered.subtract(received);

        return PurchaseOrderReceiptSummary.ItemSummary.builder()
          .productId(poItem.getProduct().getId())
          .productName(poItem.getProduct().getName())
          .productSku(poItem.getProduct().getSku())
          .orderedQuantity(ordered)
          .receivedQuantity(received)
          .pendingQuantity(pending.max(BigDecimal.ZERO))
          .fullyReceived(received.compareTo(ordered) >= 0)
          .build();
      })
      .toList();

    return PurchaseOrderReceiptSummary.builder()
      .orderId(orderId)
      .orderNumber(order.getOrderNumber())
      .orderStatus(order.getStatus())
      .totalReceipts(receipts.size())
      .items(items)
      .build();
  }

  public boolean determineIfFullyReceived(
    PurchaseOrder order,
    Map<Long, BigDecimal> previousReceived,
    ReceiveGoodsRequest request
  ) {
    for (PurchaseOrderItem poItem : order.getItems()) {
      BigDecimal totalReceived = previousReceived.getOrDefault(
        poItem.getProduct().getId(),
        BigDecimal.ZERO
      );
      ReceiveGoodsRequest.ReceiveItem current = request
        .items()
        .stream()
        .filter(r -> r.productId().equals(poItem.getProduct().getId()))
        .findFirst()
        .orElse(null);
      if (current != null) {
        totalReceived = totalReceived.add(current.receivedQuantity());
      }
      if (
        totalReceived.compareTo(BigDecimal.valueOf(poItem.getQuantity())) < 0
      ) {
        return false;
      }
    }
    return true;
  }

  public GoodReceiptResponse buildReceiptResponse(
    GoodReceipt receipt,
    PurchaseOrder order
  ) {
    List<GoodReceiptItemResponse> itemResponses = receipt
      .getItems()
      .stream()
      .map(item ->
        new GoodReceiptItemResponse(
          item.getProduct().getId(),
          item.getProduct().getName(),
          item.getProduct().getSku(),
          item.getExpectedQuantity(),
          item.getReceivedQuantity(),
          item.getReceivedQuantity().subtract(item.getExpectedQuantity())
        )
      )
      .toList();

    return new GoodReceiptResponse(
      receipt.getId(),
      receipt.getReceiptNumber(),
      order.getId(),
      order.getDestinationWarehouse().getPhysicalAddress(),
      order.getOrderNumber(),
      order.getStatus(),
      receipt.getReceivedAt(),
      receipt.getNotes(),
      receipt.getReceivedBy() != null
        ? receipt.getReceivedBy().getName()
        : null,
      itemResponses
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Stock helpers
  // ─────────────────────────────────────────────────────────────

  private StockLevel findOrCreateStockLevel(
    Product product,
    Warehouse warehouse
  ) {
    return stockLevelRepository
      .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
      .orElseGet(() ->
        stockLevelRepository.save(
          StockLevel.builder()
            .product(product)
            .warehouse(warehouse)
            .currentStock(BigDecimal.ZERO)
            .pendingStock(BigDecimal.ZERO)
            .build()
        )
      );
  }

  public void addPendingStockByWarehouse(
    Long productId,
    Long warehouseId,
    BigDecimal quantity
  ) {
    Warehouse warehouse = warehouseRepository
      .findById(warehouseId)
      .orElseThrow(() ->
        new EntityNotFoundException("Warehouse not found: " + warehouseId)
      );

    // FIX #2: Reemplaza orElseGet inline por el helper centralizado
    Product product = productRepository
      .findById(productId)
      .orElseThrow(() ->
        new EntityNotFoundException("Product not found: " + productId)
      );

    StockLevel level = findOrCreateStockLevel(product, warehouse);
    level.setPendingStock(level.getPendingStock().add(quantity));
    stockLevelRepository.save(level);
  }

  public void addCurrentStock(
    Long productId,
    Long warehouseId,
    BigDecimal quantity
  ) {
    stockLevelRepository
      .findByProductIdAndWarehouseId(productId, warehouseId)
      .ifPresent(level -> {
        level.setCurrentStock(level.getCurrentStock().add(quantity));
        stockLevelRepository.save(level);
      });
  }

  public void substractPendingStock(
    Long productId,
    Long warehouseId,
    BigDecimal quantity
  ) {
    stockLevelRepository
      .findByProductIdAndWarehouseId(productId, warehouseId)
      .ifPresent(level -> {
        // FIX #3: Evitar pending stock negativo — clamp a 0
        BigDecimal newPending = level.getPendingStock().subtract(quantity);
        level.setPendingStock(newPending.max(BigDecimal.ZERO));
        stockLevelRepository.save(level);
      });
  }

  public void substractCurrentStock(
    Long productId,
    Long warehouseId,
    BigDecimal quantity
  ) {
    stockLevelRepository
      .findByProductIdAndWarehouseId(productId, warehouseId)
      .ifPresent(level -> {
        // FIX #3: Evitar stock negativo — lanzar excepción si no hay suficiente
        if (level.getCurrentStock().compareTo(quantity) < 0) {
          throw new IllegalArgumentException(
            "Insufficient stock for product " +
              productId +
              " in warehouse " +
              warehouseId
          );
        }
        level.setCurrentStock(level.getCurrentStock().subtract(quantity));
        stockLevelRepository.save(level);
      });
  }

  // ─────────────────────────────────────────────────────────────
  // Receipts queries
  // ─────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<GoodReceiptResponse> getReceiptsByOrderId(Long orderId) {
    return goodReceiptRepository
      .findByPurchaseOrderId(orderId)
      .stream()
      .map(this::toResponse)
      .toList();
  }

  @Transactional(readOnly = true)
  public Page<GoodReceiptResponse> getAllOrders(Pageable pageable) {
    return goodReceiptRepository.findAll(pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public GoodReceiptResponse getReceiptById(Long id) {
    GoodReceipt receipt = goodReceiptRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Receipt not found: " + id)
      );
    return toResponse(receipt);
  }

  // ─────────────────────────────────────────────────────────────
  // Stock breakdown & summary
  // ─────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public ProductStockBreakdown getStockBreakdownByProduct(Long productId) {
    BigDecimal totalStock = stockLevelRepository.getTotalStockByProductId(
      productId
    );
    if (totalStock == null) totalStock = BigDecimal.ZERO;

    List<StockLevelRepository.WarehouseStockProjection> projections =
      stockLevelRepository.getStockByProductGroupedByWarehouse(productId);

    List<ProductStockBreakdown.WarehouseStockEntry> entries = projections
      .stream()
      .map(p ->
        ProductStockBreakdown.WarehouseStockEntry.builder()
          .warehouseId(p.getWarehouseId())
          .warehouseName(p.getWarehouseName())
          .currentStock(
            p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO
          )
          .pendingStock(
            p.getPendingStock() != null ? p.getPendingStock() : BigDecimal.ZERO
          )
          .build()
      )
      .toList();

    BigDecimal totalPending = entries
      .stream()
      .map(ProductStockBreakdown.WarehouseStockEntry::getPendingStock)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    return ProductStockBreakdown.builder()
      .productId(productId)
      .totalStock(totalStock)
      .totalPendingStock(totalPending)
      .warehouses(entries)
      .build();
  }

  @Transactional(readOnly = true)
  public List<WarehouseStockSummary> getGlobalStockSummary() {
    return stockLevelRepository
      .getGlobalStockByWarehouse()
      .stream()
      .map(p ->
        WarehouseStockSummary.builder()
          .warehouseId(p.getWarehouseId())
          .warehouseName(p.getWarehouseName())
          .totalStock(
            p.getCurrentStock() != null
              ? BigDecimal.valueOf(p.getCurrentStock())
              : BigDecimal.ZERO
          )
          .totalPendingStock(
            p.getPendingStock() != null
              ? BigDecimal.valueOf(p.getPendingStock())
              : BigDecimal.ZERO
          )
          .build()
      )
      .toList();
  }

  // ─────────────────────────────────────────────────────────────
  // Products in stock by warehouse (transfer modal)
  // ─────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public Page<ProductOnStock> getProductsOnStock(
    Long warehouseId,
    String search,
    Pageable pageable
  ) {
    Page<StockLevel> stockPage =
      stockLevelRepository.findStockWithProductByWarehouse(
        pageable,
        warehouseId,
        search
      );

    return stockPage.map(sl -> {
      Product p = sl.getProduct();
      return new ProductOnStock(
        p.getId(),
        p.getInternalCode(),
        p.getSku(),
        p.getName(),
        p.getSapCode(),
        p.getUom().name(),
        sl.getCurrentStock(),
        sl.getPendingStock()
      );
    });
  }

  // ─────────────────────────────────────────────────────────────
  // All products in warehouse (including zero stock)
  // ─────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public Page<ProductOnStock> getAllProductsInWarehouse(
    Long warehouseId,
    String search,
    Pageable pageable
  ) {
    warehouseRepository
      .findById(warehouseId)
      .orElseThrow(() ->
        new EntityNotFoundException("Warehouse not found: " + warehouseId)
      );

    Page<StockLevel> stockPage = stockLevelRepository.findAllStockByWarehouse(
      pageable,
      warehouseId,
      search
    );

    return stockPage.map(sl -> {
      Product p = sl.getProduct();
      return new ProductOnStock(
        p.getId(),
        p.getInternalCode(),
        p.getSku(),
        p.getName(),
        p.getSapCode(),
        p.getUom().name(),
        sl.getCurrentStock(),
        sl.getPendingStock()
      );
    });
  }

  // ─────────────────────────────────────────────────────────────
  // Private mapper
  // ─────────────────────────────────────────────────────────────

  private GoodReceiptResponse toResponse(GoodReceipt receipt) {
    List<GoodReceiptItemResponse> itemResponses = receipt
      .getItems()
      .stream()
      .map(item ->
        new GoodReceiptItemResponse(
          item.getProduct().getId(),
          item.getProduct().getName(),
          item.getProduct().getSku(),
          item.getExpectedQuantity(),
          item.getReceivedQuantity(),
          item.getReceivedQuantity().subtract(item.getExpectedQuantity())
        )
      )
      .toList();

    return new GoodReceiptResponse(
      receipt.getId(),
      receipt.getReceiptNumber(),
      receipt.getPurchaseOrder().getId(),
      receipt.getPurchaseOrder().getDestinationWarehouse().getPhysicalAddress(),
      receipt.getPurchaseOrder().getOrderNumber(),
      receipt.getPurchaseOrder().getStatus(),
      receipt.getReceivedAt(),
      receipt.getNotes(),
      receipt.getReceivedBy() != null
        ? receipt.getReceivedBy().getName()
        : null,
      itemResponses
    );
  }

  @Transactional
  public void transferStock(TransferStockRequest request) {
    if (request.fromWarehouseId().equals(request.toWarehouseId())) {
      throw new IllegalArgumentException(
        "Source and destination warehouses must be different"
      );
    }

    Warehouse fromWarehouse = warehouseRepository
      .findById(request.fromWarehouseId())
      .orElseThrow(() ->
        new EntityNotFoundException("Source warehouse not found")
      );

    Warehouse toWarehouse = warehouseRepository
      .findById(request.toWarehouseId())
      .orElseThrow(() ->
        new EntityNotFoundException("Destination warehouse not found")
      );

    StockLevel sourceStock = stockLevelRepository
      .findByProductIdAndWarehouseId(
        request.productId(),
        request.fromWarehouseId()
      )
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Stock level not found for source warehouse"
        )
      );

    // FIX #3: Validar stock suficiente antes de transferir
    if (sourceStock.getCurrentStock().compareTo(request.quantity()) < 0) {
      throw new IllegalArgumentException(
        "Insufficient stock in the source warehouse for this transfer."
      );
    }

    // FIX #2: Usar helper centralizado para destino
    Product product = sourceStock.getProduct();
    StockLevel destinationStock = findOrCreateStockLevel(product, toWarehouse);

    sourceStock.setCurrentStock(
      sourceStock.getCurrentStock().subtract(request.quantity())
    );

    destinationStock.setCurrentStock(
      destinationStock.getCurrentStock().add(request.quantity())
    );

    stockLevelRepository.save(sourceStock);
    stockLevelRepository.save(destinationStock);

    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() -> new EntityNotFoundException("User not found"));

    InventoryMovement movement = InventoryMovement.builder()
      .product(sourceStock.getProduct())
      .fromWarehouse(fromWarehouse)
      .toWarehouse(toWarehouse)
      .quantity(request.quantity())
      .type(MovementType.TRANSFER)
      .reason(
        request.reason() != null
          ? request.reason()
          : "Transfer to other warehouse"
      )
      .entryUnitPrice(request.unitCost())
      .exitUnitPrice(request.unitCost())
      .createdAt(LocalDateTime.now())
      .createdBy(createdBy)
      .build();

    inventoryMovementRepository.save(movement);
  }

  @Transactional
  public DispatchResponse outputStock(DispatchRequest request) {
    Warehouse warehouse = warehouseRepository
      .findById(request.warehouseId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Warehouse not found: " + request.warehouseId()
        )
      );

    Employee employee = employeeRepository
      .findById(request.employeeId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Employee not found: " + request.employeeId()
        )
      );

    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() -> new EntityNotFoundException("User not found"));

    DispatchNote note = DispatchNote.builder()
      .dispatchNumber("PENDING")
      .warehouse(warehouse)
      .withdrawnBy(employee)
      .notes(request.notes())
      .createdAt(LocalDateTime.now())
      .createdBy(createdBy)
      .build();

    note = dispatchNoteRepository.saveAndFlush(note);

    String dispatchNumber = String.format(
      "DS-%04d/%d",
      note.getId(),
      Year.now().getValue()
    );
    note.setDispatchNumber(dispatchNumber);

    for (DispatchRequest.DispatchItem itemReq : request.items()) {
      Product product = productRepository
        .findById(itemReq.productId())
        .orElseThrow(() ->
          new EntityNotFoundException(
            "Product not found: " + itemReq.productId()
          )
        );

      DispatchNoteItem item = DispatchNoteItem.builder()
        .dispatchNote(note)
        .product(product)
        .quantity(itemReq.quantity())
        .exitUnitPrice(itemReq.exitUnitPrice())
        .build();

      note.getItems().add(item);

      StockLevel stockLevel = findOrCreateStockLevel(product, warehouse);
      substractCurrentStock(
        product.getId(),
        warehouse.getId(),
        itemReq.quantity()
      );

      InventoryMovement movement = InventoryMovement.builder()
        .product(product)
        .fromWarehouse(warehouse)
        .quantity(itemReq.quantity())
        .type(MovementType.OUTPUT)
        .reason("Dispatch: " + dispatchNumber)
        .exitUnitPrice(itemReq.exitUnitPrice())
        .createdAt(LocalDateTime.now())
        .createdBy(createdBy)
        .build();
      inventoryMovementRepository.save(movement);
    }

    dispatchNoteRepository.save(note);
    return DispatchResponse.fromEntity(note);
  }

  @Transactional(readOnly = true)
  public Page<DispatchResponse> getAllDispatches(Pageable pageable) {
    return dispatchNoteRepository
      .findAllByOrderByCreatedAtDesc(pageable)
      .map(DispatchResponse::fromEntity);
  }

  @Transactional(readOnly = true)
  public DispatchResponse getDispatchById(Long id) {
    DispatchNote note = dispatchNoteRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Dispatch not found: " + id)
      );
    return DispatchResponse.fromEntity(note);
  }

  @Transactional
  public void adjustStock(AdjustStockRequest request) {
    Warehouse warehouse = warehouseRepository
      .findById(request.warehouseId())
      .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

    Product product = productRepository
      .findById(request.productId())
      .orElseThrow(() -> new EntityNotFoundException("Product not found"));

    StockLevel stock = findOrCreateStockLevel(product, warehouse);

    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() -> new EntityNotFoundException("User not found"));

    BigDecimal currentStock = stock.getCurrentStock();
    BigDecimal difference = request.newStock().subtract(currentStock);

    InventoryMovement movement = InventoryMovement.builder()
      .product(product)
      .fromWarehouse(warehouse)
      .toWarehouse(warehouse)
      .quantity(difference) // signed: negativo si reduce stock, positivo si aumenta
      .type(MovementType.ADJUSTMENT)
      .reason(request.reason() != null ? request.reason() : "Adjust stock")
      .entryUnitPrice(request.unitCost())
      .createdAt(LocalDateTime.now())
      .createdBy(createdBy)
      .build();

    stock.setCurrentStock(request.newStock());

    stockLevelRepository.save(stock);
    inventoryMovementRepository.save(movement);
  }

  // ─────────────────────────────────────────────────────────────
  // Kardex / Inventory Movements listing
  // ─────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public Page<InventoryMovementResponse> getMovements(
    Long productId,
    Long warehouseId,
    MovementType type,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Pageable pageable
  ) {
    BigDecimal balanceBefore = BigDecimal.ZERO;
    if (productId != null && startDate != null) {
      balanceBefore = inventoryMovementRepository.calculateRunningBalanceUntil(
        productId,
        startDate
      );
      if (balanceBefore == null) balanceBefore = BigDecimal.ZERO;
    }

    final BigDecimal openingBalance = balanceBefore;

    Page<InventoryMovement> movementsPage =
      inventoryMovementRepository.findMovementsWithFilters(
        productId,
        warehouseId,
        type,
        startDate,
        endDate,
        pageable
      );

    // FIX #5: El balance acumulado se calculaba mal — siempre sumaba al openingBalance fijo
    // en lugar de acumular. Usamos AtomicReference para acumular dentro del lambda.
    AtomicReference<BigDecimal> running = new AtomicReference<>(openingBalance);

    return movementsPage.map(m -> {
      // Outputs y adjustments negativos restan; inputs y transfers suman
      BigDecimal qty = (m.getType() == MovementType.OUTPUT)
        ? m.getQuantity().negate()
        : m.getQuantity();

      BigDecimal runningBalance = running.updateAndGet(current ->
        current.add(qty)
      );

      return new InventoryMovementResponse(
        m.getId(),
        m.getProduct().getId(),
        m.getProduct().getName(),
        m.getProduct().getSku(),
        m.getType().name(),
        m.getQuantity(),
        m.getEntryUnitPrice(),
        m.getExitUnitPrice(),
        m.getFromWarehouse() != null ? m.getFromWarehouse().getName() : null,
        m.getToWarehouse() != null ? m.getToWarehouse().getName() : null,
        m.getReason(),
        m.getCreatedAt(),
        m.getCreatedBy() != null ? m.getCreatedBy().getName() : null,
        runningBalance
      );
    });
  }
}
