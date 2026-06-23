package com.visco.backend.services;

import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.ProductPurchaseOrderSummary;
import com.visco.backend.models.dtos.PurchaseOrderItemRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemResponse;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.Requisition;
import com.visco.backend.models.entities.RequisitionItem;
import com.visco.backend.models.entities.RequisitionStatus;
import com.visco.backend.models.entities.Supplier;
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

/**
 * Handles business logic for purchase order procurement operations.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ProcurementService {

    private static final java.math.BigDecimal TAX_RATE = new java.math.BigDecimal("0.16");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WarehouseService warehouseService;
    private final RequisitionRepository requisitionRepository;
    private final GoodReceiptRepository goodReceiptRepository;
    private final StockLevelRepository stockLevelRepository;

    // ─────────────────────────────────────────────────────────────
    // Create purchase order
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates a purchase order from a request with items and optional requisition.
     *
     * <p>When {@code request.requisitionId()} is provided, each item must
     * include the {@code requisitionItemId} it is fulfilling. The service
     * validates that the sum of already-awarded quantities across previous
     * POs (excluding CANCELLED / REJECTED) plus the new quantity does not
     * exceed the requisition item's requested quantity. After persisting the
     * PO, the source requisition's status is recomputed:
     *
     * <ul>
     *   <li>Every item fully awarded → {@link RequisitionStatus#CONVERTED}</li>
     *   <li>Some items awarded, others pending → {@link RequisitionStatus#PARTIALLY_CONVERTED}</li>
     *   <li>No items awarded (defensive) → kept as {@link RequisitionStatus#APPROVED}</li>
     * </ul>
     *
     * @param request the purchase order creation request
     * @return the created purchase order response
     */
    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request) {
        Supplier supplier = supplierRepository
            .findById(request.supplierId())
            .orElseThrow(() ->
                new EntityNotFoundException("Supplier not found: " + request.supplierId())
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
            .orderNumber(request.orderNumber().trim())
            .description(request.description())
            .createdBy(createdBy)
            .destinationWarehouse(destinationWarehouse)
            .status(PurchaseOrderStatus.PENDING)
            .paymentMethod(request.paymentMethod())
            .type(request.type())
            .supplier(supplier)
            .leadTime(request.leadTime())
            .shipConditions(request.shipConditions())
            .createdAt(LocalDateTime.now())
            .build();

        Requisition requisition = null;
        if (request.requisitionId() != null) {
            requisition = requisitionRepository
                .findById(request.requisitionId())
                .orElseThrow(() ->
                    new EntityNotFoundException("Requisition not found: " + request.requisitionId())
                );
            if (
                requisition.getStatus() != RequisitionStatus.APPROVED &&
                requisition.getStatus() != RequisitionStatus.PARTIALLY_CONVERTED
            ) {
                throw new IllegalStateException(
                    "Only APPROVED or PARTIALLY_CONVERTED requisitions can receive new POs. " +
                    "Current status: " + requisition.getStatus()
                );
            }
            order.setRequisition(requisition);
        }

        List<Long> productIds = request
            .items()
            .stream()
            .map(PurchaseOrderItemRequest::productId)
            .toList();

        Map<Long, Product> productMap = productRepository
            .findAllById(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, (p) -> p));

        // Build an index of the requisition's items for fast lookups when
        // validating award quantities.
        Map<Long, RequisitionItem> requisitionItemIndex = new HashMap<>();
        if (requisition != null) {
            for (RequisitionItem ri : requisition.getItems()) {
                requisitionItemIndex.put(ri.getId(), ri);
            }
        }

        for (PurchaseOrderItemRequest itemReq : request.items()) {
            Product product = productMap.get(itemReq.productId());
            if (product == null) {
                throw new EntityNotFoundException("Product not found: " + itemReq.productId());
            }

            RequisitionItem requisitionItem = null;
            if (requisition != null) {
                if (itemReq.requisitionItemId() == null) {
                    throw new IllegalArgumentException(
                        "requisitionItemId is required on every item when the PO is " +
                        "linked to a requisition"
                    );
                }
                requisitionItem = requisitionItemIndex.get(itemReq.requisitionItemId());
                if (requisitionItem == null) {
                    throw new EntityNotFoundException(
                        "Requisition item " + itemReq.requisitionItemId() +
                        " does not belong to requisition " + requisition.getId()
                    );
                }
                if (
                    !requisitionItem.getProduct().getId().equals(product.getId())
                ) {
                    throw new IllegalArgumentException(
                        "Product " + product.getId() + " on PO item does not match " +
                        "the product (" + requisitionItem.getProduct().getId() +
                        ") of requisition item " + requisitionItem.getId()
                    );
                }

                BigDecimal alreadyAwarded = purchaseOrderRepository
                    .sumAwardedQuantityByRequisitionItemId(requisitionItem.getId());
                if (alreadyAwarded == null) alreadyAwarded = BigDecimal.ZERO;
                BigDecimal newTotal = alreadyAwarded.add(itemReq.quantity());
                if (newTotal.compareTo(requisitionItem.getQuantity()) > 0) {
                    throw new IllegalStateException(
                        "Over-award on requisition item " + requisitionItem.getId() +
                        " (product " + product.getId() + "). Requested: " +
                        requisitionItem.getQuantity() + ", already awarded: " +
                        alreadyAwarded + ", attempted to add: " + itemReq.quantity()
                    );
                }
            }

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(order)
                .product(product)
                .requisitionItem(requisitionItem)
                .quantity(itemReq.quantity())
                .unitPrice(itemReq.unitPrice())
                .build();

            order.getItems().add(item);

            warehouseService.addPendingStockByWarehouse(
                product.getId(),
                request.destinationWarehouseId(),
                itemReq.quantity()
            );
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        if (requisition != null) {
            recomputeRequisitionStatus(requisition);
        }

        return toResponse(savedOrder);
    }

    /**
     * Recomputes a requisition's lifecycle status from its current award
     * progress. Called after every PO is created from a requisition.
     *
     * <p>Mutates the passed-in entity and persists the change.
     */
    private void recomputeRequisitionStatus(Requisition requisition) {
        // Refresh items from DB so we see the lines added in this transaction
        // (some of them are referenced by the new PO we just saved).
        Requisition fresh = requisitionRepository
            .findByIdDetailed(requisition.getId())
            .orElse(requisition);

        Map<Long, BigDecimal> awardedByItem = purchaseOrderRepository
            .sumAwardedByRequisitionId(fresh.getId())
            .stream()
            .collect(
                Collectors.toMap(
                    PurchaseOrderRepository.AwardedQuantityProjection::getRequisitionItemId,
                    PurchaseOrderRepository.AwardedQuantityProjection::getAwardedQuantity
                )
            );

        boolean anyAwarded = false;
        boolean allFullyAwarded = !fresh.getItems().isEmpty();

        for (RequisitionItem ri : fresh.getItems()) {
            BigDecimal awarded = awardedByItem.getOrDefault(ri.getId(), BigDecimal.ZERO);
            if (awarded.compareTo(BigDecimal.ZERO) > 0) anyAwarded = true;
            if (awarded.compareTo(ri.getQuantity()) < 0) allFullyAwarded = false;
        }

        RequisitionStatus newStatus;
        if (fresh.getItems().isEmpty()) {
            newStatus = RequisitionStatus.APPROVED;
        } else if (allFullyAwarded) {
            newStatus = RequisitionStatus.CONVERTED;
        } else if (anyAwarded) {
            newStatus = RequisitionStatus.PARTIALLY_CONVERTED;
        } else {
            newStatus = RequisitionStatus.APPROVED;
        }

        if (fresh.getStatus() != newStatus) {
            log.info(
                "Requisition {} status transition: {} -> {}",
                fresh.getRequisitionNumber(),
                fresh.getStatus(),
                newStatus
            );
            fresh.setStatus(newStatus);
            requisitionRepository.save(fresh);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Internal state transitions (package-private for clarity)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    PurchaseOrder submitForApproval(Long orderId) {
        PurchaseOrder order = findOrderById(orderId);
        if (order.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be submitted for approval");
        }
        log.info("Submitting order ID: {} for approval", orderId);
        order.setStatus(PurchaseOrderStatus.AWAITING_APPROVAL);
        order.setUpdatedAt(LocalDateTime.now());
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    PurchaseOrder approveOrder(Long orderId, UUID approverUserId, String notes) {
        PurchaseOrder order = findOrderById(orderId);
        if (order.getStatus() != PurchaseOrderStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("Only orders awaiting approval can be approved");
        }
        User approver = userRepository
            .findById(approverUserId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + approverUserId));
        log.info("Approving order ID: {} by user: {}", orderId, approver.getName());
        order.setStatus(PurchaseOrderStatus.APPROVED);
        order.setApprovedBy(approver);
        order.setApprovedAt(LocalDateTime.now());
        order.setApprovalNotes(notes);
        order.setUpdatedAt(LocalDateTime.now());
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    PurchaseOrder rejectOrder(Long orderId, UUID rejecterUserId, String reason) {
        PurchaseOrder order = findOrderById(orderId);
        if (order.getStatus() != PurchaseOrderStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("Only orders awaiting approval can be rejected");
        }
        log.info("Rejecting order ID: {}. Reason: {}", orderId, reason);
        order.setStatus(PurchaseOrderStatus.REJECTED);
        order.setRejectionReason(reason);
        if (rejecterUserId != null) {
            userRepository.findById(rejecterUserId).ifPresent(order::setRejectedBy);
        }
        order.setUpdatedAt(LocalDateTime.now());
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    PurchaseOrder sendToSupplier(Long orderId) {
        PurchaseOrder order = findOrderById(orderId);
        if (order.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new IllegalStateException("Only approved orders can be sent to supplier");
        }
        log.info("Sending order ID: {} to supplier", orderId);
        order.setStatus(PurchaseOrderStatus.IN_TRANSIT);
        order.setUpdatedAt(LocalDateTime.now());
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    PurchaseOrder cancelOrderById(Long orderId, String reason) {
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
        log.info("Cancelling order ID: {}, current status: {}", orderId, order.getStatus());
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        order.setRejectionReason(reason);
        order.setUpdatedAt(LocalDateTime.now());

        Warehouse orderWarehouse = order.getDestinationWarehouse();
        if (orderWarehouse != null) {
            Map<Long, BigDecimal> receivedQtys = getTotalReceivedByOrder(orderId);

            for (PurchaseOrderItem item : order.getItems()) {
                BigDecimal orderedQty = item.getQuantity();
                BigDecimal receivedQty = receivedQtys.getOrDefault(
                    item.getProduct().getId(),
                    BigDecimal.ZERO
                );
                BigDecimal pendingQty = orderedQty.subtract(receivedQty);

                if (pendingQty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal currentPending = stockLevelRepository.getPendingStock(
                        item.getProduct().getId(),
                        orderWarehouse.getId()
                    );
                    if (currentPending != null && currentPending.compareTo(pendingQty) < 0) {
                        throw new IllegalStateException(
                            "Insufficient pending stock for product " +
                                item.getProduct().getId() +
                                ". Available: " +
                                currentPending +
                                ", Required: " +
                                pendingQty
                        );
                    }
                }
            }

            // Una cancelación solo revierte lo NO recibido. La mercancía ya
            // recibida sigue físicamente en el almacén, por lo que NO se
            // debe tocar current_stock (eso se hace vía Return/Disposal, no
            // aquí). Solo se resta el pending restante.
            for (PurchaseOrderItem item : order.getItems()) {
                BigDecimal orderedQty = item.getQuantity();
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

        PurchaseOrder saved = purchaseOrderRepository.save(order);

        // Cancelling a PO reduces the awarded quantity for any source
        // requisition, so we may need to step the requisition status back
        // from CONVERTED -> PARTIALLY_CONVERTED or APPROVED.
        if (saved.getRequisition() != null) {
            recomputeRequisitionStatus(saved.getRequisition());
        }

        return saved;
    }

    private Map<Long, BigDecimal> getTotalReceivedByOrder(Long orderId) {
        return goodReceiptRepository
            .getTotalReceivedByOrder(orderId)
            .stream()
            .collect(
                Collectors.toMap(
                    GoodReceiptRepository.ReceivedQuantityProjection::getProductId,
                    GoodReceiptRepository.ReceivedQuantityProjection::getTotalReceived
                )
            );
    }

    // ─────────────────────────────────────────────────────────────
    // Public API (used by controller)
    // ─────────────────────────────────────────────────────────────

    /**
     * Retrieves a paginated list of all purchase orders.
     *
     * @param pageable pagination information
     * @return page of purchase order responses
     */
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> getAllOrders(Pageable pageable) {
        return purchaseOrderRepository.findAllWithFetch(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a paginated list of purchase orders that include the given
     * product in their line items, ordered by creation date desc.
     *
     * @param productId the product ID
     * @param pageable  pagination information
     * @return page of lightweight PO summaries scoped to that product
     */
    @Transactional(readOnly = true)
    public Page<ProductPurchaseOrderSummary> getOrdersByProduct(
        Long productId,
        Pageable pageable
    ) {
        return purchaseOrderRepository.findProductPurchaseOrders(productId, pageable);
    }

    /**
     * Retrieves a purchase order by its ID.
     *
     * @param id the purchase order ID
     * @return the purchase order response
     */
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getOrderById(Long id) {
        return toResponse(findOrderById(id));
    }

    /**
     * Submits a pending order for approval workflow.
     *
     * @param id the purchase order ID
     * @return the updated purchase order response
     */
    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public PurchaseOrderResponse submitOrderForApproval(Long id) {
        return toResponse(submitForApproval(id));
    }

    /**
     * Approve by UUID — used when the caller already has the approver's UUID
     * (e.g. from a request body field like the reject endpoint).
     */
    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public PurchaseOrderResponse markAsApproved(Long id, UUID approverUserId, String notes) {
        return toResponse(approveOrder(id, approverUserId, notes));
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
        return toResponse(approveOrder(id, approver.getId(), notes));
    }

    /**
     * Rejects a purchase order that is awaiting approval.
     *
     * @param id            the purchase order ID
     * @param rejecterEmail the email of the rejecting user
     * @param reason        the rejection reason
     * @return the updated purchase order response
     */
    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public PurchaseOrderResponse rejectPurchaseOrder(Long id, String rejecterEmail, String reason) {
        UUID rejecterId = null;
        if (rejecterEmail != null) {
            User rejecter = userRepository
                .findByEmail(rejecterEmail)
                .orElseThrow(() ->
                    new UsernameNotFoundException("Rejecter not found: " + rejecterEmail)
                );
            rejecterId = rejecter.getId();
        }
        return toResponse(rejectOrder(id, rejecterId, reason));
    }

    /**
     * Marks an approved order as sent to the supplier (in-transit).
     *
     * @param id the purchase order ID
     * @return the updated purchase order response
     */
    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public PurchaseOrderResponse markAsSentToSupplier(Long id) {
        return toResponse(sendToSupplier(id));
    }

    /**
     * Cancels a purchase order and adjusts pending/current stock.
     *
     * @param id     the purchase order ID
     * @param reason the cancellation reason
     * @return the updated purchase order response
     */
    @Transactional
    @CacheEvict(value = "dashboard", allEntries = true)
    public PurchaseOrderResponse cancelOrder(Long id, String reason) {
        return toResponse(cancelOrderById(id, reason));
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private PurchaseOrder findOrderById(Long id) {
        return purchaseOrderRepository
            .findByIdDetailed(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {
        List<PurchaseOrderItemResponse> itemResponses = order
            .getItems()
            .stream()
            .map((item) ->
                new PurchaseOrderItemResponse(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getSku(),
                    item.getProduct().getInternalCode(),
                    item.getProduct().getSapCode(),
                    item.getProduct().getUom().name(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getUnitPrice().multiply(item.getQuantity()),
                    item.getRequisitionItem() != null
                        ? item.getRequisitionItem().getId()
                        : null
                )
            )
            .toList();

        BigDecimal subtotal = itemResponses
            .stream()
            .map(PurchaseOrderItemResponse::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount).setScale(2, java.math.RoundingMode.HALF_UP);

        Warehouse wh = order.getDestinationWarehouse();
        return new PurchaseOrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getDescription(),
            order.getStatus(),
            order.getSupplier() != null ? order.getSupplier().getName() : "Unknown",
            order.getSupplier() != null ? order.getSupplier().getTaxId() : null,
            order.getPaymentMethod(),
            order.getType(),
            order.getRequisition() != null ? order.getRequisition().getRequisitionNumber() : null,
            order.getShipConditions(),
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
            subtotal,
            taxAmount,
            totalAmount,
            PurchaseOrderResponse.SupplierInfo.fromEntity(order.getSupplier()),
            PurchaseOrderResponse.WarehouseInfo.fromEntity(wh),
            itemResponses
        );
    }
}
