package com.visco.backend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

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
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.SupplierRepository;
import com.visco.backend.repositories.UserRepository;

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

	// ─────────────────────────────────────────────────────────────
	// Creación de orden de compra
	// ─────────────────────────────────────────────────────────────

	@Transactional
	public PurchaseOrderResponse createPurchaseOrder(
			CreatePurchaseOrderRequest request) {

		Supplier supplier = supplierRepository.findById(request.supplierId())
				.orElseThrow(() -> new EntityNotFoundException(
						"Supplier not found: " + request.supplierId()));

		User createdBy = userRepository.findById(request.createdById())
				.orElseThrow(() -> new EntityNotFoundException(
						"User not found: " + request.createdById()));

		PurchaseOrder order = PurchaseOrder.builder()
				.orderNumber(request.orderNumber())
				.description(request.description())
				.createdBy(createdBy)
				.status(PurchaseOrderStatus.PENDING)
				.paymentMethod(request.paymentMethod())
				.type(request.type())
				.supplier(supplier)
				.createdAt(LocalDateTime.now())
				.build();

		for (PurchaseOrderItemRequest itemReq : request.items()) {
			Product product = productRepository.findById(itemReq.productId())
					.orElseThrow(() -> new EntityNotFoundException(
							"Product not found: " + itemReq.productId()));

			PurchaseOrderItem item = PurchaseOrderItem.builder()
					.purchaseOrder(order)
					.product(product)
					.quantity(itemReq.quantity())
					.unitPrice(itemReq.unitPrice())
					.build();

			order.getItems().add(item);
			warehouseService.addPendingStock(product.getId(),
					BigDecimal.valueOf(itemReq.quantity()));
		}

		PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
		return toResponse(savedOrder);
	}

	// ─────────────────────────────────────────────────────────────
	// Transiciones de estado de la orden
	// ─────────────────────────────────────────────────────────────

	@Transactional
	public void rejectOrder(Long orderId) {
		PurchaseOrder order = purchaseOrderRepository.findById(orderId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Purchase order not found: " + orderId));

		if (order.getStatus() != PurchaseOrderStatus.PENDING
				&& order.getStatus() != PurchaseOrderStatus.IN_TRANSIT) {
			throw new IllegalStateException(
					"Only pending or in-transit orders can be rejected");
		}
		log.info("Rejecting order ID: {}, current status: {}", orderId, order.getStatus());
		order.setStatus(PurchaseOrderStatus.REJECTED);
		order.setUpdatedAt(LocalDateTime.now());
		purchaseOrderRepository.save(order);

		for (PurchaseOrderItem item : order.getItems()) {
			warehouseService.substractPendingStock(item.getProduct().getId(),
					BigDecimal.valueOf(item.getQuantity()));
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

	public List<PurchaseOrderResponse> getAllOrders() {
		return purchaseOrderRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

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
		rejectOrder(id);
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
				order.getSupplier().getName(),
				order.getPaymentMethod(),
				order.getType(),
				order.getCreatedBy().getName(),
				order.getCreatedAt(),
				itemResponses);
	}
}
