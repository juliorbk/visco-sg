package com.visco.backend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementService {

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final SupplierRepository supplierRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final WarehouseService warehouseService;
	private final WarehouseRepository warehouseRepository;

	// ─────────────────────────────────────────────────────────────
	// Creación de orden de compra
	// ─────────────────────────────────────────────────────────────

	@Transactional
	public PurchaseOrderResponse createPurchaseOrder(
			CreatePurchaseOrderRequest request) {

		Supplier supplier = supplierRepository.findById(request.supplierId())
				.orElseThrow(() -> new EntityNotFoundException(
						"Supplier not found: " + request.supplierId()));

		Warehouse destinationWarehouse = warehouseRepository.findById(request.destinationWarehouse()).orElseThrow(
				() -> new EntityNotFoundException("Warehouse not found: " + request.destinationWarehouse()));

		User createdBy = userRepository.findById(request.createdById())
				.orElseThrow(() -> new EntityNotFoundException(
						"User not found: " + request.createdById()));

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

		// 1. Extract all product IDs from the request
		List<Long> productIds = request.items().stream()
				.map(PurchaseOrderItemRequest::productId)
				.toList();

		// 2. Fetch all products in ONE database query
		List<Product> products = productRepository.findAllById(productIds);

		// 3. Create a map for quick lookup (Usa java.util.stream.Collectors si no
		// tienes el import)
		Map<Long, Product> productMap = products.stream()
				.collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));

		// 4. Now loop through your items
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

			// CORRECCIÓN: Convertir la cantidad a BigDecimal para cumplir con la firma del
			// WarehouseService
			BigDecimal pendingQuantity = BigDecimal.valueOf(itemReq.quantity());

			warehouseService.addPendingStockByWarehouse(product.getId(), request.destinationWarehouse(),
					pendingQuantity);
		}

		// Importante: Asegúrate de que la relación @OneToMany en PurchaseOrder tenga
		// cascade = CascadeType.ALL
		// para que al guardar la orden se guarden automáticamente todos los
		// PurchaseOrderItem.
		PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
		return toResponse(savedOrder);
	}

	// ─────────────────────────────────────────────────────────────
	// Transiciones de estado de la orden
	// ─────────────────────────────────────────────────────────────

	@Transactional
	public void cancelOrderById(Long orderId) {
		PurchaseOrder order = purchaseOrderRepository.findById(orderId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Purchase order not found: " + orderId));

		if (order.getStatus() != PurchaseOrderStatus.PENDING
				&& order.getStatus() != PurchaseOrderStatus.IN_TRANSIT) {
			throw new IllegalStateException(
					"Only pending or in-transit orders can be cancelled");
		}

		log.info("Cancelling order ID: {}, current status: {}", orderId, order.getStatus());
		order.setStatus(PurchaseOrderStatus.CANCELLED);
		purchaseOrderRepository.save(order);

		for (PurchaseOrderItem item : order.getItems()) {
			// Convertimos la cantidad a BigDecimal
			BigDecimal quantityToSubstract = BigDecimal.valueOf(item.getQuantity());

			// CORRECCIÓN: Llamamos al helper que entiende "Almacén" en lugar de ubicación
			// exacta
			warehouseService.substractPendingStockByWarehouse(
					item.getProduct().getId(),
					order.getDestinationWarehouse().getId(),
					quantityToSubstract);
		}
	}

	@Transactional
	public void approveOrder(Long orderId) {
		PurchaseOrder order = purchaseOrderRepository.findById(orderId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Purchase order not found: " + orderId));

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
	public Page<PurchaseOrderResponse> getAllOrders(Pageable pageable) {
		return purchaseOrderRepository.findAll(pageable)
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public PurchaseOrderResponse getOrderById(Long id) {
		PurchaseOrder order = purchaseOrderRepository.findById(id)
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
	// Mapper: PurchaseOrder → PurchaseOrderResponse
	// ─────────────────────────────────────────────────────────────
	private PurchaseOrderResponse toResponse(PurchaseOrder order) {
		List<PurchaseOrderItemResponse> itemResponses = order.getItems()
				.stream()
				.map(item -> new PurchaseOrderItemResponse(
						item.getProduct().getId(), item.getProduct().getName(),
						item.getProduct().getSku(), item.getQuantity(),
						item.getUnitPrice(),
						item.getUnitPrice().multiply(
								BigDecimal.valueOf(item.getQuantity()))))
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
				itemResponses);
	}
}
