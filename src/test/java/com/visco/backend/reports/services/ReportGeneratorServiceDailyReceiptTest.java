package com.visco.backend.reports.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.GoodReceiptItem;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.PurchaseOrder;
import com.visco.backend.models.entities.PurchaseOrderItem;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.models.entities.Uom;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.reports.models.dtos.DailyReceiptReportKPIs;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.InventoryMovementRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.WarehouseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for the partial-reception fix in
 * {@link ReportGeneratorService#generateDailyReceiptReport(Long, LocalDateTime, LocalDateTime)}.
 *
 * The bug: when a PO is received in several partial deliveries the report
 * iterated over {@link GoodReceipt} rows only, so:
 * <ul>
 *   <li>the status of the last partial that actually completes the PO
 *       was still reported as PARCIAL (since each row's
 *       {@code receivedQuantity < expectedQuantity});</li>
 *   <li>the KPIs (Total Items Expected, Completeness%) summed the full
 *       PO quantity once per partial, inflating the expected and
 *       undercounting the actual completeness.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportGeneratorServiceDailyReceiptTest {

  @Mock private ProductRepository productRepository;
  @Mock private StockLevelRepository stockLevelRepository;
  @Mock private InventoryMovementRepository movementRepository;
  @Mock private WarehouseRepository warehouseRepository;
  @Mock private GoodReceiptRepository goodReceiptRepository;

  @InjectMocks private ReportGeneratorService reportGeneratorService;

  private Warehouse warehouse;
  private Supplier supplier;
  private Product productA;
  private Product productB;
  private PurchaseOrder po;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
      reportGeneratorService, "maxRecords", 50000
    );

    warehouse = Warehouse.builder()
      .id(10L)
      .name("Main Warehouse")
      .physicalAddress("Calle 1")
      .description("Main")
      .active(true)
      .build();

    supplier = Supplier.builder()
      .id(1L)
      .name("Supplier X")
      .address("Av 1")
      .email("s@x.com")
      .description("desc")
      .currency(Currency.USD)
      .active(true)
      .build();

    productA = Product.builder()
      .id(100L)
      .internalCode("IC-A")
      .sku("SKU-A")
      .name("Product A")
      .description("d")
      .sapCode("SAP-A")
      .uom(Uom.EA)
      .reorderPoint(BigDecimal.ZERO)
      .maxStock(BigDecimal.valueOf(1000))
      .active(true)
      .build();

    productB = Product.builder()
      .id(200L)
      .internalCode("IC-B")
      .sku("SKU-B")
      .name("Product B")
      .description("d")
      .sapCode("SAP-B")
      .uom(Uom.EA)
      .reorderPoint(BigDecimal.ZERO)
      .maxStock(BigDecimal.valueOf(1000))
      .active(true)
      .build();

    PurchaseOrderItem itemA = PurchaseOrderItem.builder()
      .id(11L)
      .product(productA)
      .quantity(BigDecimal.valueOf(10)) // PO total: 10
      .unitPrice(BigDecimal.valueOf(5))
      .build();
    PurchaseOrderItem itemB = PurchaseOrderItem.builder()
      .id(12L)
      .product(productB)
      .quantity(BigDecimal.valueOf(5)) // PO total: 5
      .unitPrice(BigDecimal.valueOf(3))
      .build();
    itemA.setPurchaseOrder(null);
    itemB.setPurchaseOrder(null);

    po = PurchaseOrder.builder()
      .id(99L)
      .orderNumber("PO-0001")
      .description("Test PO")
      .supplier(supplier)
      .destinationWarehouse(warehouse)
      .items(new ArrayList<>(List.of(itemA, itemB)))
      .build();
    itemA.setPurchaseOrder(po);
    itemB.setPurchaseOrder(po);
  }

  /**
   * A PO of 10 units of product A and 5 units of product B is received
   * in two partial deliveries: 4+2 the first day, 6+3 the second day
   * (which completes the PO). Both deliveries fall in the report window.
   */
  @Test
  void generateDailyReceiptReport_lastPartialThatCompletesPo_isMarkedCompletada() {
    GoodReceipt first = receipt("GR-0001", LocalDateTime.of(2026, 1, 1, 9, 0),
      List.of(
        item(productA, BigDecimal.valueOf(10), BigDecimal.valueOf(4)),
        item(productB, BigDecimal.valueOf(5),  BigDecimal.valueOf(2))
      ));
    GoodReceipt second = receipt("GR-0002", LocalDateTime.of(2026, 1, 1, 17, 0),
      List.of(
        item(productA, BigDecimal.valueOf(10), BigDecimal.valueOf(6)),
        item(productB, BigDecimal.valueOf(5),  BigDecimal.valueOf(3))
      ));

    when(goodReceiptRepository.findForDailyReport(any(), any(), any()))
      .thenReturn(List.of(first, second));
    // Cumulative across ALL receptions for the PO (10 + 5).
    GoodReceiptRepository.OrderProductReceivedProjection cumA =
      cumulative(po.getId(), productA.getId(), BigDecimal.valueOf(10));
    GoodReceiptRepository.OrderProductReceivedProjection cumB =
      cumulative(po.getId(), productB.getId(), BigDecimal.valueOf(5));
    when(goodReceiptRepository.getCumulativeReceivedForOrders(List.of(po.getId())))
      .thenReturn(List.of(cumA, cumB));

    DailyReceiptReportKPIs kpis = reportGeneratorService.generateDailyReceiptReport(
      warehouse.getId(),
      LocalDateTime.of(2026, 1, 1, 0, 0),
      LocalDateTime.of(2026, 1, 2, 0, 0)
    );

    assertNotNull(kpis);
    var rows = kpis.getRows();
    assertEquals(2, rows.size());

    // First partial: PO is at 6/15 → PARCIAL.
    var firstRow = rows.get(0);
    assertEquals("GR-0001", firstRow.getReceiptNumber());
    assertEquals("PARCIAL", firstRow.getStatus());
    assertEquals(0, BigDecimal.valueOf(6).compareTo(firstRow.getCumulativeReceivedQty()));
    assertEquals(0, BigDecimal.valueOf(15).compareTo(firstRow.getTotalOrderedQty()));
    assertEquals(0,
      new BigDecimal("40.0").compareTo(firstRow.getCumulativeCompletenessPct()));

    // Second partial: PO is now at 15/15 → COMPLETADA. (Bug fix.)
    var secondRow = rows.get(1);
    assertEquals("GR-0002", secondRow.getReceiptNumber());
    assertEquals("COMPLETADA", secondRow.getStatus());
    assertEquals(0, BigDecimal.valueOf(15).compareTo(secondRow.getCumulativeReceivedQty()));
    assertEquals(0,
      new BigDecimal("100.0").compareTo(secondRow.getCumulativeCompletenessPct()));
  }

  /**
   * KPIs are PO-based: the same PO received in 3 partials must be
   * counted as ONE order, ONE completion (if cumulative meets ordered),
   * and the expected/received totals must not be inflated by 3.
   */
  @Test
  void generateDailyReceiptReport_kpisArePoBasedNotReceiptBased() {
    // Single-product PO so the math is unambiguous.
    PurchaseOrderItem only = PurchaseOrderItem.builder()
      .id(99L)
      .product(productA)
      .quantity(BigDecimal.valueOf(10))
      .unitPrice(BigDecimal.valueOf(5))
      .build();
    PurchaseOrder oneItemPo = PurchaseOrder.builder()
      .id(po.getId())
      .orderNumber("PO-0001")
      .description("Test PO")
      .supplier(supplier)
      .destinationWarehouse(warehouse)
      .items(new ArrayList<>(List.of(only)))
      .build();
    only.setPurchaseOrder(oneItemPo);

    GoodReceipt r1 = receiptFor(oneItemPo, "GR-0001", LocalDateTime.of(2026, 1, 1, 9, 0),
      List.of(item(productA, BigDecimal.valueOf(10), BigDecimal.valueOf(3))));
    GoodReceipt r2 = receiptFor(oneItemPo, "GR-0002", LocalDateTime.of(2026, 1, 1, 12, 0),
      List.of(item(productA, BigDecimal.valueOf(10), BigDecimal.valueOf(3))));
    GoodReceipt r3 = receiptFor(oneItemPo, "GR-0003", LocalDateTime.of(2026, 1, 1, 15, 0),
      List.of(item(productA, BigDecimal.valueOf(10), BigDecimal.valueOf(4))));

    when(goodReceiptRepository.findForDailyReport(any(), any(), any()))
      .thenReturn(List.of(r1, r2, r3));
    GoodReceiptRepository.OrderProductReceivedProjection cumA2 =
      cumulative(oneItemPo.getId(), productA.getId(), BigDecimal.valueOf(10));
    when(goodReceiptRepository.getCumulativeReceivedForOrders(List.of(oneItemPo.getId())))
      .thenReturn(List.of(cumA2));

    DailyReceiptReportKPIs kpis = reportGeneratorService.generateDailyReceiptReport(
      warehouse.getId(),
      LocalDateTime.of(2026, 1, 1, 0, 0),
      LocalDateTime.of(2026, 1, 2, 0, 0)
    );

    // 3 receipts, but 1 distinct PO.
    assertEquals(3, kpis.getTotalReceipts());
    assertEquals(1, kpis.getTotalOrders());
    assertEquals(1, kpis.getTotalCompleted());
    assertEquals(0, kpis.getTotalPartial());

    // Items expected = PO ordered (10), NOT 30.
    assertEquals(10, kpis.getTotalItemsExpected());
    assertEquals(10, kpis.getTotalItemsReceived());
    assertEquals(100.0, kpis.getOverallCompletenessPct());
  }

  @Test
  void generateDailyReceiptReport_stillPartial_kpisCountPartial() {
    GoodReceipt partial = receipt("GR-0001", LocalDateTime.of(2026, 1, 1, 9, 0),
      List.of(item(productA, BigDecimal.valueOf(10), BigDecimal.valueOf(3))));

    when(goodReceiptRepository.findForDailyReport(any(), any(), any()))
      .thenReturn(List.of(partial));
    GoodReceiptRepository.OrderProductReceivedProjection cumA =
      cumulative(po.getId(), productA.getId(), BigDecimal.valueOf(3));
    GoodReceiptRepository.OrderProductReceivedProjection cumB =
      cumulative(po.getId(), productB.getId(), BigDecimal.ZERO);
    when(goodReceiptRepository.getCumulativeReceivedForOrders(List.of(po.getId())))
      .thenReturn(List.of(cumA, cumB));

    DailyReceiptReportKPIs kpis = reportGeneratorService.generateDailyReceiptReport(
      warehouse.getId(),
      LocalDateTime.of(2026, 1, 1, 0, 0),
      LocalDateTime.of(2026, 1, 2, 0, 0)
    );

    assertEquals(1, kpis.getTotalOrders());
    assertEquals(0, kpis.getTotalCompleted());
    assertEquals(1, kpis.getTotalPartial());
    assertEquals("PARCIAL", kpis.getRows().get(0).getStatus());
  }

  // ─────────────────────────────────────────────────────────────
  // helpers
  // ─────────────────────────────────────────────────────────────

  private GoodReceipt receipt(
    String number,
    LocalDateTime receivedAt,
    List<GoodReceiptItem> items
  ) {
    return receiptFor(po, number, receivedAt, items);
  }

  private GoodReceipt receiptFor(
    PurchaseOrder targetPo,
    String number,
    LocalDateTime receivedAt,
    List<GoodReceiptItem> items
  ) {
    GoodReceipt r = GoodReceipt.builder()
      .receiptNumber(number)
      .purchaseOrder(targetPo)
      .destinationWarehouse(warehouse)
      .receivedAt(receivedAt)
      .closed(false)
      .items(new ArrayList<>(items))
      .build();
    items.forEach(i -> i.setGoodReceipt(r));
    return r;
  }

  private GoodReceiptItem item(
    Product product,
    BigDecimal expected,
    BigDecimal received
  ) {
    return GoodReceiptItem.builder()
      .product(product)
      .expectedQuantity(expected)
      .receivedQuantity(received)
      .build();
  }

  private GoodReceiptRepository.OrderProductReceivedProjection cumulative(
    Long orderId,
    Long productId,
    BigDecimal totalReceived
  ) {
    GoodReceiptRepository.OrderProductReceivedProjection p =
      org.mockito.Mockito.mock(GoodReceiptRepository.OrderProductReceivedProjection.class);
    when(p.getOrderId()).thenReturn(orderId);
    when(p.getProductId()).thenReturn(productId);
    when(p.getTotalReceived()).thenReturn(totalReceived);
    return p;
  }
}
