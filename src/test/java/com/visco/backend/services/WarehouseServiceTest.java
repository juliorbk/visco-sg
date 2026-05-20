package com.visco.backend.services;

import com.visco.backend.models.dtos.*;
import com.visco.backend.models.entities.*;
import com.visco.backend.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse testWarehouse;
    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testWarehouse = Warehouse.builder()
                .id(1L)
                .name("Test Warehouse")
                .physicalAddress("Test Address")
                .active(true)
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.USER)
                .active(true)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .sku("SKU-001")
                .uom(Uom.UN)
                .build();
    }

    @Test
    void createWarehouse_Success() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "Test Warehouse", "Test Address", "Description", "SAP001", testUser.getId()
        );

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(testWarehouse);

        WarehouseDTO result = warehouseService.createWarehouse(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Warehouse");
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void createWarehouse_FailsWhenUserNotFound() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "Test Warehouse", "Test Address", "Description", "SAP001", 999L
        );

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.createWarehouse(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAllWarehouses_ReturnsPage() {
        Page<Warehouse> page = new PageImpl<>(List.of(testWarehouse));
        when(warehouseRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<WarehouseDTO> result = warehouseService.getAllWarehouses(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getWarehouse_Success() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));

        WarehouseDTO result = warehouseService.getWarehouse(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Warehouse");
    }

    @Test
    void getWarehouse_FailsWhenNotFound() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getWarehouse(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void transferStock_Success() {
        Warehouse fromWarehouse = Warehouse.builder().id(1L).name("From WH").build();
        Warehouse toWarehouse = Warehouse.builder().id(2L).name("To WH").build();
        StockLevel sourceStock = StockLevel.builder()
                .product(testProduct)
                .warehouse(fromWarehouse)
                .currentStock(BigDecimal.valueOf(100))
                .pendingStock(BigDecimal.ZERO)
                .build();

        TransferStockRequest request = new TransferStockRequest(
                1L, 2L, 1L, BigDecimal.valueOf(10), "Transfer reason", BigDecimal.valueOf(50), testUser.getId()
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(fromWarehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(toWarehouse));
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(sourceStock));
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        warehouseService.transferStock(request);

        verify(stockLevelRepository, times(2)).save(any(StockLevel.class));
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void transferStock_FailsWhenInsufficientStock() {
        Warehouse fromWarehouse = Warehouse.builder().id(1L).name("From WH").build();
        Warehouse toWarehouse = Warehouse.builder().id(2L).name("To WH").build();
        StockLevel sourceStock = StockLevel.builder()
                .product(testProduct)
                .warehouse(fromWarehouse)
                .currentStock(BigDecimal.valueOf(5))
                .pendingStock(BigDecimal.ZERO)
                .build();

        TransferStockRequest request = new TransferStockRequest(
                1L, 2L, 1L, BigDecimal.valueOf(10), "Transfer", BigDecimal.valueOf(50), testUser.getId()
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(fromWarehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(toWarehouse));
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(sourceStock));

        assertThatThrownBy(() -> warehouseService.transferStock(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void adjustStock_Success() {
        Warehouse warehouse = Warehouse.builder().id(1L).name("WH").build();
        StockLevel stock = StockLevel.builder()
                .product(testProduct)
                .warehouse(warehouse)
                .currentStock(BigDecimal.valueOf(100))
                .build();

        AdjustStockRequest request = new AdjustStockRequest(
                1L, 1L, BigDecimal.valueOf(150), "Adjustment reason", BigDecimal.valueOf(50), testUser.getId()
        );

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.of(stock));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        warehouseService.adjustStock(request);

        assertThat(stock.getCurrentStock()).isEqualTo(BigDecimal.valueOf(150));
        verify(stockLevelRepository).save(stock);
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void getStockBreakdownByProduct_Success() {
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.valueOf(100));
        when(stockLevelRepository.getStockByProductGroupedByWarehouse(1L)).thenReturn(Collections.emptyList());

        ProductStockBreakdown result = warehouseService.getStockBreakdownByProduct(1L);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getTotalStock()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void getGlobalStockSummary_Success() {
        when(stockLevelRepository.getGlobalStockByWarehouse()).thenReturn(Collections.emptyList());

        List<WarehouseStockSummary> result = warehouseService.getGlobalStockSummary();

        assertThat(result).isNotNull();
    }

    @Test
    void addPendingStockByWarehouse_Success() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 1L)).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(null);

        warehouseService.addPendingStockByWarehouse(1L, 1L, BigDecimal.valueOf(50));

        verify(stockLevelRepository).save(any(StockLevel.class));
    }

    @Test
    void getReceiptsByOrderId_ReturnsList() {
        when(goodReceiptRepository.findByPurchaseOrderId(1L)).thenReturn(Collections.emptyList());

        List<GoodReceiptResponse> result = warehouseService.getReceiptsByOrderId(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void getAllOrders_ReturnsPage() {
        Page<GoodReceipt> page = new PageImpl<>(Collections.emptyList());
        when(goodReceiptRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<GoodReceiptResponse> result = warehouseService.getAllOrders(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
    }

    @Test
    void getReceiptById_Success() {
        GoodReceipt receipt = GoodReceipt.builder()
                .id(1L)
                .receiptNumber("VIS-1-123-abc")
                .receivedAt(LocalDateTime.now())
                .build();
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L)
                .orderNumber("PO-001")
                .status(PurchaseOrderStatus.PENDING)
                .build();
        receipt.setPurchaseOrder(order);

        when(goodReceiptRepository.findById(1L)).thenReturn(Optional.of(receipt));

        GoodReceiptResponse result = warehouseService.getReceiptById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getReceiptNumber()).isEqualTo("VIS-1-123-abc");
    }

    @Test
    void getReceiptById_FailsWhenNotFound() {
        when(goodReceiptRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getReceiptById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
