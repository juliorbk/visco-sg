package com.visco.backend.services;

import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemResponse;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.Requisition;
import com.visco.backend.models.entities.RequisitionStatus;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.RequisitionRepository;
import com.visco.backend.repositories.SupplierRepository;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.repositories.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  // ─────────────────────────────────────────────────────────────
  // Creación de orden de compra
  // ─────────────────────────────────────────────────────────────

  @Transactional
  public PurchaseOrderResponse createPurchaseOrder(
    CreatePurchaseOrderRequest request
  ) {
    // Mapeamos que existan las entidades de la orden de compra
    // Proveedor
    Supplier supplier = supplierRepository
      .findById(request.supplierId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Supplier not found: " + request.supplierId()
        )
      );
    // Creador
    User createdBy = userRepository
      .findById(request.createdById())
      .orElseThrow(() ->
        new EntityNotFoundException("User not found: " + request.createdById())
      );
    // Warehouse destino
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

    // Link requisition if provided
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

      // Usa addPendingStockByWarehouse (el supplier de la orden define el almacén destino)
      // Por ahora incrementa el stock pendiente sin filtrar por warehouse específico,
      // igual que antes — cuando agregues warehouses múltiples puedes pasar el ID aquí.
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
  // Transiciones de estado
  // ─────────────────────────────────────────────────────────────

  @Transactional
  public void submitForApproval(Long orderId) {
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
  public void approveOrder(Long orderId, UUID approverUserId, String notes) {
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
  public void rejectOrder(Long orderId, UUID rejecterUserId, String reason) {
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
      userRepository.findById(rejecterUserId).ifPresent(order::setApprovedBy);
    }
    order.setUpdatedAt(LocalDateTime.now());
    purchaseOrderRepository.save(order);
  }

  @Transactional
  public void sendToSupplier(Long orderId) {
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
  public void cancelOrderById(Long orderId, String reason) {
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
    purchaseOrderRepository.save(order);

    for (PurchaseOrderItem item : order.getItems()) {
      warehouseService.substractPendingStock(
        item.getProduct().getId(),
        BigDecimal.valueOf(item.getQuantity())
      );
    }
  }

  // ─────────────────────────────────────────────────────────────
  // Consultas
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
  public PurchaseOrderResponse submitOrderForApproval(Long id) {
    submitForApproval(id);
    return getOrderById(id);
  }

  @Transactional
  public PurchaseOrderResponse markAsApproved(
    Long id,
    UUID approverUserId,
    String notes
  ) {
    approveOrder(id, approverUserId, notes);
    return getOrderById(id);
  }

  @Transactional
  public PurchaseOrderResponse rejectPurchaseOrder(
    Long id,
    UUID rejecterUserId,
    String reason
  ) {
    rejectOrder(id, rejecterUserId, reason);
    return getOrderById(id);
  }

  @Transactional
  public PurchaseOrderResponse markAsSentToSupplier(Long id) {
    sendToSupplier(id);
    return getOrderById(id);
  }

  @Transactional
  public PurchaseOrderResponse cancelOrder(Long id, String reason) {
    cancelOrderById(id, reason);
    return getOrderById(id);
  }

  private PurchaseOrder findOrderById(Long id) {
    return purchaseOrderRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Purchase order not found: " + id)
      );
  }

  // ─────────────────────────────────────────────────────────────
  // Mapper
  // ─────────────────────────────────────────────────────────────

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
      order.getLeadTime() != null ? order.getLeadTime() : null,
      itemResponses
    );
  }
}
