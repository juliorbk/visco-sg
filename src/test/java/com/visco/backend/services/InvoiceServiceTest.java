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
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private GoodReceiptRepository goodReceiptRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Supplier testSupplier;
    private Product testProduct;
    private PurchaseOrder testOrder;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder().id(1L).name("Test Supplier").build();
        testProduct = Product.builder().id(1L).name("Test Product").sku("SKU-001").uom(Uom.UN).build();
        testOrder = PurchaseOrder.builder()
                .id(1L).orderNumber("PO-001").status(PurchaseOrderStatus.PENDING)
                .supplier(testSupplier).build();
    }

    @Test
    void createInvoice_Success() {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                "INV-001", 1L, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500), "Notes",
                Collections.emptyList()
        );

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(productRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(goodReceiptRepository.findByPurchaseOrderId(1L)).thenReturn(Collections.emptyList());

        Invoice saved = Invoice.builder()
                .id(1L).invoiceNumber("INV-001").status(InvoiceStatus.MATCHED)
                .purchaseOrder(testOrder).supplier(testSupplier).build();

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(saved);

        InvoiceResponse result = invoiceService.createInvoice(request);

        assertThat(result).isNotNull();
        assertThat(result.invoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void createInvoice_FailsWhenPurchaseOrderNotFound() {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                "INV-001", 999L, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500), "Notes",
                Collections.emptyList()
        );

        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createInvoice_FailsWhenSupplierNotFound() {
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                "INV-001", 1L, 999L, LocalDate.now(), LocalDate.now().plusDays(30),
                BigDecimal.valueOf(5000), BigDecimal.valueOf(500), "Notes",
                Collections.emptyList()
        );

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getAllInvoices_ReturnsPage() {
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-001").status(InvoiceStatus.PENDING)
                .purchaseOrder(testOrder).supplier(testSupplier).build();

        Page<Invoice> page = new PageImpl<>(Collections.singletonList(invoice));
        when(invoiceRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<InvoiceResponse> result = invoiceService.getAllInvoices(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getInvoiceById_Success() {
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-001").status(InvoiceStatus.PENDING)
                .purchaseOrder(testOrder).supplier(testSupplier).build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        InvoiceResponse result = invoiceService.getInvoiceById(1L);

        assertThat(result).isNotNull();
        assertThat(result.invoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void getInvoiceById_FailsWhenNotFound() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getInvoicesByOrderId_ReturnsList() {
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-001").status(InvoiceStatus.PENDING)
                .purchaseOrder(testOrder).supplier(testSupplier).build();

        when(invoiceRepository.findByPurchaseOrderId(1L)).thenReturn(Collections.singletonList(invoice));

        var result = invoiceService.getInvoicesByOrderId(1L);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    @Test
    void markAsPaid_Success() {
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-001").status(InvoiceStatus.PENDING)
                .purchaseOrder(testOrder).supplier(testSupplier).build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse result = invoiceService.markAsPaid(1L, LocalDate.now());

        assertThat(result).isNotNull();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void markAsPaid_FailsWhenAlreadyPaid() {
        Invoice invoice = Invoice.builder()
                .id(1L).status(InvoiceStatus.PAID).build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.markAsPaid(1L, LocalDate.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void markAsPaid_FailsWhenCancelled() {
        Invoice invoice = Invoice.builder()
                .id(1L).status(InvoiceStatus.CANCELLED).build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.markAsPaid(1L, LocalDate.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void cancelInvoice_Success() {
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-001").status(InvoiceStatus.PENDING)
                .purchaseOrder(testOrder).supplier(testSupplier).build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse result = invoiceService.cancelInvoice(1L);

        assertThat(result).isNotNull();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void cancelInvoice_FailsWhenPaid() {
        Invoice invoice = Invoice.builder()
                .id(1L).status(InvoiceStatus.PAID).build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.cancelInvoice(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("paid");
    }
}
