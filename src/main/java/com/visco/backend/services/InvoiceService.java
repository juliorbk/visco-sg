package com.visco.backend.services;

import com.visco.backend.models.dtos.CreateInvoiceRequest;
import com.visco.backend.models.dtos.InvoiceItemRequest;
import com.visco.backend.models.dtos.InvoiceItemResponse;
import com.visco.backend.models.dtos.InvoiceResponse;
import com.visco.backend.models.entities.Invoice;
import com.visco.backend.models.entities.InvoiceItem;
import com.visco.backend.models.entities.InvoiceStatus;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.InvoiceRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.PurchaseOrderRepository;
import com.visco.backend.repositories.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles business logic for invoice processing and matching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final SupplierRepository supplierRepository;
  private final ProductRepository productRepository;
  private final GoodReceiptRepository goodReceiptRepository;

  /**
   * Creates an invoice from a purchase order with item matching validation.
   *
   * @param request the invoice creation request
   * @return the created invoice response
   */
  @Transactional
  @CacheEvict(value = "dashboard", key = "'spending'")
  public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
    PurchaseOrder order = purchaseOrderRepository
      .findByIdWithItems(request.purchaseOrderId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Purchase order not found: " + request.purchaseOrderId()
        )
      );

    Supplier supplier = supplierRepository
      .findById(request.supplierId())
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Supplier not found: " + request.supplierId()
        )
      );

    Map<Long, Product> productMap = productRepository
      .findAllById(
        request.items().stream().map(InvoiceItemRequest::productId).toList()
      )
      .stream()
      .collect(Collectors.toMap(Product::getId, p -> p));

    Map<Long, BigDecimal> receivedQtys = getTotalReceivedByProduct(
      order.getId()
    );

    Invoice invoice = Invoice.builder()
      .invoiceNumber(
        request.invoiceNumber() != null && !request.invoiceNumber().isBlank()
          ? request.invoiceNumber()
          : generateInvoiceNumber()
      )
      .purchaseOrder(order)
      .supplier(supplier)
      .invoiceDate(request.invoiceDate())
      .dueDate(request.dueDate())
      .totalAmount(request.totalAmount())
      .taxAmount(request.taxAmount())
      .notes(request.notes())
      .status(InvoiceStatus.PENDING)
      .build();

    int matchedItems = 0;

    for (InvoiceItemRequest itemReq : request.items()) {
      Product product = productMap.get(itemReq.productId());
      if (product == null) {
        throw new EntityNotFoundException(
          "Product not found: " + itemReq.productId()
        );
      }

      PurchaseOrderItem poItem = order
        .getItems()
        .stream()
        .filter(i -> i.getProduct().getId().equals(itemReq.productId()))
        .findFirst()
        .orElseThrow(() ->
          new IllegalArgumentException(
            "Product ID " +
              itemReq.productId() +
              " is not in purchase order " +
              order.getOrderNumber()
          )
        );

      BigDecimal poQty = poItem.getQuantity();
      BigDecimal receivedQty = receivedQtys.getOrDefault(
        itemReq.productId(),
        BigDecimal.ZERO
      );
      BigDecimal invQty = itemReq.quantity();

      boolean qtyMatch = invQty.compareTo(poQty) == 0;
      boolean receivedMatch = invQty.compareTo(receivedQty) == 0;
      boolean priceMatch =
        itemReq.unitPrice().compareTo(poItem.getUnitPrice()) == 0;

      if (qtyMatch && priceMatch && receivedMatch) {
        matchedItems++;
      }

      InvoiceItem item = InvoiceItem.builder()
        .invoice(invoice)
        .product(product)
        .quantity(invQty)
        .unitPrice(itemReq.unitPrice())
        .lineTotal(itemReq.unitPrice().multiply(invQty))
        .poQuantity(poQty)
        .receivedQuantity(receivedQty)
        .quantityMatch(qtyMatch)
        .priceMatch(priceMatch)
        .notes(itemReq.notes())
        .build();

      invoice.getItems().add(item);
    }

    if (matchedItems == request.items().size()) {
      invoice.setStatus(InvoiceStatus.MATCHED);
    } else if (matchedItems == 0) {
      invoice.setStatus(InvoiceStatus.UNMATCHED);
    } else {
      invoice.setStatus(InvoiceStatus.PARTIALLY_MATCHED);
    }

    Invoice saved = invoiceRepository.save(invoice);
    log.info(
      "Created invoice: {} for PO: {} with status: {}",
      saved.getInvoiceNumber(),
      order.getOrderNumber(),
      saved.getStatus()
    );

    return toResponse(saved);
  }

  /**
   * Retrieves a paginated list of all invoices.
   *
   * @param pageable pagination information
   * @return page of invoice responses
   */
  @Transactional(readOnly = true)
  public Page<InvoiceResponse> getAllInvoices(Pageable pageable) {
    return invoiceRepository.findAllWithFetch(pageable).map(this::toResponse);
  }

  /**
   * Retrieves an invoice by its ID.
   *
   * @param id the invoice ID
   * @return the invoice response
   */
  @Transactional(readOnly = true)
  public InvoiceResponse getInvoiceById(Long id) {
    return toResponse(findById(id));
  }

  /**
   * Retrieves all invoices associated with a purchase order.
   *
   * @param orderId the purchase order ID
   * @return list of invoice responses
   */
  @Transactional(readOnly = true)
  public List<InvoiceResponse> getInvoicesByOrderId(Long orderId) {
    return invoiceRepository
      .findByPurchaseOrderIdWithFetch(orderId)
      .stream()
      .map(this::toResponse)
      .toList();
  }

  /**
   * Marks an invoice as paid with the given payment date.
   *
   * @param id          the invoice ID
   * @param paymentDate the date of payment
   * @return the updated invoice response
   */
  @Transactional
  @CacheEvict(value = "dashboard", key = "'spending'")
  public InvoiceResponse markAsPaid(Long id, LocalDate paymentDate) {
    Invoice invoice = findById(id);
    if (invoice.getStatus() == InvoiceStatus.PAID) {
      throw new IllegalStateException("Invoice is already paid");
    }
    if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
      throw new IllegalStateException(
        "Cannot mark a cancelled invoice as paid"
      );
    }
    invoice.setStatus(InvoiceStatus.PAID);
    invoice.setPaymentDate(paymentDate);
    Invoice saved = invoiceRepository.save(invoice);
    log.info("Invoice {} marked as paid", saved.getInvoiceNumber());
    return toResponse(saved);
  }

  /**
   * Cancels an invoice that has not been paid yet.
   *
   * @param id the invoice ID
   * @return the updated invoice response
   */
  @Transactional
  @CacheEvict(value = "dashboard", key = "'spending'")
  public InvoiceResponse cancelInvoice(Long id) {
    Invoice invoice = findById(id);
    if (invoice.getStatus() == InvoiceStatus.PAID) {
      throw new IllegalStateException("Cannot cancel a paid invoice");
    }
    invoice.setStatus(InvoiceStatus.CANCELLED);
    Invoice saved = invoiceRepository.save(invoice);
    return toResponse(saved);
  }

  private Map<Long, BigDecimal> getTotalReceivedByProduct(Long orderId) {
    return goodReceiptRepository
      .getTotalReceivedByOrder(orderId)
      .stream()
      .collect(
        Collectors.toMap(
          GoodReceiptRepository.ReceivedQuantityProjection::getProductId,
          GoodReceiptRepository.ReceivedQuantityProjection::getTotalReceived
        )
      );
  }

  private Invoice findById(Long id) {
    return invoiceRepository
      .findByIdDetailed(id)
      .orElseThrow(() ->
        new EntityNotFoundException("Invoice not found: " + id)
      );
  }

  private InvoiceResponse toResponse(Invoice invoice) {
    List<InvoiceItemResponse> itemResponses = invoice
      .getItems()
      .stream()
      .map(item ->
        new InvoiceItemResponse(
          item.getProduct().getId(),
          item.getProduct().getName(),
          item.getProduct().getSku(),
          item.getQuantity(),
          item.getUnitPrice(),
          item.getLineTotal(),
          item.getPoQuantity(),
          item.getReceivedQuantity(),
          item.getQuantityMatch(),
          item.getPriceMatch(),
          item.getNotes()
        )
      )
      .toList();

    return new InvoiceResponse(
      invoice.getId(),
      invoice.getInvoiceNumber(),
      invoice.getPurchaseOrder().getId(),
      invoice.getPurchaseOrder().getOrderNumber(),
      invoice.getSupplier().getName(),
      invoice.getInvoiceDate(),
      invoice.getDueDate(),
      invoice.getTotalAmount(),
      invoice.getTaxAmount(),
      invoice.getStatus(),
      invoice.getMatchingNotes(),
      invoice.getPaymentDate(),
      invoice.getNotes(),
      invoice.getCreatedAt(),
      itemResponses
    );
  }

  private String generateInvoiceNumber() {
    Long nextSeq = invoiceRepository.getNextInvoiceSequence();
    return String.format("FAC-%05d/%d", nextSeq, Year.now().getValue());
  }
}
