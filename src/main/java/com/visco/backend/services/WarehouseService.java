package com.visco.backend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visco.backend.models.dtos.AdjustStockRequest;
import com.visco.backend.models.dtos.CreateWarehouseRequest;
import com.visco.backend.models.dtos.GoodReceiptItemResponse;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.ProductStockBreakdown;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.TransferStockRequest;
import com.visco.backend.models.dtos.WarehouseResponse;
import com.visco.backend.models.dtos.WarehouseStockSummary;
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
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.InventoryMovementRepository;
import com.visco.backend.repositories.LocationRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.repositories.WarehouseRepository;

import lombok.RequiredArgsConstructor;

// DIGITAL KARDEX
@Service
@RequiredArgsConstructor
public class WarehouseService {

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final GoodReceiptRepository goodReceiptRepository;
	private final StockLevelRepository stockLevelRepository;
	private final WarehouseRepository warehouseRepository;
	private final UserRepository userRepository;

	private final LocationRepository locationRepository;
	private final ProductRepository productRepository;
	private final InventoryMovementRepository inventoryMovementRepository;

	@Transactional
	public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {

		User manager = userRepository.findById(request.responsibleUserId())
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + request.responsibleUserId()));

		Warehouse warehouse = Warehouse.builder()
				.name(request.name())
				.physicalAddress(request.physicalAddress())
				.description(request.description())
				.sapCenterCode(request.sapCenterCode())
				.active(true)
				.responsibleUser(manager)
				.storageLocations(new HashSet<>())
				.build();

		Warehouse saved = warehouseRepository.save(warehouse);

		return WarehouseResponse.builder()
				.id(saved.getId())
				.name(saved.getName())
				.sapCenterCode(saved.getSapCenterCode())
				.build();
	}

	@Transactional
	public GoodReceiptResponse receiveGoods(Long orderId, ReceiveGoodsRequest request) {
		PurchaseOrder order = purchaseOrderRepository.findById(orderId)
				.orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + orderId));

		if (order.getStatus() == PurchaseOrderStatus.DELIVERED
				|| order.getStatus() == PurchaseOrderStatus.CANCELLED
				|| order.getStatus() == PurchaseOrderStatus.REJECTED) {
			throw new IllegalStateException("Cannot receive goods for an order with status: " + order.getStatus());
		}

		GoodReceipt receipt = GoodReceipt.builder()
				.receiptNumber("VIS-" + orderId + "-" + System.currentTimeMillis())
				.purchaseOrder(order)
				.receivedAt(LocalDateTime.now())
				.notes(request.notes())
				.build();

		Map<Long, BigDecimal> previousReceived = new HashMap<>();
		for (GoodReceipt prev : goodReceiptRepository.findByPurchaseOrderId(orderId)) {
			for (GoodReceiptItem prevItem : prev.getItems()) {
				previousReceived.merge(prevItem.getProduct().getId(), prevItem.getReceivedQuantity(), BigDecimal::add);
			}
		}

		// CORRECCIÓN: Extraemos el locationId del DTO de recepción para asentar el
		// inventario físico
		Long targetLocationId = request.destinationLocationId();

		for (ReceiveGoodsRequest.ReceiveItem itemReq : request.items()) {
			PurchaseOrderItem poItem = order.getItems().stream()
					.filter(i -> i.getProduct().getId().equals(itemReq.productId()))
					.findFirst()
					.orElseThrow(
							() -> new EntityNotFoundException("Product not found in order: " + itemReq.productId()));

			BigDecimal expected = BigDecimal.valueOf(poItem.getQuantity());
			BigDecimal received = itemReq.receivedQuantity();

			GoodReceiptItem item = GoodReceiptItem.builder()
					.goodReceipt(receipt)
					.product(poItem.getProduct())
					.expectedQuantity(expected)
					.receivedQuantity(received)
					.build();

			receipt.getItems().add(item);

			// CORRECCIÓN: Pasamos targetLocationId para cumplir con la firma multilocación
			addCurrentStock(poItem.getProduct().getId(), targetLocationId, received);
			substractPendingStock(poItem.getProduct().getId(), targetLocationId, received);
		}

		goodReceiptRepository.save(receipt);

		boolean allFullyReceived = determineIfFullyReceived(order, previousReceived, request);
		order.setStatus(allFullyReceived ? PurchaseOrderStatus.DELIVERED : PurchaseOrderStatus.PARTIALLY_DELIVERED);
		purchaseOrderRepository.save(order);

		return buildReceiptResponse(receipt, order);
	}

	public boolean determineIfFullyReceived(PurchaseOrder order, Map<Long, BigDecimal> previousReceived,
			ReceiveGoodsRequest request) {
		for (PurchaseOrderItem poItem : order.getItems()) {
			BigDecimal totalReceived = previousReceived.getOrDefault(poItem.getProduct().getId(), BigDecimal.ZERO);
			ReceiveGoodsRequest.ReceiveItem current = request.items().stream()
					.filter(r -> r.productId().equals(poItem.getProduct().getId()))
					.findFirst().orElse(null);
			if (current != null) {
				totalReceived = totalReceived.add(current.receivedQuantity());
			}
			if (totalReceived.compareTo(BigDecimal.valueOf(poItem.getQuantity())) < 0) {
				return false;
			}
		}
		return true;
	}

	public GoodReceiptResponse buildReceiptResponse(GoodReceipt receipt, PurchaseOrder order) {
		List<GoodReceiptItemResponse> itemResponses = receipt.getItems().stream()
				.map(item -> new GoodReceiptItemResponse(
						item.getProduct().getId(),
						item.getProduct().getName(),
						item.getProduct().getSku(),
						item.getExpectedQuantity(),
						item.getReceivedQuantity(),
						item.getReceivedQuantity().subtract(item.getExpectedQuantity())))
				.toList();

		return new GoodReceiptResponse(
				receipt.getId(),
				receipt.getReceiptNumber(),
				order.getId(),
				order.getOrderNumber(),
				order.getStatus(),
				receipt.getReceivedAt(),
				receipt.getNotes(),
				itemResponses);
	}

	// ─── Inventory Transfers ────────────────────────────────────

	@Transactional
	public void transferStock(TransferStockRequest request) {
		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

		Location fromLocation = locationRepository.findById(request.fromLocationId())
				.orElseThrow(() -> new EntityNotFoundException("Source location not found: " + request.fromLocationId()));

		Location toLocation = locationRepository.findById(request.toLocationId())
				.orElseThrow(() -> new EntityNotFoundException("Destination location not found: " + request.toLocationId()));

		User createdBy = userRepository.findById(request.createdById())
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + request.createdById()));

		if (fromLocation.getId().equals(toLocation.getId())) {
			throw new IllegalArgumentException("Source and destination locations must be different");
		}

		substractCurrentStock(product.getId(), fromLocation.getId(), request.quantity());
		addCurrentStock(product.getId(), toLocation.getId(), request.quantity());

		InventoryMovement movement = InventoryMovement.builder()
				.product(product)
				.fromLocation(fromLocation)
				.toLocation(toLocation)
				.quantity(request.quantity())
				.type(MovementType.TRANSFER)
				.createdAt(LocalDateTime.now())
				.createdBy(createdBy)
				.build();

		inventoryMovementRepository.save(movement);
	}

	// ─── Stock Adjustment ───────────────────────────────────────

	@Transactional
	public void adjustStock(AdjustStockRequest request) {
		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

		Location location = locationRepository.findById(request.locationId())
				.orElseThrow(() -> new EntityNotFoundException("Location not found: " + request.locationId()));

		User createdBy = userRepository.findById(request.createdById())
				.orElseThrow(() -> new EntityNotFoundException("User not found: " + request.createdById()));

		StockLevel level = getOrCreateStockLevel(product.getId(), location.getId());
		level.setCurrentStock(request.newStock());
		stockLevelRepository.save(level);

		InventoryMovement movement = InventoryMovement.builder()
				.product(product)
				.toLocation(location)
				.quantity(request.newStock())
				.type(MovementType.ADJUSTMENT)
				.reason(request.reason())
				.createdAt(LocalDateTime.now())
				.createdBy(createdBy)
				.build();

		inventoryMovementRepository.save(movement);
	}

	// ─────────────────────────────────────────────────────────────
	// Helpers de stock (Refactorizados para Multilocación)
	// ─────────────────────────────────────────────────────────────

	public void addCurrentStock(Long productId, Long locationId, BigDecimal quantity) {
		StockLevel level = getOrCreateStockLevel(productId, locationId);
		level.setCurrentStock(level.getCurrentStock().add(quantity));
		stockLevelRepository.save(level);
	}

	public void substractPendingStock(Long productId, Long locationId, BigDecimal quantity) {
		StockLevel level = getOrCreateStockLevel(productId, locationId);
		BigDecimal newPending = level.getPendingStock().subtract(quantity);
		level.setPendingStock(newPending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newPending);
		stockLevelRepository.save(level);
	}

	// Agrega este helper en tu WarehouseService si no lo habías pegado:
	public void substractPendingStockByWarehouse(Long productId, Long warehouseId, BigDecimal quantity) {
		Location defaultLocation = locationRepository.findFirstByWarehouseId(warehouseId)
				.orElseThrow(() -> new EntityNotFoundException(
						"El almacén no tiene ubicaciones configuradas para limpiar stock pendiente"));
		substractPendingStock(productId, defaultLocation.getId(), quantity);
	}

	public void addPendingStock(Long productId, Long locationId, BigDecimal quantity) {
		StockLevel level = getOrCreateStockLevel(productId, locationId);
		level.setPendingStock(level.getPendingStock().add(quantity));
		stockLevelRepository.save(level);
	}

	public void addPendingStockByWarehouse(Long productId, Long warehouseId, BigDecimal quantity) {
		Location defaultLocation = locationRepository.findFirstByWarehouseId(warehouseId)
				.orElseThrow(() -> new EntityNotFoundException(
						"El almacén no tiene ubicaciones configuradas para registrar stock pendiente"));

		addPendingStock(productId, defaultLocation.getId(), quantity);
	}

	public void substractCurrentStock(Long productId, Long locationId, BigDecimal quantity) {
		StockLevel level = stockLevelRepository.findByProductIdAndLocationId(productId, locationId)
				.orElseThrow(() -> new IllegalStateException(
						"No hay stock registrado para el producto " + productId + " en la ubicación " + locationId));

		if (level.getCurrentStock().compareTo(quantity) < 0) {
			throw new IllegalStateException("Stock insuficiente en la ubicación seleccionada");
		}

		level.setCurrentStock(level.getCurrentStock().subtract(quantity));
		stockLevelRepository.save(level);
	}

	private StockLevel getOrCreateStockLevel(Long productId, Long locationId) {
		return stockLevelRepository.findByProductIdAndLocationId(productId, locationId)
				.orElseGet(() -> {
					Product product = productRepository.getReferenceById(productId);
					Location location = locationRepository.getReferenceById(locationId);

					return StockLevel.builder()
							.product(product)
							.location(location)
							.currentStock(BigDecimal.ZERO)
							.pendingStock(BigDecimal.ZERO)
							.build();
				});
	}

	// ─── Consultas de solo lectura ──────────────────────────────

	@Transactional(readOnly = true)
	public List<GoodReceiptResponse> getReceiptsByOrderId(Long orderId) {
		return goodReceiptRepository.findByPurchaseOrderId(orderId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public Page<GoodReceiptResponse> getAllOrders(Pageable pageable) {
		return goodReceiptRepository.findAll(pageable)
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public GoodReceiptResponse getReceiptById(Long id) {
		GoodReceipt receipt = goodReceiptRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + id));
		return toResponse(receipt);
	}

	@Transactional(readOnly = true)
	public List<WarehouseResponse> getAllWarehouses() {
		return warehouseRepository.findAll().stream()
				.filter(Warehouse::isActive)
				.map(w -> WarehouseResponse.builder()
						.id(w.getId())
						.name(w.getName())
						.sapCenterCode(w.getSapCenterCode())
						.build())
				.toList();
	}

	@Transactional(readOnly = true)
	public ProductStockBreakdown getStockBreakdownByProduct(Long productId) {
		BigDecimal totalStock = stockLevelRepository.getTotalStockByProductId(productId);
		if (totalStock == null)
			totalStock = BigDecimal.ZERO;

		List<StockLevelRepository.WarehouseStockProjection> projections = stockLevelRepository
				.getStockByProductGroupedByWarehouse(productId);

		List<ProductStockBreakdown.WarehouseStockEntry> entries = projections.stream()
				.map(p -> ProductStockBreakdown.WarehouseStockEntry.builder()
						.warehouseId(p.getWarehouseId())
						.warehouseName(p.getWarehouseName())
						.currentStock(p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO)
						.pendingStock(p.getPendingStock() != null ? p.getPendingStock() : BigDecimal.ZERO)
						.build())
				.toList();

		BigDecimal totalPending = entries.stream()
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
		List<StockLevelRepository.WarehouseStockProjection> projections = stockLevelRepository
				.getGlobalStockByWarehouse();

		return projections.stream()
				.map(p -> WarehouseStockSummary.builder()
						.warehouseId(p.getWarehouseId())
						.warehouseName(p.getWarehouseName())
						.totalStock(p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO)
						.totalPendingStock(p.getPendingStock() != null ? p.getPendingStock() : BigDecimal.ZERO)
						.build())
				.toList();
	}

	private GoodReceiptResponse toResponse(GoodReceipt receipt) {
		List<GoodReceiptItemResponse> itemResponses = receipt.getItems().stream()
				.map(item -> new GoodReceiptItemResponse(
						item.getProduct().getId(),
						item.getProduct().getName(),
						item.getProduct().getSku(),
						item.getExpectedQuantity(),
						item.getReceivedQuantity(),
						item.getReceivedQuantity().subtract(item.getExpectedQuantity())))
				.toList();

		return new GoodReceiptResponse(
				receipt.getId(),
				receipt.getReceiptNumber(),
				receipt.getPurchaseOrder().getId(),
				receipt.getPurchaseOrder().getOrderNumber(),
				receipt.getPurchaseOrder().getStatus(),
				receipt.getReceivedAt(),
				receipt.getNotes(),
				itemResponses);
	}
}