package com.visco.backend.services;

import com.visco.backend.models.dtos.AdjustStockRequest;
import com.visco.backend.models.dtos.CreateWarehouseRequest;
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
import com.visco.backend.models.entities.Location;
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
import com.visco.backend.repositories.LocationRepository;
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
import java.util.stream.Collectors;
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
  private final LocationRepository locationRepository;

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
    return warehouseRepository
      .findAllWithFetch(pageable)
      .map(WarehouseDTO::fromEntity);
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

  @Transactional
  @CacheEvict(value = "dashboard", allEntries = true)
  public GoodReceiptResponse receiveGoods(
    Long orderId,
    ReceiveGoodsRequest request
  ) {
    PurchaseOrder order = purchaseOrderRepository
      .findByIdDetailed(orderId)
      .orElseThrow(() ->
        new EntityNotFoundException("Purchase order not found: " + orderId)
      );

    if (
      order.getStatus() == PurchaseOrderStatus.DELIVERED ||
      order.getStatus() == PurchaseOrderStatus.CANCELLED ||
      order.getStatus() == PurchaseOrderStatus.REJECTED ||
      order.getStatus() == PurchaseOrderStatus.PENDING ||
      order.getStatus() == PurchaseOrderStatus.AWAITING_APPROVAL
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

    Location location = locationRepository
      .findById(request.locationId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Location not found: " + request.locationId()
        )
      );

    if (!location.getWarehouse().getId().equals(destWarehouse.getId())) {
      throw new IllegalArgumentException(
        "Location does not belong to the specified warehouse"
      );
    }

    int currentYear = Year.now().getValue();

    GoodReceipt receipt = GoodReceipt.builder()
      .receiptNumber("PENDING")
      .purchaseOrder(order)
      .destinationWarehouse(destWarehouse)
      .receivedAt(LocalDateTime.now())
      .notes(request.notes())
      .build();

    receipt = goodReceiptRepository.saveAndFlush(receipt);

    String receiptNumber = String.format(
      "RC-%04d/%d",
      receipt.getId(),
      currentYear
    );
    receipt.setReceiptNumber(receiptNumber);
    Map<Long, BigDecimal> previousReceived = goodReceiptRepository
      .getTotalReceivedByOrder(orderId)
      .stream()
      .collect(
        Collectors.toMap(
          GoodReceiptRepository.ReceivedQuantityProjection::getProductId,
          GoodReceiptRepository.ReceivedQuantityProjection::getTotalReceived,
          BigDecimal::add
        )
      );

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

      BigDecimal expected = poItem.getQuantity();
      BigDecimal received = itemReq.receivedQuantity();

      GoodReceiptItem item = GoodReceiptItem.builder()
        .goodReceipt(receipt)
        .product(poItem.getProduct())
        .expectedQuantity(expected)
        .receivedQuantity(received)
        .location(location)
        .build();

      receipt.getItems().add(item);

      // Operación atómica: upsert + increment en una sola sentencia SQL
      stockLevelRepository.addCurrentStockAtomic(
        poItem.getProduct().getId(),
        destWarehouse.getId(),
        received
      );

      // Descontar pending stock atómicamente
      stockLevelRepository.subtractPendingStockAtomic(
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
      .findByIdDetailed(orderId)
      .orElseThrow(() ->
        new EntityNotFoundException("Purchase order not found: " + orderId)
      );

    List<GoodReceipt> receipts =
      goodReceiptRepository.findByPurchaseOrderIdWithFetch(orderId);

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

    List<PurchaseOrderReceiptSummary.ItemSummary> items = order
      .getItems()
      .stream()
      .map(poItem -> {
        BigDecimal ordered = poItem.getQuantity();
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
      if (totalReceived.compareTo(poItem.getQuantity()) < 0) {
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
          item.getReceivedQuantity().subtract(item.getExpectedQuantity()),
          item.getLocation() != null ? item.getLocation().getId() : null,
          item.getLocation() != null ? item.getLocation().getCode() : null
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
      itemResponses,
      GoodReceiptResponse.PurchaseOrderSummary.fromEntity(order)
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Stock helpers — operaciones atómicas vía SQL nativo
  // ─────────────────────────────────────────────────────────────

  public void addPendingStockByWarehouse(
    Long productId,
    Long warehouseId,
    BigDecimal quantity
  ) {
    warehouseRepository
      .findById(warehouseId)
      .orElseThrow(() ->
        new EntityNotFoundException("Warehouse not found: " + warehouseId)
      );

    productRepository
      .findById(productId)
      .orElseThrow(() ->
        new EntityNotFoundException("Product not found: " + productId)
      );

    stockLevelRepository.addPendingStockAtomic(
      productId,
      warehouseId,
      quantity
    );
  }

  public void addCurrentStock(
    Long productId,
    Long warehouseId,
    BigDecimal quantity
  ) {
    stockLevelRepository.addCurrentStockAtomic(
      productId,
      warehouseId,
      quantity
    );
  }

  public void substractPendingStock(
    Long productId,
    Long warehouseId,
    BigDecimal quantity
  ) {
    stockLevelRepository.subtractPendingStockAtomic(
      productId,
      warehouseId,
      quantity
    );
  }

  public void substractCurrentStock(
    Long productId,
    Long warehouseId,
    BigDecimal quantity
  ) {
    int updated = stockLevelRepository.subtractCurrentStockAtomic(
      productId,
      warehouseId,
      quantity
    );
    if (updated == 0) {
      // Puede ser que no exista stock level o que no haya suficiente stock
      stockLevelRepository
        .findByProductIdAndWarehouseId(productId, warehouseId)
        .ifPresentOrElse(
          level -> {
            if (level.getCurrentStock().compareTo(quantity) < 0) {
              throw new IllegalArgumentException(
                "Insufficient stock for product " +
                  productId +
                  " in warehouse " +
                  warehouseId
              );
            }
          },
          () -> {
            throw new EntityNotFoundException(
              "Stock level not found for product " +
                productId +
                " in warehouse " +
                warehouseId
            );
          }
        );
    }
  }

  @Transactional(readOnly = true)
  public List<GoodReceiptResponse> getReceiptsByOrderId(Long orderId) {
    return goodReceiptRepository
      .findByPurchaseOrderIdWithFetch(orderId)
      .stream()
      .map(this::toResponse)
      .toList();
  }

  @Transactional(readOnly = true)
  public Page<GoodReceiptResponse> getAllOrders(Pageable pageable) {
    return goodReceiptRepository
      .findAllWithFetch(pageable)
      .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public GoodReceiptResponse getReceiptById(Long id) {
    GoodReceipt receipt = goodReceiptRepository
      .findByIdDetailed(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Receipt not found: " + id)
      );
    return toResponse(receipt);
  }

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
        sl.getPendingStock(),
        p.getReorderPoint(),
        p.getMaxStock()
      );
    });
  }

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
        sl.getPendingStock(),
        p.getReorderPoint(),
        p.getMaxStock()
      );
    });
  }

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
          item.getReceivedQuantity().subtract(item.getExpectedQuantity()),
          item.getLocation() != null ? item.getLocation().getId() : null,
          item.getLocation() != null ? item.getLocation().getCode() : null
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
      itemResponses,
      GoodReceiptResponse.PurchaseOrderSummary.fromEntity(receipt.getPurchaseOrder())
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

    // Validar stock suficiente antes de transferir
    BigDecimal currentStock =
      stockLevelRepository.getStockByProductAndWarehouse(
        request.productId(),
        request.fromWarehouseId()
      );
    if (
      currentStock == null || currentStock.compareTo(request.quantity()) < 0
    ) {
      throw new IllegalArgumentException(
        "Insufficient stock in the source warehouse for this transfer."
      );
    }

    // Operaciones atómicas: debitar origen y acreditar destino
    stockLevelRepository.subtractCurrentStockAtomic(
      request.productId(),
      request.fromWarehouseId(),
      request.quantity()
    );

    stockLevelRepository.addCurrentStockAtomic(
      request.productId(),
      request.toWarehouseId(),
      request.quantity()
    );

    Product product = productRepository
      .findById(request.productId())
      .orElseThrow(() -> new EntityNotFoundException("Product not found"));

    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() -> new EntityNotFoundException("User not found"));

    InventoryMovement movement = InventoryMovement.builder()
      .product(product)
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
  public DispatchResponse outputStock(
    DispatchRequest request,
    String currentUserEmail
  ) {
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

    if (!employee.isActive()) {
      throw new IllegalArgumentException(
        "Employee is inactive: " + request.employeeId()
      );
    }

    User createdBy = userRepository
      .findByEmail(currentUserEmail)
      .orElseThrow(() ->
        new EntityNotFoundException("User not found: " + currentUserEmail)
      );

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

    List<Long> productIds = request
      .items()
      .stream()
      .map(DispatchRequest.DispatchItem::productId)
      .toList();
    Map<Long, Product> productMap = productRepository
      .findAllById(productIds)
      .stream()
      .collect(Collectors.toMap(Product::getId, p -> p));

    for (DispatchRequest.DispatchItem itemReq : request.items()) {
      Product product = productMap.get(itemReq.productId());
      if (product == null) {
        throw new EntityNotFoundException(
          "Product not found: " + itemReq.productId()
        );
      }

      DispatchNoteItem item = DispatchNoteItem.builder()
        .dispatchNote(note)
        .product(product)
        .quantity(itemReq.quantity())
        .exitUnitPrice(itemReq.exitUnitPrice())
        .build();

      note.getItems().add(item);

      // Operación atómica: debitar stock
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
      .findAllWithFetch(pageable)
      .map(DispatchResponse::fromEntity);
  }

  @Transactional(readOnly = true)
  public DispatchResponse getDispatchById(Long id) {
    DispatchNote note = dispatchNoteRepository
      .findByIdDetailed(id)
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

    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() -> new EntityNotFoundException("User not found"));

    BigDecimal currentStock =
      stockLevelRepository.getStockByProductAndWarehouse(
        product.getId(),
        warehouse.getId()
      );
    if (currentStock == null) currentStock = BigDecimal.ZERO;

    BigDecimal difference = request.newStock().subtract(currentStock);

    // Operación atómica: upsert con valor exacto
    stockLevelRepository.setCurrentStockAtomic(
      product.getId(),
      warehouse.getId(),
      request.newStock()
    );

    InventoryMovement movement = InventoryMovement.builder()
      .product(product)
      .fromWarehouse(warehouse)
      .toWarehouse(warehouse)
      .quantity(difference)
      .type(MovementType.ADJUSTMENT)
      .reason(request.reason() != null ? request.reason() : "Adjust stock")
      .entryUnitPrice(request.unitCost())
      .createdAt(LocalDateTime.now())
      .createdBy(createdBy)
      .build();

    inventoryMovementRepository.save(movement);
  }

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

    AtomicReference<BigDecimal> running = new AtomicReference<>(openingBalance);

    return movementsPage.map(m -> {
      BigDecimal signedQty = signedQuantityForRunningBalance(m);

      BigDecimal runningBalance = running.updateAndGet(current ->
        current.add(signedQty)
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

  /**
   * The running balance in the kardex is the cumulative quantity of a
   * product. INPUT, OUTPUT, and ADJUSTMENT all change that total.
   * INPUT adds, OUTPUT subtracts, and ADJUSTMENT stores an already-signed
   * quantity (newStock - currentStock), so we add it as-is. TRANSFER
   * moves stock between warehouses without changing the per-product
   * total, so it contributes zero to the running balance.
   */
  private BigDecimal signedQuantityForRunningBalance(InventoryMovement m) {
    return switch (m.getType()) {
      case INPUT -> m.getQuantity();
      case OUTPUT, DISPATCH -> m.getQuantity().negate();
      case TRANSFER -> BigDecimal.ZERO;
      case ADJUSTMENT -> m.getQuantity();
    };
  }
}
