package com.visco.backend.services;

import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemResponse;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.SupplierRepository;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.repositories.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ─────────────────────────────────────────────────────────────
    // Creación de orden de compra
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request) {
        // Mapeamos que existan las entidades de la orden de compra
        // Proveedor
        Supplier supplier = supplierRepository
            .findById(request.supplierId())
            .orElseThrow(() ->
                new EntityNotFoundException("Supplier not found: " + request.supplierId())
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
            .createdAt(LocalDateTime.now())
            .build();

        List<Long> productIds = request
            .items()
            .stream()
            .map(PurchaseOrderItemRequest::productId)
            .toList();

        Map<Long, Product> productMap = productRepository
            .findAllById(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, (p) -> p));

        for (PurchaseOrderItemRequest itemReq : request.items()) {
            Product product = productMap.get(itemReq.productId());
            if (product == null) {
                throw new EntityNotFoundException("Product not found: " + itemReq.productId());
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
    public void cancelOrderById(Long orderId) {
        PurchaseOrder order = purchaseOrderRepository
            .findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + orderId));

        if (
            order.getStatus() != PurchaseOrderStatus.PENDING &&
            order.getStatus() != PurchaseOrderStatus.IN_TRANSIT
        ) {
            throw new IllegalStateException("Only pending or in-transit orders can be cancelled");
        }

        log.info("Cancelling order ID: {}, current status: {}", orderId, order.getStatus());
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(order);

        for (PurchaseOrderItem item : order.getItems()) {
            warehouseService.substractPendingStock(
                item.getProduct().getId(),
                BigDecimal.valueOf(item.getQuantity())
            );
        }
    }

    @Transactional
    public void approveOrder(Long orderId) {
        PurchaseOrder order = purchaseOrderRepository
            .findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + orderId));

        if (order.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be approved");
        }

        log.info("Approving order ID: {}, current status: {}", orderId, order.getStatus());
        order.setStatus(PurchaseOrderStatus.IN_TRANSIT);
        order.setUpdatedAt(LocalDateTime.now());
        purchaseOrderRepository.save(order);
    }

    // ─────────────────────────────────────────────────────────────
    // Consultas
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAllOrders() {
        return purchaseOrderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getOrderById(Long id) {
        PurchaseOrder order = purchaseOrderRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + id));
        return toResponse(order);
    }

    @Transactional
    public PurchaseOrderResponse markAsApproved(Long id) {
        approveOrder(id);
        return getOrderById(id);
    }

    @Transactional
    public PurchaseOrderResponse cancelOrder(Long id) {
        cancelOrderById(id);
        return getOrderById(id);
    }

    // ─────────────────────────────────────────────────────────────
    // Mapper
    // ─────────────────────────────────────────────────────────────

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {
        List<PurchaseOrderItemResponse> itemResponses = order
            .getItems()
            .stream()
            .map((item) ->
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
            itemResponses
        );
    }
}
