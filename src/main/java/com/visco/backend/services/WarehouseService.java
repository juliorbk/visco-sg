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
import com.visco.backend.models.dtos.WarehouseResponse;
import com.visco.backend.models.dtos.WarehouseStockSummary;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.Location;
import com.visco.backend.models.entities.MovementType;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.InventoryMovementRepository;
import com.visco.backend.repositories.LocationRepository;
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
  private final LocationRepository locationRepository;
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
  public List<WarehouseResponse> getAllWarehouses() {
    return warehouseRepository
      .findAll()
      .stream()
      .filter(Warehouse::isActive)
      .map(w ->
        WarehouseResponse.builder()
          .id(w.getId())
          .name(w.getName())
          .sapCenterCode(w.getSapCenterCode())
          .build()
      )
      .toList();
  }

  // ─────────────────────────────────────────────────────────────
  // Goods receiving
  // ─────────────────────────────────────────────────────────────

  @Transactional
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

    Location destLocation = locationRepository
      .findById(request.destinationLocationId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Location not found: " + request.destinationLocationId()
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

      // Put-away: find or create StockLevel at the destination location
      StockLevel stockLevel = stockLevelRepository
        .findByProductIdAndLocationId(
          poItem.getProduct().getId(),
          request.destinationLocationId()
        )
        .orElseGet(() ->
          StockLevel.builder()
            .product(poItem.getProduct())
            .location(destLocation)
            .currentStock(BigDecimal.ZERO)
            .pendingStock(BigDecimal.ZERO)
            .build()
        );

      stockLevel.setCurrentStock(stockLevel.getCurrentStock().add(received));
      stockLevelRepository.save(stockLevel);

      // Remove from pending stock (from whichever level it was added)
      substractPendingStock(poItem.getProduct().getId(), received);

      // Record inventory movement (INPUT)
      InventoryMovement movement = InventoryMovement.builder()
        .product(poItem.getProduct())
        .toLocation(destLocation)
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
      itemResponses
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Stock helpers
  // ─────────────────────────────────────────────────────────────

  // public void addPendingStock(Long productId, BigDecimal quantity) {
  // StockLevel level = getFirstStockLevel(productId);
  // level.setPendingStock(level.getPendingStock().add(quantity));
  // stockLevelRepository.save(level);
  // }

  public void addPendingStockByWarehouse(
    Long productId,
    Long warehouseId,
    BigDecimal quantity
  ) {
    List<StockLevel> levels =
      stockLevelRepository.findByProductIdAndLocationWarehouseId(
        productId,
        warehouseId
      );
    if (levels.isEmpty()) {
      return;
    }
    StockLevel level = levels.get(0);
    level.setPendingStock(level.getPendingStock().add(quantity));
    stockLevelRepository.save(level);
  }

  public void addCurrentStock(Long productId, BigDecimal quantity) {
    StockLevel level = getFirstStockLevel(productId);
    level.setCurrentStock(level.getCurrentStock().add(quantity));
  }

  public void substractPendingStock(Long productId, BigDecimal quantity) {
    StockLevel level = getFirstStockLevel(productId);
    level.setPendingStock(level.getPendingStock().subtract(quantity));
  }

  public void substractCurrentStock(Long productId, BigDecimal quantity) {
    StockLevel level = getFirstStockLevel(productId);
    level.setCurrentStock(level.getCurrentStock().subtract(quantity));
  }

  private StockLevel getFirstStockLevel(Long productId) {
    List<StockLevel> levels = stockLevelRepository.findByProductId(productId);
    if (levels.isEmpty()) {
      throw new EntityNotFoundException(
        "No stock level found for product ID: " + productId
      );
    }
    return levels.get(0);
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
      itemResponses
    );
  }

  @Transactional
  public void transferStock(TransferStockRequest request) {
    // 1. Fetch the source stock level
    StockLevel sourceStock = stockLevelRepository
      .findByProductIdAndLocationId(
        request.productId(),
        request.fromLocationId()
      )
      .orElseThrow(() ->
        new EntityNotFoundException("Stock level not found for source location")
      );

    // 2. Validate sufficient stock
    if (sourceStock.getCurrentStock().compareTo(request.quantity()) < 0) {
      throw new IllegalArgumentException(
        "Insufficient stock in the source location for this transfer."
      );
    }

    // 3. Fetch or create the destination stock level
    StockLevel destinationStock = stockLevelRepository
      .findByProductIdAndLocationId(request.productId(), request.toLocationId())
      .orElseGet(() -> {
        Location toLocation = locationRepository
          .findById(request.toLocationId())
          .orElseThrow(() ->
            new EntityNotFoundException("Destination location not found")
          );

        return StockLevel.builder()
          .product(sourceStock.getProduct())
          .location(toLocation)
          .currentStock(BigDecimal.ZERO)
          .pendingStock(BigDecimal.ZERO)
          .build();
      });

    // 4. Update the balances
    sourceStock.setCurrentStock(
      sourceStock.getCurrentStock().subtract(request.quantity())
    );
    destinationStock.setCurrentStock(
      destinationStock.getCurrentStock().add(request.quantity())
    );

    stockLevelRepository.save(sourceStock);
    stockLevelRepository.save(destinationStock);

    // 5. Record the movement in the audit trail
    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() -> new EntityNotFoundException("User not found"));

    InventoryMovement movement = InventoryMovement.builder()
      .product(sourceStock.getProduct())
      .fromLocation(sourceStock.getLocation()) //Original location
      .toLocation(destinationStock.getLocation()) //New location
      .quantity(request.quantity())
      .type(MovementType.TRANSFER) // Type
      .reason(
        request.reason() != null
          ? request.reason()
          : "Transfer to other location"
      ) //Reason
      .entryUnitPrice(request.unitCost())
      .exitUnitPrice(request.unitCost())
      .createdAt(LocalDateTime.now()) //Date
      .createdBy(createdBy) //Responsible
      .build();

    inventoryMovementRepository.save(movement);
  }

  @Transactional
  public void adjustStock(AdjustStockRequest request) {
    // 1. Fetch the stock level
    StockLevel stock = stockLevelRepository
      .findByProductIdAndLocationId(request.productId(), request.locationId())
      .orElseThrow(() -> new EntityNotFoundException("Stock level not found"));

    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() -> new EntityNotFoundException("User not found"));

    BigDecimal currentStock = stock.getCurrentStock();
    BigDecimal difference = request.newStock().subtract(currentStock);

    // 2. Record the movement in the audit trail
    InventoryMovement movement = InventoryMovement.builder()
      .product(stock.getProduct())
      .fromLocation(stock.getLocation()) //Original location
      .toLocation(stock.getLocation()) //New location
      .quantity(difference)
      .type(MovementType.ADJUSTMENT) // Type
      .reason(request.reason() != null ? request.reason() : "Adjust stock") //Reason
      .entryUnitPrice(request.unitCost())
      .createdAt(LocalDateTime.now()) //Date
      .createdBy(createdBy) //Responsible
      .build();

    // 2. Update the stock level
    // Asignamos directamente el valor, porque ya es un BigDecimal
    stock.setCurrentStock(request.newStock());

    stockLevelRepository.save(stock);
    inventoryMovementRepository.save(movement);
  }

  // ─────────────────────────────────────────────────────────────
  // Kardex / Inventory Movements listing
  // ─────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<InventoryMovementResponse> getMovements(
    Long productId,
    Long locationId,
    MovementType type,
    LocalDateTime startDate,
    LocalDateTime endDate
  ) {
    List<InventoryMovement> movements =
      inventoryMovementRepository.findMovementsWithFilters(
        productId,
        locationId,
        type,
        startDate,
        endDate
      );

    BigDecimal[] runningBalance = { BigDecimal.ZERO };

    return movements
      .stream()
      .map(m -> {
        runningBalance[0] = runningBalance[0].add(m.getQuantity());

        BigDecimal entryPrice = m.getEntryUnitPrice();
        BigDecimal exitPrice = m.getExitUnitPrice();

        return new InventoryMovementResponse(
          m.getId(),
          m.getProduct().getId(),
          m.getProduct().getName(),
          m.getProduct().getSku(),
          m.getType().name(),
          m.getQuantity(),
          entryPrice,
          exitPrice,
          m.getFromLocation() != null
            ? m.getFromLocation().getLocationCode()
            : null,
          m.getToLocation() != null
            ? m.getToLocation().getLocationCode()
            : null,
          m.getReason(),
          m.getCreatedAt(),
          m.getCreatedBy() != null ? m.getCreatedBy().getName() : null,
          runningBalance[0]
        );
      })
      .toList();
  }
}
