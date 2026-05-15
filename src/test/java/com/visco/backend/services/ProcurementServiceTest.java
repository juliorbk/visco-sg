package com.visco.backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.visco.backend.models.dtos.CreatePurchaseOrderRequest;
import com.visco.backend.models.dtos.PurchaseOrderItemRequest;
import com.visco.backend.models.dtos.PurchaseOrderResponse;
import com.visco.backend.models.entities.PaymentMethod;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.PurchaseOrderStatus;
import com.visco.backend.models.entities.PurchaseOrderType;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.models.entities.User;
import com.visco.backend.models.entities.UserRole;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.SupplierRepository;
import com.visco.backend.repositories.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProcurementServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WarehouseService warehouseService;

    @InjectMocks
    private ProcurementService procurementService;

    private Supplier supplier;
    private User user;
    private Product product;
    private CreatePurchaseOrderRequest createRequest;
    private PurchaseOrder order;

    @BeforeEach
    void setUp() {
        supplier = Supplier.builder()
                .id(1L)
                .name("Test Supplier")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("user@test.com")
                .role(UserRole.MANAGER)
                .active(true)
                .build();

        product = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Test Product")
                .uom(Uom.UNIDAD)
                .active(true)
                .build();

        createRequest = new CreatePurchaseOrderRequest(
                "PO-001",
                "Test order",
                1L,
                PaymentMethod.BANK_TRANSFER,
                PurchaseOrderType.MATERIALS,
                user.getId(),
                List.of(new PurchaseOrderItemRequest(1L, 50, BigDecimal.valueOf(100))));

        order = PurchaseOrder.builder()
                .id(1L)
                .orderNumber("PO-001")
                .description("Test order")
                .status(PurchaseOrderStatus.PENDING)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .type(PurchaseOrderType.MATERIALS)
                .supplier(supplier)
                .createdBy(user)
                .items(new java.util.ArrayList<>())
                .build();
    }

    @Test
    void createPurchaseOrder_shouldSucceed() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(i -> {
            PurchaseOrder saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PurchaseOrderResponse response = procurementService.createPurchaseOrder(createRequest);

        assertNotNull(response);
        assertEquals("PO-001", response.orderNumber());
        assertEquals(PurchaseOrderStatus.PENDING, response.status());
        assertEquals("Test Supplier", response.supplierName());
        verify(warehouseService).addPendingStock(1L, BigDecimal.valueOf(50));
    }

    @Test
    void createPurchaseOrder_shouldThrow_whenSupplierNotFound() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> procurementService.createPurchaseOrder(createRequest));
    }

    @Test
    void createPurchaseOrder_shouldThrow_whenUserNotFound() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> procurementService.createPurchaseOrder(createRequest));
    }

    @Test
    void createPurchaseOrder_shouldThrow_whenProductNotFound() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of());

        assertThrows(EntityNotFoundException.class, () -> procurementService.createPurchaseOrder(createRequest));
    }

    @Test
    void cancelOrderById_shouldSucceed() {
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(50)
                .unitPrice(BigDecimal.valueOf(100))
                .build();
        order.getItems().add(item);

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        procurementService.cancelOrderById(1L);

        assertEquals(PurchaseOrderStatus.CANCELLED, order.getStatus());
        verify(warehouseService).substractPendingStock(1L, BigDecimal.valueOf(50));
    }

    @Test
    void cancelOrderById_shouldThrow_whenStatusNotCancellable() {
        order.setStatus(PurchaseOrderStatus.DELIVERED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> procurementService.cancelOrderById(1L));
    }

    @Test
    void cancelOrderById_shouldThrow_whenOrderNotFound() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> procurementService.cancelOrderById(99L));
    }

    @Test
    void approveOrder_shouldSucceed() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(order);

        procurementService.approveOrder(1L);

        assertEquals(PurchaseOrderStatus.IN_TRANSIT, order.getStatus());
    }

    @Test
    void approveOrder_shouldThrow_whenNotPending() {
        order.setStatus(PurchaseOrderStatus.DELIVERED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> procurementService.approveOrder(1L));
    }

    @Test
    void approveOrder_shouldThrow_whenOrderNotFound() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> procurementService.approveOrder(99L));
    }

    @Test
    void getAllOrders_shouldReturnList() {
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(order));

        List<PurchaseOrderResponse> result = procurementService.getAllOrders();

        assertEquals(1, result.size());
    }

    @Test
    void getOrderById_shouldReturnResponse() {
        order.getItems().add(PurchaseOrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(50)
                .unitPrice(BigDecimal.valueOf(100))
                .purchaseOrder(order)
                .build());

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        PurchaseOrderResponse result = procurementService.getOrderById(1L);

        assertEquals("PO-001", result.orderNumber());
        assertEquals("Test Supplier", result.supplierName());
    }

    @Test
    void getOrderById_shouldThrow_whenNotFound() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> procurementService.getOrderById(99L));
    }
}
