package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.models.dtos.MonthlySpendingDTO;
import com.visco.backend.models.dtos.RecentOrderDTO;
import com.visco.backend.models.dtos.SpendingStatsDTO;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.SupplierRepository;
import com.visco.backend.repositories.UserRepository;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Uom;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private PurchaseOrderRepository orderRepository;

    @Mock
    private GoodReceiptRepository goodReceiptRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @InjectMocks
    private StatsService statsService;

    private Product product;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Test Supplier");

        product = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .uom(Uom.UNIDAD)
                .reorderPoint(BigDecimal.valueOf(10))
                .active(true)
                .build();
    }

    @Test
    void getKpis_shouldReturnStats() {
        when(orderRepository.count()).thenReturn(100L);
        when(orderRepository.countDeliveredOrders()).thenReturn(85L);
        when(productRepository.getTotalInventoryUnits()).thenReturn(BigDecimal.valueOf(5000));

        Object month = java.sql.Timestamp.valueOf(LocalDateTime.now().withDayOfMonth(1));
        when(orderRepository.getMonthlySpending(any(LocalDateTime.class)))
                .thenReturn(List.of(createMonthlyProjection(month, BigDecimal.valueOf(25000))));

        KpiStatsDTO kpis = statsService.getKpis();

        assertEquals(100L, kpis.getTotalOrders());
        assertEquals(BigDecimal.valueOf(5000), kpis.getTotalInventoryUnits());
        assertEquals(BigDecimal.valueOf(25000), kpis.getMonthlySpend());
        assertEquals(85.0, kpis.getFulfillmentRate(), 0.1);
    }

    @Test
    void getKpis_shouldReturnZeroFulfillment_whenNoOrders() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.countDeliveredOrders()).thenReturn(0L);
        when(productRepository.getTotalInventoryUnits()).thenReturn(BigDecimal.ZERO);
        when(orderRepository.getMonthlySpending(any(LocalDateTime.class))).thenReturn(List.of());

        KpiStatsDTO kpis = statsService.getKpis();

        assertEquals(0, kpis.getTotalOrders());
        assertEquals(0.0, kpis.getFulfillmentRate(), 0.1);
    }

    @Test
    void getRecentOrders_shouldReturnList() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(1L)
                .orderNumber("PO-001")
                .createdAt(LocalDateTime.now())
                .supplier(supplier)
                .status(PurchaseOrderStatus.PENDING)
                .build();

        when(orderRepository.findRecentOrders(PageRequest.of(0, 5))).thenReturn(List.of(order));

        List<RecentOrderDTO> result = statsService.getRecentOrders(5);

        assertEquals(1, result.size());
        assertEquals("PO-001", result.get(0).getOrderNumber());
        assertEquals("Test Supplier", result.get(0).getSupplierName());
        assertEquals(PurchaseOrderStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    void getSpendingStats_shouldReturnStats() {
        Object month1 = java.sql.Timestamp.valueOf(LocalDateTime.now().minusMonths(1).withDayOfMonth(1));
        Object month2 = java.sql.Timestamp.valueOf(LocalDateTime.now().withDayOfMonth(1));

        when(orderRepository.getMonthlySpending(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        createMonthlyProjection(month1, BigDecimal.valueOf(10000)),
                        createMonthlyProjection(month2, BigDecimal.valueOf(15000))));

        when(orderRepository.getSpendingByCategory(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        createCategoryProjection("Category A", BigDecimal.valueOf(15000)),
                        createCategoryProjection("Category B", BigDecimal.valueOf(10000))));

        SpendingStatsDTO stats = statsService.getSpendingStats();

        assertEquals(BigDecimal.valueOf(15000), stats.getTotalMonthly());
        assertEquals(2, stats.getMonthlyBreakdown().size());
        assertEquals(2, stats.getByCategory().size());
        assertTrue(stats.getByCategoryPercent().containsKey("Category A"));
    }

    @Test
    void getSpendingStats_shouldHandleEmptyData() {
        when(orderRepository.getMonthlySpending(any(LocalDateTime.class))).thenReturn(List.of());
        when(orderRepository.getSpendingByCategory(any(LocalDateTime.class))).thenReturn(List.of());

        SpendingStatsDTO stats = statsService.getSpendingStats();

        assertEquals(BigDecimal.ZERO, stats.getTotalMonthly());
        assertTrue(stats.getMonthlyBreakdown().isEmpty());
        assertTrue(stats.getByCategory().isEmpty());
    }

    @Test
    void getCriticalInventory_shouldReturnItems() {
        when(productRepository.findProductsBelowReorderPoint()).thenReturn(List.of(product));
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.valueOf(5));

        List<CriticalInventoryItemDTO> result = statsService.getCriticalInventory();

        assertEquals(1, result.size());
        assertEquals("Test Product", result.get(0).getProductName());
        assertEquals(BigDecimal.valueOf(5), result.get(0).getCurrentStock());
        assertEquals(BigDecimal.valueOf(10), result.get(0).getReorderPoint());
        assertEquals("WARNING", result.get(0).getSeverity());
    }

    @Test
    void getCriticalInventory_shouldMarkAsCritical_whenStockZero() {
        when(productRepository.findProductsBelowReorderPoint()).thenReturn(List.of(product));
        when(stockLevelRepository.getTotalStockByProductId(1L)).thenReturn(BigDecimal.ZERO);

        List<CriticalInventoryItemDTO> result = statsService.getCriticalInventory();

        assertEquals("CRITICAL", result.get(0).getSeverity());
    }

    private PurchaseOrderRepository.MonthlySpendingProjection createMonthlyProjection(Object month, BigDecimal total) {
        return new PurchaseOrderRepository.MonthlySpendingProjection() {
            @Override
            public Object getMonth() {
                return month;
            }

            @Override
            public BigDecimal getTotal() {
                return total;
            }
        };
    }

    private PurchaseOrderRepository.CategorySpendingProjection createCategoryProjection(String name, BigDecimal total) {
        return new PurchaseOrderRepository.CategorySpendingProjection() {
            @Override
            public String getCategoryName() {
                return name;
            }

            @Override
            public BigDecimal getTotal() {
                return total;
            }
        };
    }
}
