package com.visco.backend.services;

import com.visco.backend.models.dtos.AdjustStockRequest;
import com.visco.backend.models.dtos.CreateWarehouseRequest;
import com.visco.backend.models.dtos.GoodReceiptItemResponse;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.InventoryMovementResponse;
import com.visco.backend.models.dtos.ProductStockBreakdown;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.TransferStockRequest;
import com.visco.backend.models.dtos.WarehouseDTO;
import com.visco.backend.models.dtos.WarehouseStockSummary;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
      .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
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

    Warehouse destWarehouse = warehouseRepository
      .findById(request.destinationWarehouseId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Warehouse not found: " + request.destinationWarehouseId()
        )
      );

    GoodReceipt receipt = GoodReceipt.builder()
      .receiptNumber(
        "VIS-" +
          orderId +
          "-" +
          System.currentTimeMillis() +
          "-" +
          UUID.randomUUID().toString().substring(0, 8)
      )
      .purchaseOrder(order)
      .destinationWarehouseId(order.getDestinationWarehouse().getId())
      .receivedAt(LocalDateTime.now())
      .notes(request.notes())
      .build();

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

      // Find or create StockLevel at the destination warehouse
      StockLevel stockLevel = stockLevelRepository
        .findByProductIdAndWarehouseId(
          poItem.getProduct().getId(),
          request.destinationWarehouseId()
        )
        .orElseGet(() ->
          StockLevel.builder()
            .product(poItem.getProduct())
            .warehouse(destWarehouse)
            .currentStock(BigDecimal.ZERO)
            .pendingStock(BigDecimal.ZERO)
            .build()
        );

      stockLevel.setCurrentStock(stockLevel.getCurrentStock().add(received));
      stockLevelRepository.save(stockLevel);

      // Remove from pending stock for this product/warehouse
      substractPendingStock(
        poItem.getProduct().getId(),
        request.destinationWarehouseId(),
        received
      );

      // Record inventory movement (INPUT)
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

    goodReceiptRepository.save(receipt);

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
    purchaseOrderRepository.save(order);

    return buildReceiptResponse(receipt, order);
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

    StockLevel level = stockLevelRepository
      .findByProductIdAndWarehouseId(productId, warehouseId)
      .orElseGet(() -> {
        Product product = productRepository
          .findById(productId)
          .orElseThrow(() ->
            new EntityNotFoundException("Product not found: " + productId)
          );
        return StockLevel.builder()
          .product(product)
          .warehouse(warehouse)
          .currentStock(BigDecimal.ZERO)
          .pendingStock(BigDecimal.ZERO)
          .build();
      });
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
        level.setPendingStock(level.getPendingStock().subtract(quantity));
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
            p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO
          )
          .totalPendingStock(
            p.getPendingStock() != null ? p.getPendingStock() : BigDecimal.ZERO
          )
          .build()
      )
      .toList();
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

    if (sourceStock.getCurrentStock().compareTo(request.quantity()) < 0) {
      throw new IllegalArgumentException(
        "Insufficient stock in the source warehouse for this transfer."
      );
    }

    StockLevel destinationStock = stockLevelRepository
      .findByProductIdAndWarehouseId(
        request.productId(),
        request.toWarehouseId()
      )
      .orElseGet(() ->
        StockLevel.builder()
          .product(sourceStock.getProduct())
          .warehouse(toWarehouse)
          .currentStock(BigDecimal.ZERO)
          .pendingStock(BigDecimal.ZERO)
          .build()
      );

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
  public void adjustStock(AdjustStockRequest request) {
    Warehouse warehouse = warehouseRepository
      .findById(request.warehouseId())
      .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

    StockLevel stock = stockLevelRepository
      .findByProductIdAndWarehouseId(request.productId(), request.warehouseId())
      .orElseThrow(() -> new EntityNotFoundException("Stock level not found"));

    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() -> new EntityNotFoundException("User not found"));

    BigDecimal currentStock = stock.getCurrentStock();
    BigDecimal difference = request.newStock().subtract(currentStock);

    InventoryMovement movement = InventoryMovement.builder()
      .product(stock.getProduct())
      .fromWarehouse(warehouse)
      .toWarehouse(warehouse)
      .quantity(difference)
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

    return movementsPage.map(m -> {
      BigDecimal qty =
        m.getType() == MovementType.OUTPUT ||
        m.getType() == MovementType.ADJUSTMENT
          ? m.getQuantity().negate()
          : m.getQuantity();

      BigDecimal entryPrice = m.getEntryUnitPrice();
      BigDecimal exitPrice = m.getExitUnitPrice();

      BigDecimal runningBalance = openingBalance.add(qty);

      return new InventoryMovementResponse(
        m.getId(),
        m.getProduct().getId(),
        m.getProduct().getName(),
        m.getProduct().getSku(),
        m.getType().name(),
        m.getQuantity(),
        entryPrice,
        exitPrice,
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
