package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.visco.backend.models.dtos.GoodReceiptResponse;
import com.visco.backend.models.dtos.ProductStockBreakdown;
import com.visco.backend.models.dtos.ReceiveGoodsRequest;
import com.visco.backend.models.dtos.WarehouseResponse;
import com.visco.backend.models.dtos.WarehouseStockSummary;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.WarehouseRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private GoodReceiptRepository goodReceiptRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private PurchaseOrder order;
    private Product product;
    private PurchaseOrderItem orderItem;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .uom(Uom.UNIDAD)
                .active(true)
                .build();

        orderItem = PurchaseOrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(100)
                .unitPrice(BigDecimal.TEN)
                .build();

        order = PurchaseOrder.builder()
                .id(1L)
                .orderNumber("PO-001")
                .status(PurchaseOrderStatus.PENDING)
                .items(new ArrayList<>(List.of(orderItem)))
                .build();
        orderItem.setPurchaseOrder(order);
    }

    @Test
    void receiveGoods_shouldDeliverFully() {
        ReceiveGoodsRequest request = new ReceiveGoodsRequest(
                List.of(new ReceiveGoodsRequest.ReceiveItem(1L, BigDecimal.valueOf(100))),
                "All good");

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(goodReceiptRepository.findByPurchaseOrderId(1L)).thenReturn(List.of());
        when(stockLevelRepository.findByProductId(1L)).thenReturn(
                List.of(createStockLevel(BigDecimal.ZERO, BigDecimal.valueOf(100))));
        when(goodReceiptRepository.save(any(GoodReceipt.class))).thenAnswer(i -> i.getArgument(0));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        GoodReceiptResponse response = warehouseService.receiveGoods(1L, request);

        assertNotNull(response);
        assertEquals(PurchaseOrderStatus.DELIVERED, response.updatedStatus());
        assertEquals("PO-001", response.orderNumber());
    }

    @Test
    void receiveGoods_shouldPartiallyDeliver() {
        ReceiveGoodsRequest request = new ReceiveGoodsRequest(
                List.of(new ReceiveGoodsRequest.ReceiveItem(1L, BigDecimal.valueOf(50))),
                "Partial delivery");

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(goodReceiptRepository.findByPurchaseOrderId(1L)).thenReturn(List.of());
        when(stockLevelRepository.findByProductId(1L)).thenReturn(
                List.of(createStockLevel(BigDecimal.ZERO, BigDecimal.valueOf(100))));
        when(goodReceiptRepository.save(any(GoodReceipt.class))).thenAnswer(i -> i.getArgument(0));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        GoodReceiptResponse response = warehouseService.receiveGoods(1L, request);

        assertNotNull(response);
        assertEquals(PurchaseOrderStatus.PARTIALLY_DELIVERED, response.updatedStatus());
    }

    @Test
    void receiveGoods_shouldThrow_whenOrderInInvalidStatus() {
        order.setStatus(PurchaseOrderStatus.DELIVERED);
        ReceiveGoodsRequest request = new ReceiveGoodsRequest(
                List.of(new ReceiveGoodsRequest.ReceiveItem(1L, BigDecimal.valueOf(100))),
                "");

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> warehouseService.receiveGoods(1L, request));
    }

    @Test
    void receiveGoods_shouldThrow_whenProductNotInOrder() {
        ReceiveGoodsRequest request = new ReceiveGoodsRequest(
                List.of(new ReceiveGoodsRequest.ReceiveItem(99L, BigDecimal.valueOf(100))),
                "");

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(EntityNotFoundException.class, () -> warehouseService.receiveGoods(1L, request));
    }

    @Test
    void determineIfFullyReceived_shouldReturnTrue_whenAllItemsFullyReceived() {
        ReceiveGoodsRequest request = new ReceiveGoodsRequest(
                List.of(new ReceiveGoodsRequest.ReceiveItem(1L, BigDecimal.valueOf(100))),
                "");

        boolean result = warehouseService.determineIfFullyReceived(order, new HashMap<>(), request);

        assertTrue(result);
    }

    @Test
    void determineIfFullyReceived_shouldReturnFalse_whenNotAllItemsReceived() {
        ReceiveGoodsRequest request = new ReceiveGoodsRequest(
                List.of(new ReceiveGoodsRequest.ReceiveItem(1L, BigDecimal.valueOf(50))),
                "");

        boolean result = warehouseService.determineIfFullyReceived(order, new HashMap<>(), request);

        assertFalse(result);
    }

    @Test
    void determineIfFullyReceived_shouldAccountForPreviousReceipts() {
        HashMap<Long, BigDecimal> previousReceived = new HashMap<>();
        previousReceived.put(1L, BigDecimal.valueOf(40));
        ReceiveGoodsRequest request = new ReceiveGoodsRequest(
                List.of(new ReceiveGoodsRequest.ReceiveItem(1L, BigDecimal.valueOf(60))),
                "");

        boolean result = warehouseService.determineIfFullyReceived(order, previousReceived, request);

        assertTrue(result);
    }

    @Test
    void addPendingStock_shouldAddQuantity() {
        StockLevel level = createStockLevel(BigDecimal.valueOf(50), BigDecimal.valueOf(30));
        when(stockLevelRepository.findByProductId(1L)).thenReturn(List.of(level));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(level);

        warehouseService.addPendingStock(1L, BigDecimal.valueOf(20));

        assertEquals(BigDecimal.valueOf(50), level.getPendingStock());
        verify(stockLevelRepository).save(level);
    }

    @Test
    void addCurrentStock_shouldAddQuantity() {
        StockLevel level = createStockLevel(BigDecimal.valueOf(50), BigDecimal.ZERO);
        when(stockLevelRepository.findByProductId(1L)).thenReturn(List.of(level));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(level);

        warehouseService.addCurrentStock(1L, BigDecimal.valueOf(20));

        assertEquals(BigDecimal.valueOf(70), level.getCurrentStock());
        verify(stockLevelRepository).save(level);
    }

    @Test
    void substractCurrentStock_shouldSubtractQuantity() {
        StockLevel level = createStockLevel(BigDecimal.valueOf(100), BigDecimal.ZERO);
        when(stockLevelRepository.findByProductId(1L)).thenReturn(List.of(level));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(level);

        warehouseService.substractCurrentStock(1L, BigDecimal.valueOf(30));

        assertEquals(BigDecimal.valueOf(70), level.getCurrentStock());
    }

    @Test
    void substractPendingStock_shouldSubtractQuantity() {
        StockLevel level = createStockLevel(BigDecimal.ZERO, BigDecimal.valueOf(100));
        when(stockLevelRepository.findByProductId(1L)).thenReturn(List.of(level));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(level);

        warehouseService.substractPendingStock(1L, BigDecimal.valueOf(40));

        assertEquals(BigDecimal.valueOf(60), level.getPendingStock());
    }

    @Test
    void addPendingStock_shouldThrow_whenNoStockLevel() {
        when(stockLevelRepository.findByProductId(1L)).thenReturn(List.of());

        assertThrows(EntityNotFoundException.class, () -> warehouseService.addPendingStock(1L, BigDecimal.TEN));
    }

    @Test
    void getReceiptsByOrderId_shouldReturnList() {
        when(goodReceiptRepository.findByPurchaseOrderId(1L)).thenReturn(List.of());

        List<GoodReceiptResponse> result = warehouseService.getReceiptsByOrderId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllOrders_shouldReturnPage() {
        when(goodReceiptRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        Page<GoodReceiptResponse> result = warehouseService.getAllOrders(PageRequest.of(0, 10));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getReceiptById_shouldThrow_whenNotFound() {
        when(goodReceiptRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> warehouseService.getReceiptById(99L));
    }

    @Test
    void getAllWarehouses_shouldReturnActiveOnly() {
        Warehouse w1 = new Warehouse();
        w1.setId(1L);
        w1.setName("Main Warehouse");
        w1.setActive(true);

        Warehouse w2 = new Warehouse();
        w2.setId(2L);
        w2.setName("Inactive Warehouse");
        w2.setActive(false);

        when(warehouseRepository.findAll()).thenReturn(List.of(w1, w2));

        List<WarehouseResponse> result = warehouseService.getAllWarehouses();

        assertEquals(1, result.size());
        assertEquals("Main Warehouse", result.get(0).getName());
    }

    @Test
    void getStockBreakdownByProduct_shouldReturnBreakdown() {
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.valueOf(150));
        when(stockLevelRepository.getStockByProductGroupedByWarehouse(1L))
                .thenReturn(List.of(createWarehouseProjection(1L, "Warehouse A", BigDecimal.valueOf(100), BigDecimal.valueOf(50))));

        ProductStockBreakdown result = warehouseService.getStockBreakdownByProduct(1L);

        assertEquals(1L, result.getProductId());
        assertEquals(BigDecimal.valueOf(150), result.getTotalStock());
        assertEquals(1, result.getWarehouses().size());
    }

    @Test
    void getGlobalStockSummary_shouldReturnSummary() {
        when(stockLevelRepository.getGlobalStockByWarehouse())
                .thenReturn(List.of(createWarehouseProjection(1L, "Warehouse A", BigDecimal.valueOf(200), BigDecimal.valueOf(100))));

        List<WarehouseStockSummary> result = warehouseService.getGlobalStockSummary();

        assertEquals(1, result.size());
        assertEquals("Warehouse A", result.get(0).getWarehouseName());
    }

    private StockLevel createStockLevel(BigDecimal current, BigDecimal pending) {
        StockLevel level = new StockLevel();
        level.setId(1L);
        level.setProduct(product);
        level.setCurrentStock(current);
        level.setPendingStock(pending);
        return level;
    }

    private StockLevelRepository.WarehouseStockProjection createWarehouseProjection(
            Long id, String name, BigDecimal current, BigDecimal pending) {
        return new StockLevelRepository.WarehouseStockProjection() {
            @Override
            public Long getWarehouseId() {
                return id;
            }

            @Override
            public String getWarehouseName() {
                return name;
            }

            @Override
            public BigDecimal getCurrentStock() {
                return current;
            }

            @Override
            public BigDecimal getPendingStock() {
                return pending;
            }
        };
    }
}
