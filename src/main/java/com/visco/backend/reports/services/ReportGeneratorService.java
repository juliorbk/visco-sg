package com.visco.backend.reports.services;

import com.visco.backend.models.entities.GoodReceipt;
import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.MovementType;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.reports.models.dtos.AlertReportDTO;
import com.visco.backend.reports.models.dtos.DailyReceiptReportDTO;
import com.visco.backend.reports.models.dtos.DailyReceiptReportKPIs;
import com.visco.backend.reports.models.dtos.MovementReportDTO;
import com.visco.backend.reports.models.dtos.StockReportDTO;
import com.visco.backend.reports.models.dtos.StockReportDTO.WarehouseStockInfo;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.CategoryDistributionDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.TopProductDTO;
import com.visco.backend.repositories.GoodReceiptRepository;
import com.visco.backend.repositories.InventoryMovementRepository;
import com.visco.backend.repositories.InventoryMovementSpecification;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.ProductSpecification;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.WarehouseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/**
 * Generates the raw data DTOs that feed into Excel and PDF export services.
 */
public class ReportGeneratorService {

  private final ProductRepository productRepository;
  private final StockLevelRepository stockLevelRepository;
  private final InventoryMovementRepository movementRepository;
  private final WarehouseRepository warehouseRepository;
  private final GoodReceiptRepository goodReceiptRepository;

  @Value("${app.reports.max-records-per-export:50000}")
  private int maxRecords;

  /**
   * Builds a list of StockReportDTO with current stock, pending stock, and status classification.
   * Capped at {@code maxRecords} via SQL pagination.
   */
  public List<StockReportDTO> generateStockReport(
    LocalDateTime startDate,
    LocalDateTime endDate,
    Long categoryId,
    Long warehouseId,
    String search
  ) {
    var pageable = PageRequest.ofSize(maxRecords);
    var productPage = productRepository.findAll(
      Specification.where(ProductSpecification.freeSearchAcrossFields(search))
        .and(ProductSpecification.hasCategory(categoryId)),
      pageable
    );
    List<Product> products = productPage.getContent();

    if (products.size() == maxRecords && productPage.getTotalElements() > maxRecords) {
      log.warn(
        "Stock report hit maxRecords cap ({} of {} total). "
          + "Consider narrowing category/warehouse filters to see all data.",
        maxRecords, productPage.getTotalElements()
      );
    }

    List<Long> productIds = products.stream().map(Product::getId).toList();
    Map<Long, BigDecimal[]> stockMap = stockLevelRepository
      .sumStockByProductIds(productIds)
      .stream()
      .collect(
        Collectors.toMap(
          row -> (Long) row[0],
          row ->
            new BigDecimal[] {
              row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO,
              row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO,
            }
        )
      );

    Map<Long, List<StockLevel>> stockByProduct = productIds.isEmpty()
      ? Map.of()
      : stockLevelRepository
          .findByProductIdIn(productIds)
          .stream()
          .collect(Collectors.groupingBy(s -> s.getProduct().getId()));

    return products
      .stream()
      .map(product -> {
        BigDecimal[] stocks = stockMap.getOrDefault(
          product.getId(),
          new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO }
        );
        BigDecimal currentStock = stocks[0];
        BigDecimal reorderPoint =
          product.getReorderPoint() != null
            ? product.getReorderPoint()
            : BigDecimal.ZERO;
        BigDecimal maxStock =
          product.getMaxStock() != null
            ? product.getMaxStock()
            : BigDecimal.ZERO;

        String status;
        if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
          status = "CRITICO";
        } else if (currentStock.compareTo(reorderPoint) <= 0) {
          status = "BAJO";
        } else if (
          maxStock.compareTo(BigDecimal.ZERO) > 0 &&
          currentStock.compareTo(maxStock) >= 0
        ) {
          status = "EXCESO";
        } else {
          status = "NORMAL";
        }

        List<StockLevel> slList = stockByProduct.getOrDefault(
          product.getId(),
          List.of()
        );
        List<WarehouseStockInfo> warehouseDetails = slList
          .stream()
          .filter(
            sl ->
              warehouseId == null ||
              sl.getWarehouse().getId().equals(warehouseId)
          )
          .map(sl ->
            WarehouseStockInfo.builder()
              .warehouseName(sl.getWarehouse().getName())
              .stock(sl.getCurrentStock())
              .build()
          )
          .toList();

        if (warehouseId != null && warehouseDetails.isEmpty()) {
          return null;
        }

        return StockReportDTO.builder()
          .productId(product.getId())
          .internalCode(product.getInternalCode())
          .sku(product.getSku())
          .productName(product.getName())
          .currentStock(currentStock)
          .pendingStock(stocks[1])
          .maxStock(maxStock)
          .reorderPoint(reorderPoint)
          .category(
            product.getCategory() != null
              ? product.getCategory().getName()
              : null
          )
          .supplier(
            product.getSupplier() != null
              ? product.getSupplier().getName()
              : null
          )
          .uom(product.getUom() != null ? product.getUom().name() : null)
          .status(status)
          .warehouseDetails(warehouseDetails)
          .build();
      })
      .filter(r -> r != null)
      .collect(Collectors.toList());
  }

  /**
   * Builds a list of MovementReportDTO from inventory movements using a streaming query.
   * The result is capped at {@code maxRecords} via SQL pagination (LIMIT) so we don't
   * pull millions of rows into memory.
   */
  public List<MovementReportDTO> generateMovementReport(
    LocalDateTime startDate,
    LocalDateTime endDate,
    String movementType,
    Long productId,
    Long warehouseId,
    String userName
  ) {
    MovementType typeFilter =
      movementType != null ? MovementType.valueOf(movementType) : null;

    Specification<InventoryMovement> spec = Specification.where(
        InventoryMovementSpecification.withAssociations()
      )
      .and(InventoryMovementSpecification.hasProductId(productId))
      .and(InventoryMovementSpecification.touchesWarehouse(warehouseId))
      .and(InventoryMovementSpecification.hasType(typeFilter))
      .and(
        InventoryMovementSpecification.createdAtBetween(startDate, endDate)
      );

    List<InventoryMovement> movements = movementRepository.findAll(
      spec,
      PageRequest.of(0, maxRecords, Sort.by("createdAt").ascending())
    ).getContent();

    if (movements.size() == maxRecords) {
      log.warn(
        "Movement report hit maxRecords cap ({}). Results may be truncated; "
          + "consider narrowing date range or filters.",
        maxRecords
      );
    }

    return movements
      .stream()
      .map(m -> {
          String wh =
            m.getFromWarehouse() != null ? m.getFromWarehouse().getName() : "";
          String whDest =
            m.getToWarehouse() != null ? m.getToWarehouse().getName() : "";
          String ref = switch (m.getType()) {
            case INPUT -> "Receipt";
            case OUTPUT -> "Output";
            case TRANSFER -> "Transfer";
            case ADJUSTMENT -> "Adjustment";
            case DISPATCH -> "Dispatch";
          };

          return MovementReportDTO.builder()
            .id(m.getId())
            .movementDate(m.getCreatedAt())
            .movementType(m.getType().name())
            .productId(m.getProduct().getId())
            .productCode(m.getProduct().getInternalCode())
            .sku(m.getProduct().getSku())
            .productName(m.getProduct().getName())
            .quantity(m.getQuantity())
            .warehouseName(wh)
            .warehouseDestination(whDest)
            .userName(
              m.getCreatedBy() != null ? m.getCreatedBy().getName() : ""
            )
            .reference(ref)
            .reason(m.getReason())
            .build();
        })
        .collect(Collectors.toList());
  }

  /**
   * Builds a list of AlertReportDTO by evaluating products below reorder point and overstock.
   * Capped at min(maxRecords, 2000) per source (below-reorder, overstock) to keep the
   * export bounded even with 260k+ products in the catalog.
   */
  public List<AlertReportDTO> generateAlertReport(
    String severity,
    String alertType,
    Long warehouseId
  ) {
    List<AlertReportDTO> alerts = new ArrayList<>();
    int maxAlerts = Math.min(maxRecords, 2000);

    var belowReorder = productRepository.findCriticalInventory(
      PageRequest.of(0, maxAlerts)
    );
    var overstock = productRepository.findOverstockInventory(
      PageRequest.of(0, maxAlerts)
    );

    if (belowReorder.size() == maxAlerts || overstock.size() == maxAlerts) {
      log.warn(
        "Alert report hit per-source cap ({}). Some alerts may not appear; "
          + "filter by warehouse for a tighter result set.",
        maxAlerts
      );
    }

    List<Long> allProductIds = new ArrayList<>();
    for (var p : belowReorder) allProductIds.add(p.getProductId());
    for (var p : overstock) allProductIds.add(p.getProductId());

    Map<Long, List<StockLevel>> stockByProduct = allProductIds.isEmpty()
      ? Map.of()
      : stockLevelRepository
          .findByProductIdIn(allProductIds)
          .stream()
          .collect(Collectors.groupingBy(s -> s.getProduct().getId()));

    // Pre-cargar stock por warehouse si es necesario
    Map<Long, BigDecimal> stockByProductAndWarehouse;
    if (warehouseId != null) {
      stockByProductAndWarehouse = allProductIds
        .stream()
        .collect(
          Collectors.toMap(
            pid -> pid,
            pid ->
              stockLevelRepository.getStockByProductAndWarehouse(
                pid,
                warehouseId
              ),
            (a, b) -> a
          )
        );
    } else {
      stockByProductAndWarehouse = Map.of();
    }

    for (var p : belowReorder) {
      if (warehouseId != null) {
        BigDecimal whStock = stockByProductAndWarehouse.getOrDefault(
          p.getProductId(),
          BigDecimal.ZERO
        );
        if (whStock.compareTo(BigDecimal.ZERO) <= 0) continue;
      }

      BigDecimal currentStock =
        p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO;
      BigDecimal reorderPoint =
        p.getReorderPoint() != null ? p.getReorderPoint() : BigDecimal.ZERO;

      String alertTypeStr =
        currentStock.compareTo(BigDecimal.ZERO) <= 0
          ? "FUERA_STOCK"
          : "STOCK_BAJO";
      String sev =
        currentStock.compareTo(BigDecimal.ZERO) <= 0
          ? "CRITICA"
          : currentStock.compareTo(
              reorderPoint.multiply(BigDecimal.valueOf(0.5))
            ) <=
            0
            ? "CRITICA"
            : "ALTA";

      if (alertType != null && !alertType.equals(alertTypeStr)) continue;
      if (severity != null && !severity.equals(sev)) continue;

      List<String> warehouses = stockByProduct
        .getOrDefault(p.getProductId(), List.of())
        .stream()
        .filter(sl -> sl.getCurrentStock().compareTo(BigDecimal.ZERO) > 0)
        .map(sl -> sl.getWarehouse().getName())
        .toList();

      alerts.add(
        AlertReportDTO.builder()
          .productId(p.getProductId())
          .internalCode("")
          .sku(p.getSku())
          .productName(p.getProductName())
          .currentStock(currentStock)
          .reorderPoint(reorderPoint)
          .maxStock(p.getMaxStock())
          .alertType(alertTypeStr)
          .severity(sev)
          .detectedAt(LocalDateTime.now())
          .recommendedAction(
            currentStock.compareTo(BigDecimal.ZERO) <= 0
              ? "Realizar compra de emergencia"
              : "Reabastecer producto"
          )
          .affectedWarehouses(warehouses)
          .build()
      );
    }

    for (var p : overstock) {
      boolean already = alerts
        .stream()
        .anyMatch(a -> a.getProductId().equals(p.getProductId()));
      if (already) continue;

      if (warehouseId != null) {
        BigDecimal whStock = stockByProductAndWarehouse.getOrDefault(
          p.getProductId(),
          BigDecimal.ZERO
        );
        if (whStock.compareTo(BigDecimal.ZERO) <= 0) continue;
      }

      BigDecimal currentStock =
        p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO;

      if (alertType != null && !"EXCESO".equals(alertType)) continue;
      if (severity != null && !"MEDIA".equals(severity)) continue;

      List<String> warehouses = stockByProduct
        .getOrDefault(p.getProductId(), List.of())
        .stream()
        .filter(sl -> sl.getCurrentStock().compareTo(BigDecimal.ZERO) > 0)
        .map(sl -> sl.getWarehouse().getName())
        .toList();

      alerts.add(
        AlertReportDTO.builder()
          .productId(p.getProductId())
          .internalCode("")
          .sku(p.getSku())
          .productName(p.getProductName())
          .currentStock(currentStock)
          .reorderPoint(p.getReorderPoint())
          .maxStock(p.getMaxStock())
          .alertType("EXCESO")
          .severity("MEDIA")
          .detectedAt(LocalDateTime.now())
          .recommendedAction("Revisar rotación, considerar devolución")
          .affectedWarehouses(warehouses)
          .build()
      );
    }

    return alerts;
  }

  /**
   * Builds a list of WarehouseAnalysisDTO with per-warehouse metrics, top products, and category distribution.
   */
  public List<WarehouseAnalysisDTO> generateWarehouseAnalysis(
    Long warehouseId
  ) {
    List<WarehouseAnalysisDTO> result = new ArrayList<>();
    List<Warehouse> warehouses;
    if (warehouseId != null) {
      warehouses = warehouseRepository.findAllById(List.of(warehouseId));
    } else {
      warehouses = warehouseRepository
        .findAll(Pageable.ofSize(maxRecords))
        .getContent();
    }

    for (Warehouse w : warehouses) {
      var whPage = stockLevelRepository.findByWarehouse(
        Pageable.ofSize(maxRecords),
        w.getId(),
        null,
        null,
        null,
        false
      );
      List<StockLevel> positiveStock = whPage
        .getContent()
        .stream()
        .filter(sl -> sl.getCurrentStock().compareTo(BigDecimal.ZERO) > 0)
        .toList();

      int productCount = positiveStock.size();
      int criticalCount = 0;
      int lowStockCount = 0;

      for (StockLevel sl : positiveStock) {
        Product p = sl.getProduct();
        BigDecimal rp =
          p.getReorderPoint() != null ? p.getReorderPoint() : BigDecimal.ZERO;
        if (sl.getCurrentStock().compareTo(rp) <= 0) {
          lowStockCount++;
          if (sl.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0) {
            criticalCount++;
          }
        }
      }

      List<TopProductDTO> topByQuantity = positiveStock
        .stream()
        .sorted((a, b) -> b.getCurrentStock().compareTo(a.getCurrentStock()))
        .limit(10)
        .map(sl ->
          TopProductDTO.builder()
            .productName(sl.getProduct().getName())
            .value(sl.getCurrentStock())
            .build()
        )
        .collect(Collectors.toList());

      Map<String, Long> categoryCount = positiveStock
        .stream()
        .collect(
          Collectors.groupingBy(
            sl -> {
              Product p = sl.getProduct();
              return p.getCategory() != null
                ? p.getCategory().getName()
                : "Sin categoría";
            },
            Collectors.counting()
          )
        );

      long totalCat = categoryCount
        .values()
        .stream()
        .mapToLong(Long::longValue)
        .sum();
      List<CategoryDistributionDTO> categoryDist = categoryCount
        .entrySet()
        .stream()
        .map(e ->
          CategoryDistributionDTO.builder()
            .categoryName(e.getKey())
            .quantity(e.getValue().intValue())
            .percentage(
              totalCat > 0
                ? (e.getValue().doubleValue() / totalCat) * 100.0
                : 0.0
            )
            .build()
        )
        .collect(Collectors.toList());

      double capacityUtilization = Math.min(
        100.0,
        ((double) productCount / Math.max(1, 100)) * 100
      );

      result.add(
        WarehouseAnalysisDTO.builder()
          .warehouseId(w.getId())
          .warehouseName(w.getName())
          .productCount(productCount)
          .totalValue(BigDecimal.ZERO)
          .capacityUtilization(capacityUtilization)
          .criticalProducts(criticalCount)
          .lowStockProducts(lowStockCount)
          .averageRotation(BigDecimal.ZERO)
          .topByValue(topByQuantity)
          .topByQuantity(topByQuantity)
          .categoryDistribution(categoryDist)
          .build()
      );
    }

    return result;
  }

  public DailyReceiptReportKPIs generateDailyReceiptReport(
    Long warehouseId,
    LocalDateTime start,
    LocalDateTime end
  ) {
    var receipts = goodReceiptRepository.findForDailyReport(warehouseId, start, end);

    if (receipts.isEmpty()) {
      return buildEmptyKpis(warehouseId);
    }

    // Ordered quantity per PO (sum of PO item quantities, all products).
    Map<Long, BigDecimal> orderedByPo = new java.util.HashMap<>();
    for (var gr : receipts) {
      var po = gr.getPurchaseOrder();
      if (po == null) continue;
      BigDecimal poOrdered = po.getItems().stream()
        .map(it -> it.getQuantity() != null ? it.getQuantity() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
      orderedByPo.merge(po.getId(), poOrdered, (a, b) -> a.signum() != 0 ? a : b);
    }

    // All-time cumulative received per PO, fetched in a single query
    // so we can seed the running total with the amount that arrived
    // BEFORE the report period.
    List<Long> poIds = receipts.stream()
      .map(gr -> gr.getPurchaseOrder().getId())
      .distinct()
      .toList();
    Map<Long, BigDecimal> totalReceivedAllTimeByPo = new java.util.HashMap<>();
    var cumulativeRows = goodReceiptRepository.getCumulativeReceivedForOrders(poIds);
    for (var row : cumulativeRows) {
      totalReceivedAllTimeByPo.merge(row.getOrderId(),
        row.getTotalReceived() != null ? row.getTotalReceived() : BigDecimal.ZERO,
        BigDecimal::add);
    }

    // Sort by receivedAt so we can walk the chronological order of
    // partial deliveries and compute the running cumulative state at
    // each reception. The running counter is seeded with the
    // pre-period total so receptions that happened before the report
    // window are not forgotten.
    var sortedReceipts = receipts.stream()
      .sorted(java.util.Comparator.comparing(GoodReceipt::getReceivedAt))
      .toList();

    // Map of PO ID → running cumulative received in chronological order,
    // seeded with the PRE-PERIOD amount (= all-time total minus what we
    // are about to replay from the report window). The all-time total
    // is the final state after every receipt, including those in this
    // report; subtracting the window sum gives us the seed so that
    // after replaying every report-row receipt the running total again
    // equals the all-time total.
    Map<Long, BigDecimal> inWindowReceivedByPo = new java.util.HashMap<>();
    for (var gr : sortedReceipts) {
      if (gr.getPurchaseOrder() == null) continue;
      BigDecimal rec = gr.getItems().stream()
        .map(i -> i.getReceivedQuantity() != null ? i.getReceivedQuantity() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
      inWindowReceivedByPo.merge(gr.getPurchaseOrder().getId(), rec, BigDecimal::add);
    }
    Map<Long, BigDecimal> runningCumulativeByPo = new java.util.HashMap<>();
    for (Long poId : poIds) {
      BigDecimal allTime = totalReceivedAllTimeByPo.getOrDefault(poId, BigDecimal.ZERO);
      BigDecimal inWindow = inWindowReceivedByPo.getOrDefault(poId, BigDecimal.ZERO);
      runningCumulativeByPo.put(poId, allTime.subtract(inWindow).max(BigDecimal.ZERO));
    }

    // We also need the sum of the report-window receipts per PO, so we
    // can subtract it from the all-time total and recover the
    // pre-period seed (already done above), AND so we can know exactly
    // which receipts to "replay" when re-deriving the per-row
    // cumulative. The running counter above does that implicitly.

    List<DailyReceiptReportDTO> rows = new ArrayList<>();
    for (var gr : sortedReceipts) {
      var po = gr.getPurchaseOrder();
      var supplier = po != null ? po.getSupplier() : null;
      var items = gr.getItems();

      BigDecimal totalReceivedThisEvent = items.stream()
        .map(i -> i.getReceivedQuantity() != null ? i.getReceivedQuantity() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

      // Update running cumulative (state AFTER this reception).
      BigDecimal cumulativeAtThis = runningCumulativeByPo.merge(
        po.getId(), totalReceivedThisEvent, BigDecimal::add);

      BigDecimal poOrdered = orderedByPo.getOrDefault(po.getId(), BigDecimal.ZERO);
      BigDecimal poCompletenessPct = poOrdered.compareTo(BigDecimal.ZERO) > 0
        ? cumulativeAtThis.multiply(BigDecimal.valueOf(100))
          .divide(poOrdered, 1, java.math.RoundingMode.HALF_UP)
        : BigDecimal.ZERO;
      String poStatus = poOrdered.compareTo(BigDecimal.ZERO) > 0
        && cumulativeAtThis.compareTo(poOrdered) >= 0
          ? "COMPLETADA" : "PARCIAL";

      rows.add(DailyReceiptReportDTO.builder()
        .receiptNumber(gr.getReceiptNumber())
        .receivedAt(gr.getReceivedAt())
        .purchaseOrderNumber(po != null ? po.getOrderNumber() : "")
        .supplierName(supplier != null ? supplier.getName() : "")
        .supplierRif(supplier != null ? supplier.getTaxId() : null)
        .supplierTaxId(supplier != null ? supplier.getTaxId() : null)
        .status(poStatus)
        .itemCount(items.size())
        .totalReceivedQty(totalReceivedThisEvent)
        .totalOrderedQty(poOrdered)
        .cumulativeReceivedQty(cumulativeAtThis)
        .cumulativeCompletenessPct(poCompletenessPct)
        .receivedBy(gr.getReceivedBy() != null ? gr.getReceivedBy().getName() : "")
        .notes(gr.getNotes())
        .build());
    }

    int totalReceipts = rows.size();
    // PO-level KPIs: count each PO at most once regardless of how many
    // partials it has. The receipt count is still per-reception.
    int totalOrders = poIds.size();
    int totalCompleted = (int) poIds.stream()
      .filter(id -> {
        BigDecimal ord = orderedByPo.getOrDefault(id, BigDecimal.ZERO);
        BigDecimal rec = totalReceivedAllTimeByPo.getOrDefault(id, BigDecimal.ZERO);
        return ord.signum() > 0 && rec.compareTo(ord) >= 0;
      })
      .count();
    int totalPartial = totalOrders - totalCompleted;

    // Items-received and items-expected are PO-based: each PO's ordered
    // and all-time-received quantities, summed across distinct POs (so
    // a 3-partial PO contributes its PO quantity once, not 3x).
    BigDecimal poTotalReceived = totalReceivedAllTimeByPo.values().stream()
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal poTotalOrdered = orderedByPo.values().stream()
      .filter(v -> v.signum() > 0)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    int totalItemsReceived = poTotalReceived.intValue();
    int totalItemsExpected = poTotalOrdered.intValue();
    double overallCompleteness = poTotalOrdered.compareTo(BigDecimal.ZERO) > 0
      ? poTotalReceived.multiply(BigDecimal.valueOf(100))
          .divide(poTotalOrdered, 1, java.math.RoundingMode.HALF_UP)
          .doubleValue()
      : 0.0;

    Map<String, Long> supplierCounts = receipts.stream()
      .map(gr -> gr.getPurchaseOrder() != null
        && gr.getPurchaseOrder().getSupplier() != null
          ? gr.getPurchaseOrder().getSupplier().getName() : "")
      .filter(name -> name != null && !name.isEmpty())
      .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
    String topSupplier = supplierCounts.entrySet().stream()
      .max(Map.Entry.comparingByValue())
      .map(Map.Entry::getKey).orElse("");

    Map<String, Long> productCounts = new java.util.LinkedHashMap<>();
    for (var gr : receipts) {
      for (var item : gr.getItems()) {
        String pname = item.getProduct() != null ? item.getProduct().getName() : "";
        productCounts.merge(pname, item.getReceivedQuantity().longValue(), Long::sum);
      }
    }
    String topProduct = productCounts.entrySet().stream()
      .max(Map.Entry.comparingByValue())
      .map(Map.Entry::getKey).orElse("");

    String warehouseName = receipts.get(0).getDestinationWarehouse().getName();

    DailyReceiptReportKPIs kpis = DailyReceiptReportKPIs.builder()
      .totalReceipts(totalReceipts)
      .totalOrders(totalOrders)
      .totalPartial(totalPartial)
      .totalCompleted(totalCompleted)
      .totalItemsReceived(totalItemsReceived)
      .totalItemsExpected(totalItemsExpected)
      .overallCompletenessPct(overallCompleteness)
      .topSupplier(topSupplier)
      .topProduct(topProduct)
      .generatedAt(LocalDateTime.now())
      .warehouseName(warehouseName)
      .build();

    kpis.setRows(rows);
    return kpis;
  }

  private DailyReceiptReportKPIs buildEmptyKpis(Long warehouseId) {
    String warehouseName = warehouseId != null
      ? warehouseRepository.findById(warehouseId).map(Warehouse::getName).orElse("")
      : "";
    return DailyReceiptReportKPIs.builder()
      .totalReceipts(0)
      .totalOrders(0)
      .totalPartial(0)
      .totalCompleted(0)
      .totalItemsReceived(0)
      .totalItemsExpected(0)
      .overallCompletenessPct(0.0)
      .generatedAt(LocalDateTime.now())
      .warehouseName(warehouseName)
      .build();
  }
}
