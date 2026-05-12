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

import com.visco.backend.models.dtos.GoodReceiptItemResponse;
import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.StockLevelRepository;

import jakarta.persistence.EntityNotFoundException;

//DIGITAL KARDEX
@Service
public class WarehouseService {

	private final PurchaseOrderRepository purchaseOrderRepository;
	private final GoodReceiptRepository goodReceiptRepository;
	private final StockLevelRepository stockLevelRepository;

	public WarehouseService(PurchaseOrderRepository purchaseOrderRepository,
			GoodReceiptRepository goodReceiptRepository, StockLevelRepository stockLevelRepository) {
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.goodReceiptRepository = goodReceiptRepository;
		this.stockLevelRepository = stockLevelRepository;
	}

	@Transactional
	public GoodReceiptResponse receiveGoods(Long orderId, ReceiveGoodsRequest request) {
		// 1. Buscar la orden de compra
		PurchaseOrder order = purchaseOrderRepository.findById(orderId)
				.orElseThrow(() -> new EntityNotFoundException("Purchase order not found: " + orderId));

		// 2. Validar que la orden permita recepción
		if (order.getStatus() == PurchaseOrderStatus.DELIVERED
				|| order.getStatus() == PurchaseOrderStatus.CANCELLED
				|| order.getStatus() == PurchaseOrderStatus.REJECTED) {
			throw new IllegalStateException(
					"Cannot receive goods for an order with status: " + order.getStatus());
		}

		// 3. Crear la nota de recepción
		GoodReceipt receipt = GoodReceipt.builder()
				.receiptNumber("VIS-" + orderId + "-" + System.currentTimeMillis())
				.purchaseOrder(order)
				.receivedAt(LocalDateTime.now())
				.notes(request.notes())
				.build();

		// 4. Acumular recepciones previas para saber cuánto se ha recibido antes
		Map<Long, BigDecimal> previousReceived = new HashMap<>();
		for (GoodReceipt prev : goodReceiptRepository.findByPurchaseOrderId(orderId)) {
			for (GoodReceiptItem prevItem : prev.getItems()) {
				previousReceived.merge(prevItem.getProduct().getId(), prevItem.getReceivedQuantity(),
						BigDecimal::add);
			}
		}

		// 5. Procesar cada item de la recepción actual
		for (ReceiveGoodsRequest.ReceiveItem itemReq : request.items()) {
			// Buscar el item correspondiente en la orden de compra
			PurchaseOrderItem poItem = order.getItems().stream()
					.filter(i -> i.getProduct().getId().equals(itemReq.productId()))
					.findFirst()
					.orElseThrow(() -> new EntityNotFoundException(
							"Product not found in order: " + itemReq.productId()));

			BigDecimal expected = BigDecimal.valueOf(poItem.getQuantity());
			BigDecimal received = itemReq.receivedQuantity();

			// Guardar el detalle en la nota de recepción
			GoodReceiptItem item = GoodReceiptItem.builder()
					.goodReceipt(receipt)
					.product(poItem.getProduct())
					.expectedQuantity(expected)
					.receivedQuantity(received)
					.build();

			receipt.getItems().add(item);

			// 6. Ajustar stock: lo que estaba pendiente ahora está en inventario físico
			addCurrentStock(poItem.getProduct().getId(), received);
			substractPendingStock(poItem.getProduct().getId(), received);
		}

		// 7. Persistir la nota de recepción
		goodReceiptRepository.save(receipt);

		// 8. Determinar si la orden está completa o parcialmente recibida
		boolean allFullyReceived = determineIfFullyReceived(order, previousReceived, request);
		order.setStatus(allFullyReceived ? PurchaseOrderStatus.DELIVERED : PurchaseOrderStatus.PARTIALLY_DELIVERED);
		purchaseOrderRepository.save(order);

		// 9. Retornar respuesta con el detalle de la recepción
		return buildReceiptResponse(receipt, order);
	}

	// Verifica si TODOS los items de la orden han sido recibidos completamente
	// Suma recepciones previas + recepción actual y compara contra la cantidad
	// ordenada
	public boolean determineIfFullyReceived(PurchaseOrder order,
			Map<Long, BigDecimal> previousReceived, ReceiveGoodsRequest request) {
		for (PurchaseOrderItem poItem : order.getItems()) {
			BigDecimal totalReceived = previousReceived
					.getOrDefault(poItem.getProduct().getId(), BigDecimal.ZERO);
			ReceiveGoodsRequest.ReceiveItem current = request.items().stream()
					.filter(r -> r.productId().equals(poItem.getProduct().getId()))
					.findFirst().orElse(null);
			if (current != null) {
				totalReceived = totalReceived.add(current.receivedQuantity());
			}
			// Si algún item aún no se ha recibido completo → la orden está parcial
			if (totalReceived.compareTo(BigDecimal.valueOf(poItem.getQuantity())) < 0) {
				return false;
			}
		}
		return true;
	}

	// Construye la respuesta con los datos de la recepción y el estado actualizado
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

	// ─────────────────────────────────────────────────────────────
	// Helpers de stock
	// ─────────────────────────────────────────────────────────────

	// Incrementa el stock pendiente (se espera recibir esta mercancía)
	public void addPendingStock(Long productId, BigDecimal quantity) {
		List<StockLevel> stockLevels = stockLevelRepository.findByProductId(productId);

		if (stockLevels.isEmpty()) {
			throw new EntityNotFoundException(
					"No stock level found for product ID: " + productId);
		}

		StockLevel level = stockLevels.get(0); // Asume un stock level por producto
		level.setPendingStock(level.getPendingStock().add(quantity));
		stockLevelRepository.save(level);

	}

	// Incrementa el stock físico (la mercancía ya está en el warehouse)
	public void addCurrentStock(Long productId, BigDecimal quantity) {
		List<StockLevel> stockLevels = stockLevelRepository.findByProductId(productId);

		if (stockLevels.isEmpty()) {
			throw new EntityNotFoundException(
					"No stock level found for product ID: " + productId);
		}

		StockLevel level = stockLevels.get(0);
		level.setCurrentStock(level.getCurrentStock().add(quantity));
		stockLevelRepository.save(level);

	}

	// Reduce el stock físico (ej: venta, ajuste, merma)
	public void substractCurrentStock(Long productId, BigDecimal quantity) {
		List<StockLevel> stockLevels = stockLevelRepository.findByProductId(productId);

		if (stockLevels.isEmpty()) {
			throw new EntityNotFoundException(
					"No stock level found for product ID: " + productId);
		}

		StockLevel level = stockLevels.get(0);
		level.setCurrentStock(level.getCurrentStock().subtract(quantity));
		stockLevelRepository.save(level);

	}

	// Reduce el stock pendiente (ej: se recibió, se canceló la orden)
	public void substractPendingStock(Long productId, BigDecimal quantity) {
		List<StockLevel> stockLevels = stockLevelRepository.findByProductId(productId);

		if (stockLevels.isEmpty()) {
			throw new EntityNotFoundException(
					"No stock level found for product ID: " + productId);
		}

		StockLevel level = stockLevels.get(0);
		level.setPendingStock(level.getPendingStock().subtract(quantity));
		stockLevelRepository.save(level);

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