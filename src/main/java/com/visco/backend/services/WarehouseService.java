package com.visco.backend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.visco.backend.models.dtos.CreateWarehouseRequest;
import com.visco.backend.models.dtos.GoodReceiptItemResponse;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.ProductStockBreakdown;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.WarehouseDTO;
import com.visco.backend.models.dtos.WarehouseResponse;
import com.visco.backend.models.dtos.WarehouseStockSummary;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.repositories.WarehouseRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WarehouseService {

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final GoodReceiptRepository goodReceiptRepository;
	private final StockLevelRepository stockLevelRepository;
	private final WarehouseRepository warehouseRepository;
	private final UserRepository userRepository;

	// ─────────────────────────────────────────────────────────────
	// Warehouse CRUD
	// ─────────────────────────────────────────────────────────────

	@Transactional
	public WarehouseDTO createWarehouse(CreateWarehouseRequest request) {
		User responsible = userRepository.findById(request.responsibleUserId())
				.orElseThrow(() -> new EntityNotFoundException(
						"User not found: " + request.responsibleUserId()));

		Warehouse warehouse =
				Warehouse.builder().name(request.name()).physicalAddress(request.physicalAddress())
						.description(request.description()).sapCenterCode(request.sapCenterCode())
						.responsibleUser(responsible).active(true).build();

		return WarehouseDTO.fromEntity(warehouseRepository.save(warehouse));
	}

	@Transactional(readOnly = true)
	public List<WarehouseResponse> getAllWarehouses() {
		return warehouseRepository.findAll().stream().filter(Warehouse::isActive)
				.map(w -> WarehouseResponse.builder().id(w.getId()).name(w.getName())
						.sapCenterCode(w.getSapCenterCode()).build())
				.toList();
	}

	// ─────────────────────────────────────────────────────────────
	// Goods receiving
	// ─────────────────────────────────────────────────────────────

	@Transactional
	public GoodReceiptResponse receiveGoods(Long orderId, ReceiveGoodsRequest request) {
		PurchaseOrder order = purchaseOrderRepository.findById(orderId).orElseThrow(
				() -> new EntityNotFoundException("Purchase order not found: " + orderId));

		if (order.getStatus() == PurchaseOrderStatus.DELIVERED
				|| order.getStatus() == PurchaseOrderStatus.CANCELLED
				|| order.getStatus() == PurchaseOrderStatus.REJECTED) {
			throw new IllegalStateException(
					"Cannot receive goods for an order with status: " + order.getStatus());
		}

		GoodReceipt receipt = GoodReceipt.builder()
				.receiptNumber("VIS-" + orderId + "-" + System.currentTimeMillis())
				.purchaseOrder(order).receivedAt(LocalDateTime.now()).notes(request.notes())
				.build();

		Map<Long, BigDecimal> previousReceived = new HashMap<>();
		for (GoodReceipt prev : goodReceiptRepository.findByPurchaseOrderId(orderId)) {
			for (GoodReceiptItem prevItem : prev.getItems()) {
				previousReceived.merge(prevItem.getProduct().getId(),
						prevItem.getReceivedQuantity(), BigDecimal::add);
			}
		}

		for (ReceiveGoodsRequest.ReceiveItem itemReq : request.items()) {
			PurchaseOrderItem poItem = order.getItems().stream()
					.filter(i -> i.getProduct().getId().equals(itemReq.productId())).findFirst()
					.orElseThrow(() -> new EntityNotFoundException(
							"Product not found in order: " + itemReq.productId()));

			BigDecimal expected = BigDecimal.valueOf(poItem.getQuantity());
			BigDecimal received = itemReq.receivedQuantity();

			GoodReceiptItem item =
					GoodReceiptItem.builder().goodReceipt(receipt).product(poItem.getProduct())
							.expectedQuantity(expected).receivedQuantity(received).build();

			receipt.getItems().add(item);
			addCurrentStock(poItem.getProduct().getId(), received);
			substractPendingStock(poItem.getProduct().getId(), received);
		}

		goodReceiptRepository.save(receipt);

		boolean allFullyReceived = determineIfFullyReceived(order, previousReceived, request);
		order.setStatus(allFullyReceived ? PurchaseOrderStatus.DELIVERED
				: PurchaseOrderStatus.PARTIALLY_DELIVERED);
		purchaseOrderRepository.save(order);

		return buildReceiptResponse(receipt, order);
	}

	public boolean determineIfFullyReceived(PurchaseOrder order,
			Map<Long, BigDecimal> previousReceived, ReceiveGoodsRequest request) {
		for (PurchaseOrderItem poItem : order.getItems()) {
			BigDecimal totalReceived =
					previousReceived.getOrDefault(poItem.getProduct().getId(), BigDecimal.ZERO);
			ReceiveGoodsRequest.ReceiveItem current = request.items().stream()
					.filter(r -> r.productId().equals(poItem.getProduct().getId())).findFirst()
					.orElse(null);
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
				.map(item -> new GoodReceiptItemResponse(item.getProduct().getId(),
						item.getProduct().getName(), item.getProduct().getSku(),
						item.getExpectedQuantity(), item.getReceivedQuantity(),
						item.getReceivedQuantity().subtract(item.getExpectedQuantity())))
				.toList();

		return new GoodReceiptResponse(receipt.getId(), receipt.getReceiptNumber(), order.getId(),
				order.getOrderNumber(), order.getStatus(), receipt.getReceivedAt(),
				receipt.getNotes(), itemResponses);
	}

	// ─────────────────────────────────────────────────────────────
	// Stock helpers
	// ─────────────────────────────────────────────────────────────

	// public void addPendingStock(Long productId, BigDecimal quantity) {
	// StockLevel level = getFirstStockLevel(productId);
	// level.setPendingStock(level.getPendingStock().add(quantity));
	// stockLevelRepository.save(level);
	// }

	public void addPendingStockByWarehouse(Long productId, Long warehouseId, BigDecimal quantity) {
		StockLevel level = getFirstStockLevel(productId);
		level.setPendingStock(level.getPendingStock().add(quantity));
		stockLevelRepository.save(level);
	}

	public void addCurrentStock(Long productId, BigDecimal quantity) {
		StockLevel level = getFirstStockLevel(productId);
		level.setCurrentStock(level.getCurrentStock().add(quantity));
		stockLevelRepository.save(level);
	}

	public void substractCurrentStock(Long productId, BigDecimal quantity) {
		StockLevel level = getFirstStockLevel(productId);
		level.setCurrentStock(level.getCurrentStock().subtract(quantity));
		stockLevelRepository.save(level);
	}

	public void substractPendingStock(Long productId, BigDecimal quantity) {
		StockLevel level = getFirstStockLevel(productId);
		level.setPendingStock(level.getPendingStock().subtract(quantity));
		stockLevelRepository.save(level);
	}

	private StockLevel getFirstStockLevel(Long productId) {
		List<StockLevel> levels = stockLevelRepository.findByProductId(productId);
		if (levels.isEmpty()) {
			throw new EntityNotFoundException("No stock level found for product ID: " + productId);
		}
		return levels.get(0);
	}

	// ─────────────────────────────────────────────────────────────
	// Receipts queries
	// ─────────────────────────────────────────────────────────────

	@Transactional(readOnly = true)
	public List<GoodReceiptResponse> getReceiptsByOrderId(Long orderId) {
		return goodReceiptRepository.findByPurchaseOrderId(orderId).stream().map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public Page<GoodReceiptResponse> getAllOrders(Pageable pageable) {
		return goodReceiptRepository.findAll(pageable).map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public GoodReceiptResponse getReceiptById(Long id) {
		GoodReceipt receipt = goodReceiptRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + id));
		return toResponse(receipt);
	}

	// ─────────────────────────────────────────────────────────────
	// Stock breakdown & summary
	// ─────────────────────────────────────────────────────────────

	@Transactional(readOnly = true)
	public ProductStockBreakdown getStockBreakdownByProduct(Long productId) {
		BigDecimal totalStock = stockLevelRepository.getTotalStockByProductId(productId);
		if (totalStock == null)
			totalStock = BigDecimal.ZERO;

		List<StockLevelRepository.WarehouseStockProjection> projections =
				stockLevelRepository.getStockByProductGroupedByWarehouse(productId);

		List<ProductStockBreakdown.WarehouseStockEntry> entries = projections.stream()
				.map(p -> ProductStockBreakdown.WarehouseStockEntry.builder()
						.warehouseId(p.getWarehouseId()).warehouseName(p.getWarehouseName())
						.currentStock(
								p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO)
						.pendingStock(
								p.getPendingStock() != null ? p.getPendingStock() : BigDecimal.ZERO)
						.build())
				.toList();

		BigDecimal totalPending =
				entries.stream().map(ProductStockBreakdown.WarehouseStockEntry::getPendingStock)
						.reduce(BigDecimal.ZERO, BigDecimal::add);

		return ProductStockBreakdown.builder().productId(productId).totalStock(totalStock)
				.totalPendingStock(totalPending).warehouses(entries).build();
	}

	@Transactional(readOnly = true)
	public List<WarehouseStockSummary> getGlobalStockSummary() {
		return stockLevelRepository.getGlobalStockByWarehouse().stream()
				.map(p -> WarehouseStockSummary.builder().warehouseId(p.getWarehouseId())
						.warehouseName(p.getWarehouseName())
						.totalStock(
								p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO)
						.totalPendingStock(
								p.getPendingStock() != null ? p.getPendingStock() : BigDecimal.ZERO)
						.build())
				.toList();
	}

	// ─────────────────────────────────────────────────────────────
	// Private mapper
	// ─────────────────────────────────────────────────────────────

	private GoodReceiptResponse toResponse(GoodReceipt receipt) {
		List<GoodReceiptItemResponse> itemResponses = receipt.getItems().stream()
				.map(item -> new GoodReceiptItemResponse(item.getProduct().getId(),
						item.getProduct().getName(), item.getProduct().getSku(),
						item.getExpectedQuantity(), item.getReceivedQuantity(),
						item.getReceivedQuantity().subtract(item.getExpectedQuantity())))
				.toList();

		return new GoodReceiptResponse(receipt.getId(), receipt.getReceiptNumber(),
				receipt.getPurchaseOrder().getId(), receipt.getPurchaseOrder().getOrderNumber(),
				receipt.getPurchaseOrder().getStatus(), receipt.getReceivedAt(), receipt.getNotes(),
				itemResponses);
	}
}
