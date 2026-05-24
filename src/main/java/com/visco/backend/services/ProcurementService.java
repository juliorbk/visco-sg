package com.visco.backend.services;

import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemResponse;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.Requisition;
import com.visco.backend.models.entities.RequisitionStatus;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.User;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProcurementService {

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final WarehouseRepository warehouseRepository;
  private final SupplierRepository supplierRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final WarehouseService warehouseService;
  private final RequisitionRepository requisitionRepository;
  private final GoodReceiptRepository goodReceiptRepository;

  // ─────────────────────────────────────────────────────────────
  // Create purchase order
  // ─────────────────────────────────────────────────────────────

  @Transactional
  @CacheEvict(value = "dashboard", allEntries = true)
  public PurchaseOrderResponse createPurchaseOrder(
    CreatePurchaseOrderRequest request
  ) {
    Supplier supplier = supplierRepository
      .findById(request.supplierId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Supplier not found: " + request.supplierId()
        )
      );

    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() ->
        new EntityNotFoundException("User not found: " + request.createdById())
      );

    Warehouse destinationWarehouse = warehouseRepository
      .findById(request.destinationWarehouseId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Warehouse not found: " + request.destinationWarehouseId()
        )
      );

    PurchaseOrder order = PurchaseOrder.builder()
      .orderNumber(request.orderNumber())
      .description(request.description())
      .createdBy(createdBy)
      .destinationWarehouse(destinationWarehouse)
      .status(PurchaseOrderStatus.PENDING)
      .paymentMethod(request.paymentMethod())
      .type(request.type())
      .supplier(supplier)
      .leadTime(request.leadTime())
      .createdAt(LocalDateTime.now())
      .build();

    if (request.requisitionId() != null) {
      Requisition requisition = requisitionRepository
        .findById(request.requisitionId())
        .orElseThrow(() ->
          new EntityNotFoundException(
            "Requisition not found: " + request.requisitionId()
          )
        );
      if (requisition.getStatus() != RequisitionStatus.APPROVED) {
        throw new IllegalStateException(
          "Only approved requisitions can be converted to PO"
        );
      }
      order.setRequisition(requisition);
      requisition.setStatus(RequisitionStatus.CONVERTED);
      requisitionRepository.save(requisition);
    }

    List<Long> productIds = request
      .items()
      .stream()
      .map(PurchaseOrderItemRequest::productId)
      .toList();

    Map<Long, Product> productMap = productRepository
      .findAllById(productIds)
      .stream()
      .collect(Collectors.toMap(Product::getId, p -> p));

    for (PurchaseOrderItemRequest itemReq : request.items()) {
      Product product = productMap.get(itemReq.productId());
      if (product == null) {
        throw new EntityNotFoundException(
          "Product not found: " + itemReq.productId()
        );
      }

      PurchaseOrderItem item = PurchaseOrderItem.builder()
        .purchaseOrder(order)
        .product(product)
        .quantity(itemReq.quantity())
        .unitPrice(itemReq.unitPrice())
        .build();

      order.getItems().add(item);

      warehouseService.addPendingStockByWarehouse(
        product.getId(),
        request.destinationWarehouseId(),
        BigDecimal.valueOf(itemReq.quantity())
      );
    }

    PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
    return toResponse(savedOrder);
  }

  // ─────────────────────────────────────────────────────────────
  // Internal state transitions (package-private for clarity)
  // ─────────────────────────────────────────────────────────────

  @Transactional
  void submitForApproval(Long orderId) {
    PurchaseOrder order = findOrderById(orderId);
    if (order.getStatus() != PurchaseOrderStatus.PENDING) {
      throw new IllegalStateException(
        "Only pending orders can be submitted for approval"
      );
    }
    log.info("Submitting order ID: {} for approval", orderId);
    order.setStatus(PurchaseOrderStatus.AWAITING_APPROVAL);
    order.setUpdatedAt(LocalDateTime.now());
    purchaseOrderRepository.save(order);
  }

  @Transactional
  void approveOrder(Long orderId, UUID approverUserId, String notes) {
    PurchaseOrder order = findOrderById(orderId);
    if (order.getStatus() != PurchaseOrderStatus.AWAITING_APPROVAL) {
      throw new IllegalStateException(
        "Only orders awaiting approval can be approved"
      );
    }
    User approver = userRepository
      .findById(approverUserId)
      .orElseThrow(() ->
        new EntityNotFoundException("User not found: " + approverUserId)
      );
    log.info("Approving order ID: {} by user: {}", orderId, approver.getName());
    order.setStatus(PurchaseOrderStatus.APPROVED);
    order.setApprovedBy(approver);
    order.setApprovedAt(LocalDateTime.now());
    order.setApprovalNotes(notes);
    order.setUpdatedAt(LocalDateTime.now());
    purchaseOrderRepository.save(order);
  }

  @Transactional
  void rejectOrder(Long orderId, UUID rejecterUserId, String reason) {
    PurchaseOrder order = findOrderById(orderId);
    if (order.getStatus() != PurchaseOrderStatus.AWAITING_APPROVAL) {
      throw new IllegalStateException(
        "Only orders awaiting approval can be rejected"
      );
    }
    log.info("Rejecting order ID: {}. Reason: {}", orderId, reason);
    order.setStatus(PurchaseOrderStatus.REJECTED);
    order.setRejectionReason(reason);
    if (rejecterUserId != null) {
      userRepository.findById(rejecterUserId).ifPresent(order::setRejectedBy);
    }
    order.setUpdatedAt(LocalDateTime.now());
    purchaseOrderRepository.save(order);
  }

  @Transactional
  void sendToSupplier(Long orderId) {
    PurchaseOrder order = findOrderById(orderId);
    if (order.getStatus() != PurchaseOrderStatus.APPROVED) {
      throw new IllegalStateException(
        "Only approved orders can be sent to supplier"
      );
    }
    log.info("Sending order ID: {} to supplier", orderId);
    order.setStatus(PurchaseOrderStatus.IN_TRANSIT);
    order.setUpdatedAt(LocalDateTime.now());
    purchaseOrderRepository.save(order);
  }

  @Transactional
  void cancelOrderById(Long orderId, String reason) {
    PurchaseOrder order = findOrderById(orderId);
    if (
      order.getStatus() == PurchaseOrderStatus.DELIVERED ||
      order.getStatus() == PurchaseOrderStatus.CANCELLED ||
      order.getStatus() == PurchaseOrderStatus.REJECTED
    ) {
      throw new IllegalStateException(
        "Cannot cancel an order with status: " + order.getStatus()
      );
    }
    log.info(
      "Cancelling order ID: {}, current status: {}",
      orderId,
      order.getStatus()
    );
    order.setStatus(PurchaseOrderStatus.CANCELLED);
    order.setRejectionReason(reason);
    order.setUpdatedAt(LocalDateTime.now());

    Warehouse orderWarehouse = order.getDestinationWarehouse();
    if (orderWarehouse != null) {
      Map<Long, BigDecimal> receivedQtys = getTotalReceivedByOrder(orderId);
      for (PurchaseOrderItem item : order.getItems()) {
        BigDecimal orderedQty = BigDecimal.valueOf(item.getQuantity());
        BigDecimal receivedQty = receivedQtys.getOrDefault(
          item.getProduct().getId(),
          BigDecimal.ZERO
        );
        BigDecimal pendingQty = orderedQty.subtract(receivedQty);
        if (pendingQty.compareTo(BigDecimal.ZERO) > 0) {
          warehouseService.substractPendingStock(
            item.getProduct().getId(),
            orderWarehouse.getId(),
            pendingQty
          );
        }
      }
    }

    purchaseOrderRepository.save(order);
  }

  private Map<Long, BigDecimal> getTotalReceivedByOrder(Long orderId) {
    List<GoodReceipt> receipts = goodReceiptRepository.findByPurchaseOrderId(
      orderId
    );
    Map<Long, BigDecimal> received = new HashMap<>();
    for (GoodReceipt receipt : receipts) {
      for (GoodReceiptItem item : receipt.getItems()) {
        received.merge(
          item.getProduct().getId(),
          item.getReceivedQuantity(),
          BigDecimal::add
        );
      }
    }
    return received;
  }

  // ─────────────────────────────────────────────────────────────
  // Public API (used by controller)
  // ─────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public Page<PurchaseOrderResponse> getAllOrders(Pageable pageable) {
    return purchaseOrderRepository.findAll(pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public PurchaseOrderResponse getOrderById(Long id) {
    return toResponse(findOrderById(id));
  }

  @Transactional
  @CacheEvict(value = "dashboard", allEntries = true)
  public PurchaseOrderResponse submitOrderForApproval(Long id) {
    submitForApproval(id);
    return getOrderById(id);
  }

  /**
   * Approve by UUID — used when the caller already has the approver's UUID
   * (e.g. from a request body field like the reject endpoint).
   */
  @Transactional
  @CacheEvict(value = "dashboard", allEntries = true)
  public PurchaseOrderResponse markAsApproved(
    Long id,
    UUID approverUserId,
    String notes
  ) {
    approveOrder(id, approverUserId, notes);
    return getOrderById(id);
  }

  /**
   * FIX (BUG 3): New entry point used by the controller's /approve endpoint.
   * The controller only has access to UserDetails (email), not a UUID, because
   * the old extractUuidFrom(UserDetails) helper was never implemented.
   * We resolve the User by email here in the service layer instead.
   */
  @Transactional
  public PurchaseOrderResponse markAsApprovedByEmail(
    Long id,
    String approverEmail,
    String notes
  ) {
    User approver = userRepository
      .findByEmail(approverEmail)
      .orElseThrow(() ->
        new UsernameNotFoundException("Approver not found: " + approverEmail)
      );
    return markAsApproved(id, approver.getId(), notes);
  }

  @Transactional
  @CacheEvict(value = "dashboard", allEntries = true)
  public PurchaseOrderResponse rejectPurchaseOrder(
    Long id,
    String rejecterEmail,
    String reason
  ) {
    User rejecter = userRepository
      .findByEmail(rejecterEmail)
      .orElseThrow(() ->
        new UsernameNotFoundException("Rejecter not found: " + rejecterEmail)
      );
    rejectOrder(id, rejecter.getId(), reason);
    return getOrderById(id);
  }

  @Transactional
  public PurchaseOrderResponse markAsSentToSupplier(Long id) {
    sendToSupplier(id);
    return getOrderById(id);
  }

  @Transactional
  @CacheEvict(value = "dashboard", allEntries = true)
  public PurchaseOrderResponse cancelOrder(Long id, String reason) {
    cancelOrderById(id, reason);
    return getOrderById(id);
  }

  // ─────────────────────────────────────────────────────────────
  // Private helpers
  // ─────────────────────────────────────────────────────────────

  private PurchaseOrder findOrderById(Long id) {
    return purchaseOrderRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Purchase order not found: " + id)
      );
  }

  private PurchaseOrderResponse toResponse(PurchaseOrder order) {
    List<PurchaseOrderItemResponse> itemResponses = order
      .getItems()
      .stream()
      .map(item ->
        new PurchaseOrderItemResponse(
          item.getProduct().getId(),
          item.getProduct().getName(),
          item.getProduct().getSku(),
          item.getQuantity(),
          item.getUnitPrice(),
          item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        )
      )
      .toList();

    Warehouse wh = order.getDestinationWarehouse();
    return new PurchaseOrderResponse(
      order.getId(),
      order.getOrderNumber(),
      order.getDescription(),
      order.getStatus(),
      order.getSupplier() != null ? order.getSupplier().getName() : "Unknown",
      order.getPaymentMethod(),
      order.getType(),
      order.getCreatedBy() != null ? order.getCreatedBy().getName() : "Unknown",
      order.getCreatedAt(),
      order.getApprovalNotes(),
      order.getRejectionReason(),
      order.getApprovedBy() != null ? order.getApprovedBy().getName() : null,
      order.getApprovedAt(),
      order.getRequisition() != null ? order.getRequisition().getId() : null,
      wh != null ? wh.getId() : null,
      wh != null ? wh.getName() : null,
      order.getLeadTime() != null ? order.getLeadTime() : null,
      itemResponses
    );
  }
}
