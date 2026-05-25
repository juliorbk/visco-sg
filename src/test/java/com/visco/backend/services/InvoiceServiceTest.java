package com.visco.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visco.backend.models.dtos.CreateInvoiceRequest;
import com.visco.backend.models.dtos.InvoiceItemRequest;
import com.visco.backend.models.dtos.InvoiceResponse;
import com.visco.backend.models.entities.CostCenter;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.Invoice;
import com.visco.backend.models.entities.InvoiceStatus;
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
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.InvoiceRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;
    @Mock private GoodReceiptRepository goodReceiptRepository;

    @InjectMocks private InvoiceService invoiceService;

    @Captor private ArgumentCaptor<Invoice> invoiceCaptor;

    private static final Long INVOICE_ID = 1L;
    private static final Long ORDER_ID = 1L;
    private static final Long SUPPLIER_ID = 1L;
    private static final Long PRODUCT_ID = 1L;
    private static final BigDecimal QUANTITY = BigDecimal.TEN;
    private static final BigDecimal UNIT_PRICE = new BigDecimal("100.00");
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("1000.00");

    // ── Helpers ──────────────────────────────────────────────────────

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("user@test.com")
                .password("encoded")
                .role(UserRole.PROCUREMENT)
                .active(true)
                .build();
    }

    private Supplier buildSupplier() {
        return Supplier.builder()
                .id(SUPPLIER_ID)
                .name("Test Supplier")
                .email("supplier@test.com")
                .address("123 St")
                .currency(Currency.USD)
                .active(true)
                .build();
    }

    private Product buildProduct() {
        return Product.builder()
                .id(PRODUCT_ID)
                .internalCode("IC-001")
                .sku("SKU-001")
                .name("Test Product")
                .sapCode("SAP-001")
                .uom(Uom.EA)
                .reorderPoint(new BigDecimal("5"))
                .active(true)
                .build();
    }

    private Warehouse buildWarehouse() {
        return Warehouse.builder()
                .id(1L)
                .name("Main WH")
                .physicalAddress("Addr")
                .description("Desc")
                .active(true)
                .build();
    }

    private PurchaseOrder buildPurchaseOrder(PurchaseOrderStatus status) {
        Product product = buildProduct();
        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(QUANTITY.intValue())
                .unitPrice(UNIT_PRICE)
                .build();
        PurchaseOrder order = PurchaseOrder.builder()
                .id(ORDER_ID)
                .orderNumber("PO-001")
                .description("Test PO")
                .status(status)
                .supplier(buildSupplier())
                .createdBy(buildUser())
                .destinationWarehouse(buildWarehouse())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .type(PurchaseOrderType.MATERIALS)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        item.setPurchaseOrder(order);
        order.getItems().add(item);
        return order;
    }

    private CreateInvoiceRequest buildInvoiceRequest(BigDecimal invQty, BigDecimal invPrice) {
        return new CreateInvoiceRequest(
                "INV-001",
                ORDER_ID,
                SUPPLIER_ID,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                TOTAL_AMOUNT,
                new BigDecimal("100.00"),
                "Test invoice",
                List.of(new InvoiceItemRequest(PRODUCT_ID, invQty, invPrice, "Item note"))
        );
    }

    // ── createInvoice ───────────────────────────────────────────────

    @Test
    void shouldCreateInvoiceWithMatchedStatus_whenAllThreeWayMatch() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Supplier supplier = buildSupplier();
        Product product = buildProduct();
        GoodReceipt receipt = GoodReceipt.builder().id(1L).purchaseOrder(order).build();
        GoodReceiptItem receiptItem = GoodReceiptItem.builder()
                .id(1L).goodReceipt(receipt).product(product)
                .expectedQuantity(QUANTITY).receivedQuantity(QUANTITY)
                .build();
        receipt.setItems(List.of(receiptItem));

        when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of(receipt));
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice invEntity = inv.getArgument(0);
            invEntity.setId(INVOICE_ID);
            return invEntity;
        });

        InvoiceResponse response = invoiceService.createInvoice(buildInvoiceRequest(QUANTITY, UNIT_PRICE));

        assertThat(response.status()).isEqualTo(InvoiceStatus.MATCHED);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getStatus()).isEqualTo(InvoiceStatus.MATCHED);
    }

    @Test
    void shouldCreateInvoiceWithPartiallyMatchedStatus_whenSomeItemsMatch() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Supplier supplier = buildSupplier();
        Product product = buildProduct();
        GoodReceipt receipt = GoodReceipt.builder().id(1L).purchaseOrder(order).build();
        GoodReceiptItem receiptItem = GoodReceiptItem.builder()
                .id(1L).goodReceipt(receipt).product(product)
                .expectedQuantity(QUANTITY).receivedQuantity(QUANTITY)
                .build();
        receipt.setItems(List.of(receiptItem));

        when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of(receipt));
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice invEntity = inv.getArgument(0);
            invEntity.setId(INVOICE_ID);
            return invEntity;
        });

        var request = new CreateInvoiceRequest(
                "INV-001", ORDER_ID, SUPPLIER_ID,
                LocalDate.now(), LocalDate.now().plusDays(30),
                TOTAL_AMOUNT, new BigDecimal("100.00"), "Test",
                List.of(new InvoiceItemRequest(PRODUCT_ID, BigDecimal.valueOf(5), UNIT_PRICE, "Partial"))
        );

        InvoiceResponse response = invoiceService.createInvoice(request);

        assertThat(response.status()).isEqualTo(InvoiceStatus.UNMATCHED);
    }

    @Test
    void shouldCreateInvoiceWithPartiallyMatched_whenMultiItemAndSomeMatch() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Supplier supplier = buildSupplier();
        Product product = buildProduct();
        Product product2 = Product.builder()
                .id(2L).internalCode("IC-002").sku("SKU-002").name("Product 2")
                .sapCode("SAP-002").uom(Uom.EA).reorderPoint(new BigDecimal("3")).active(true).build();

        PurchaseOrderItem item2 = PurchaseOrderItem.builder()
                .id(2L).purchaseOrder(order).product(product2)
                .quantity(5).unitPrice(new BigDecimal("50.00"))
                .build();
        order.getItems().add(item2);

        GoodReceipt receipt = GoodReceipt.builder().id(1L).purchaseOrder(order).build();
        GoodReceiptItem receiptItem1 = GoodReceiptItem.builder()
                .id(1L).goodReceipt(receipt).product(product)
                .expectedQuantity(QUANTITY).receivedQuantity(QUANTITY)
                .build();
        GoodReceiptItem receiptItem2 = GoodReceiptItem.builder()
                .id(2L).goodReceipt(receipt).product(product2)
                .expectedQuantity(BigDecimal.valueOf(5)).receivedQuantity(BigDecimal.valueOf(5))
                .build();
        receipt.setItems(List.of(receiptItem1, receiptItem2));

        when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(productRepository.findAllById(List.of(PRODUCT_ID, 2L))).thenReturn(List.of(product, product2));
        when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of(receipt));
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice invEntity = inv.getArgument(0);
            invEntity.setId(INVOICE_ID);
            return invEntity;
        });

        var request = new CreateInvoiceRequest(
                "INV-002", ORDER_ID, SUPPLIER_ID,
                LocalDate.now(), LocalDate.now().plusDays(30),
                new BigDecimal("1250.00"), new BigDecimal("100.00"), "Test",
                List.of(
                        new InvoiceItemRequest(PRODUCT_ID, QUANTITY, UNIT_PRICE, "Match"),
                        new InvoiceItemRequest(2L, BigDecimal.valueOf(5), new BigDecimal("60.00"), "Price mismatch")
                )
        );

        InvoiceResponse response = invoiceService.createInvoice(request);

        assertThat(response.status()).isEqualTo(InvoiceStatus.PARTIALLY_MATCHED);
    }

    @Test
    void shouldCreateInvoiceWithUnmatchedStatus_whenQuantityMismatch() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Supplier supplier = buildSupplier();
        Product product = buildProduct();
        GoodReceipt receipt = GoodReceipt.builder().id(1L).purchaseOrder(order).build();
        GoodReceiptItem receiptItem = GoodReceiptItem.builder()
                .id(1L).goodReceipt(receipt).product(product)
                .expectedQuantity(QUANTITY).receivedQuantity(QUANTITY)
                .build();
        receipt.setItems(List.of(receiptItem));

        when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of(receipt));
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice invEntity = inv.getArgument(0);
            invEntity.setId(INVOICE_ID);
            return invEntity;
        });

        InvoiceResponse response = invoiceService.createInvoice(
                buildInvoiceRequest(BigDecimal.valueOf(8), UNIT_PRICE));

        assertThat(response.status()).isEqualTo(InvoiceStatus.UNMATCHED);
    }

    @Test
    void shouldCreateInvoiceWithUnmatchedStatus_whenPriceMismatch() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Supplier supplier = buildSupplier();
        Product product = buildProduct();
        GoodReceipt receipt = GoodReceipt.builder().id(1L).purchaseOrder(order).build();
        GoodReceiptItem receiptItem = GoodReceiptItem.builder()
                .id(1L).goodReceipt(receipt).product(product)
                .expectedQuantity(QUANTITY).receivedQuantity(QUANTITY)
                .build();
        receipt.setItems(List.of(receiptItem));

        when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
        when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of(receipt));
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice invEntity = inv.getArgument(0);
            invEntity.setId(INVOICE_ID);
            return invEntity;
        });

        InvoiceResponse response = invoiceService.createInvoice(
                buildInvoiceRequest(QUANTITY, new BigDecimal("110.00")));

        assertThat(response.status()).isEqualTo(InvoiceStatus.UNMATCHED);
    }

    @Test
    void shouldThrowEntityNotFoundException_whenPurchaseOrderNotFound() {
        var request = new CreateInvoiceRequest(
                "INV-001", 99L, SUPPLIER_ID,
                LocalDate.now(), LocalDate.now().plusDays(30),
                TOTAL_AMOUNT, new BigDecimal("100.00"), "Test",
                List.of(new InvoiceItemRequest(PRODUCT_ID, QUANTITY, UNIT_PRICE, "Item"))
        );
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Purchase order not found");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenSupplierNotFound() {
        when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(buildPurchaseOrder(PurchaseOrderStatus.DELIVERED)));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(buildInvoiceRequest(QUANTITY, UNIT_PRICE)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Supplier not found");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenProductNotFound() {
        when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(buildPurchaseOrder(PurchaseOrderStatus.DELIVERED)));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(buildSupplier()));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of());

        assertThatThrownBy(() -> invoiceService.createInvoice(buildInvoiceRequest(QUANTITY, UNIT_PRICE)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void shouldThrowIllegalArgumentException_whenProductNotInPurchaseOrder() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Supplier supplier = buildSupplier();
        Product product = buildProduct();
        Product otherProduct = Product.builder()
                .id(99L).internalCode("IC-099").sku("SKU-099").name("Other Product")
                .sapCode("SAP-099").uom(Uom.EA).reorderPoint(new BigDecimal("1")).active(true).build();

        when(purchaseOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(productRepository.findAllById(List.of(99L))).thenReturn(List.of(otherProduct));
        when(goodReceiptRepository.findByPurchaseOrderId(ORDER_ID)).thenReturn(List.of());

        var request = new CreateInvoiceRequest(
                "INV-001", ORDER_ID, SUPPLIER_ID,
                LocalDate.now(), LocalDate.now().plusDays(30),
                TOTAL_AMOUNT, new BigDecimal("100.00"), "Test",
                List.of(new InvoiceItemRequest(99L, QUANTITY, UNIT_PRICE, "Not in PO"))
        );

        assertThatThrownBy(() -> invoiceService.createInvoice(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not in purchase order");
    }

    // ── markAsPaid ─────────────────────────────────────────────────

    @Test
    void shouldMarkAsPaid_whenStatusIsNotPaidOrCancelled() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID).invoiceNumber("INV-001")
                .purchaseOrder(order)
                .supplier(buildSupplier())
                .invoiceDate(LocalDate.now())
                .totalAmount(TOTAL_AMOUNT)
                .status(InvoiceStatus.MATCHED)
                .items(new ArrayList<>())
                .build();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenReturn(invoice);

        LocalDate paymentDate = LocalDate.now();
        InvoiceResponse response = invoiceService.markAsPaid(INVOICE_ID, paymentDate);

        assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.paymentDate()).isEqualTo(paymentDate);
    }

    @Test
    void shouldThrowIllegalStateException_whenMarkingAlreadyPaidInvoice() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID).invoiceNumber("INV-001")
                .purchaseOrder(order)
                .supplier(buildSupplier())
                .invoiceDate(LocalDate.now())
                .totalAmount(TOTAL_AMOUNT)
                .status(InvoiceStatus.PAID)
                .items(new ArrayList<>())
                .build();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.markAsPaid(INVOICE_ID, LocalDate.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invoice is already paid");
    }

    @Test
    void shouldThrowIllegalStateException_whenMarkingCancelledInvoiceAsPaid() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID).invoiceNumber("INV-001")
                .purchaseOrder(order)
                .supplier(buildSupplier())
                .invoiceDate(LocalDate.now())
                .totalAmount(TOTAL_AMOUNT)
                .status(InvoiceStatus.CANCELLED)
                .items(new ArrayList<>())
                .build();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.markAsPaid(INVOICE_ID, LocalDate.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot mark a cancelled invoice as paid");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenInvoiceNotFoundForPaid() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.markAsPaid(99L, LocalDate.now()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invoice not found");
    }

    // ── cancelInvoice ───────────────────────────────────────────────

    @Test
    void shouldCancelInvoice_whenStatusIsNotPaid() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID).invoiceNumber("INV-001")
                .purchaseOrder(order)
                .supplier(buildSupplier())
                .invoiceDate(LocalDate.now())
                .totalAmount(TOTAL_AMOUNT)
                .status(InvoiceStatus.MATCHED)
                .items(new ArrayList<>())
                .build();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenReturn(invoice);

        InvoiceResponse response = invoiceService.cancelInvoice(INVOICE_ID);

        assertThat(response.status()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void shouldCancelInvoice_whenStatusIsPending() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID).invoiceNumber("INV-001")
                .purchaseOrder(order)
                .supplier(buildSupplier())
                .invoiceDate(LocalDate.now())
                .totalAmount(TOTAL_AMOUNT)
                .status(InvoiceStatus.PENDING)
                .items(new ArrayList<>())
                .build();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenReturn(invoice);

        InvoiceResponse response = invoiceService.cancelInvoice(INVOICE_ID);

        assertThat(response.status()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void shouldThrowIllegalStateException_whenCancellingPaidInvoice() {
        PurchaseOrder order = buildPurchaseOrder(PurchaseOrderStatus.DELIVERED);
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID).invoiceNumber("INV-001")
                .purchaseOrder(order)
                .supplier(buildSupplier())
                .invoiceDate(LocalDate.now())
                .totalAmount(TOTAL_AMOUNT)
                .status(InvoiceStatus.PAID)
                .items(new ArrayList<>())
                .build();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.cancelInvoice(INVOICE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a paid invoice");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenInvoiceNotFoundForCancel() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.cancelInvoice(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invoice not found");
    }

    // ── getInvoiceById ──────────────────────────────────────────────

    @Test
    void shouldGetInvoiceById_whenExists() {
        Invoice invoice = Invoice.builder()
                .id(INVOICE_ID).invoiceNumber("INV-001")
                .purchaseOrder(buildPurchaseOrder(PurchaseOrderStatus.DELIVERED))
                .supplier(buildSupplier())
                .invoiceDate(LocalDate.now())
                .totalAmount(TOTAL_AMOUNT)
                .status(InvoiceStatus.PENDING)
                .items(new ArrayList<>())
                .build();
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById(INVOICE_ID);

        assertThat(response).isNotNull();
        assertThat(response.invoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void shouldThrowEntityNotFoundException_whenInvoiceNotFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invoice not found");
    }
}
