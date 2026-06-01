package com.visco.backend.reports.services;

import com.visco.backend.models.entities.InventoryMovement;
import com.visco.backend.models.entities.MovementType;
import com.visco.backend.models.entities.Product;
import com.visco.backend.models.entities.StockLevel;
import com.visco.backend.models.entities.Warehouse;
import com.visco.backend.repositories.InventoryMovementRepository;
import com.visco.backend.repositories.ProductRepository;
import com.visco.backend.repositories.StockLevelRepository;
import com.visco.backend.repositories.WarehouseRepository;
import com.visco.backend.reports.models.dtos.AlertReportDTO;
import com.visco.backend.reports.models.dtos.MovementReportDTO;
import com.visco.backend.reports.models.dtos.StockReportDTO;
import com.visco.backend.reports.models.dtos.StockReportDTO.WarehouseStockInfo;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.CategoryDistributionDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.TopProductDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportGeneratorService {

    private final ProductRepository productRepository;
    private final StockLevelRepository stockLevelRepository;
    private final InventoryMovementRepository movementRepository;
    private final WarehouseRepository warehouseRepository;

    public List<StockReportDTO> generateStockReport(
            LocalDateTime startDate, LocalDateTime endDate,
            Long categoryId, Long warehouseId, String search) {
        List<Product> products;

        if (categoryId != null) {
            var catPage = productRepository.findByCategoryIdWithFetch(categoryId, PageRequest.of(0, Integer.MAX_VALUE));
            products = catPage.getContent();
        } else {
            products = productRepository.findAll();
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(q)
                            || p.getSku().toLowerCase().contains(q)
                            || p.getInternalCode().toLowerCase().contains(q))
                    .toList();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, BigDecimal[]> stockMap = stockLevelRepository.sumStockByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new BigDecimal[]{
                                row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO,
                                row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO
                        }));

        Map<Long, List<StockLevel>> stockByProduct = productIds.isEmpty() ? Map.of()
                : productIds.stream()
                    .flatMap(id -> stockLevelRepository.findByProductId(id).stream())
                    .collect(Collectors.groupingBy(s -> s.getProduct().getId()));

        return products.stream().map(product -> {
            BigDecimal[] stocks = stockMap.getOrDefault(product.getId(),
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal currentStock = stocks[0];
            BigDecimal reorderPoint = product.getReorderPoint() != null ? product.getReorderPoint() : BigDecimal.ZERO;
            BigDecimal maxStock = product.getMaxStock() != null ? product.getMaxStock() : BigDecimal.ZERO;

            String status;
            if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
                status = "CRITICO";
            } else if (currentStock.compareTo(reorderPoint) <= 0) {
                status = "BAJO";
            } else if (maxStock.compareTo(BigDecimal.ZERO) > 0 && currentStock.compareTo(maxStock) >= 0) {
                status = "EXCESO";
            } else {
                status = "NORMAL";
            }

            List<StockLevel> slList = stockByProduct.getOrDefault(product.getId(), List.of());
            List<WarehouseStockInfo> warehouseDetails = slList.stream()
                    .filter(sl -> warehouseId == null || sl.getWarehouse().getId().equals(warehouseId))
                    .map(sl -> WarehouseStockInfo.builder()
                            .warehouseName(sl.getWarehouse().getName())
                            .stock(sl.getCurrentStock())
                            .build())
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
                    .category(product.getCategory() != null ? product.getCategory().getName() : null)
                    .supplier(product.getSupplier() != null ? product.getSupplier().getName() : null)
                    .uom(product.getUom() != null ? product.getUom().name() : null)
                    .status(status)
                    .warehouseDetails(warehouseDetails)
                    .build();
        })
        .filter(r -> r != null)
        .collect(Collectors.toList());
    }

    public List<MovementReportDTO> generateMovementReport(
            LocalDateTime startDate, LocalDateTime endDate,
            String movementType, Long productId, Long warehouseId, String userName) {
        MovementType typeFilter = movementType != null ? MovementType.valueOf(movementType) : null;

        var page = movementRepository
                .findMovementsWithFilters(productId, warehouseId, typeFilter, startDate, endDate,
                        PageRequest.of(0, Integer.MAX_VALUE));

        return page.getContent().stream().map(m -> {
            String wh = m.getFromWarehouse() != null ? m.getFromWarehouse().getName() : "";
            String whDest = m.getToWarehouse() != null ? m.getToWarehouse().getName() : "";
            String ref = switch (m.getType()) {
                case INPUT -> "Recepción";
                case OUTPUT -> "Salida";
                case TRANSFER -> "Transferencia";
                case ADJUSTMENT -> "Ajuste";
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
                    .userName(m.getCreatedBy() != null ? m.getCreatedBy().getName() : "")
                    .reference(ref)
                    .reason(m.getReason())
                    .build();
        }).collect(Collectors.toList());
    }

    public List<AlertReportDTO> generateAlertReport(
            String severity, String alertType, Long warehouseId) {
        List<AlertReportDTO> alerts = new ArrayList<>();

        var belowReorder = productRepository.findCriticalInventory();
        for (var p : belowReorder) {
            if (warehouseId != null) {
                BigDecimal whStock = stockLevelRepository.getStockByProductAndWarehouse(p.getProductId(), warehouseId);
                if (whStock == null || whStock.compareTo(BigDecimal.ZERO) <= 0) continue;
            }

            BigDecimal currentStock = p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO;
            BigDecimal reorderPoint = p.getReorderPoint() != null ? p.getReorderPoint() : BigDecimal.ZERO;

            String alertTypeStr = currentStock.compareTo(BigDecimal.ZERO) <= 0 ? "FUERA_STOCK" : "STOCK_BAJO";
            String sev = currentStock.compareTo(BigDecimal.ZERO) <= 0 ? "CRITICA"
                    : currentStock.compareTo(reorderPoint.multiply(BigDecimal.valueOf(0.5))) <= 0 ? "CRITICA" : "ALTA";

            if (alertType != null && !alertType.equals(alertTypeStr)) continue;
            if (severity != null && !severity.equals(sev)) continue;

            List<StockLevel> slList = stockLevelRepository.findByProductId(p.getProductId());
            List<String> warehouses = slList.stream()
                    .filter(sl -> sl.getCurrentStock().compareTo(BigDecimal.ZERO) > 0)
                    .map(sl -> sl.getWarehouse().getName())
                    .toList();

            alerts.add(AlertReportDTO.builder()
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
                    .recommendedAction(currentStock.compareTo(BigDecimal.ZERO) <= 0
                            ? "Realizar compra de emergencia" : "Reabastecer producto")
                    .affectedWarehouses(warehouses)
                    .build());
        }

        var overstock = productRepository.findOverstockInventory();
        for (var p : overstock) {
            boolean already = alerts.stream().anyMatch(a -> a.getProductId().equals(p.getProductId()));
            if (already) continue;

            if (warehouseId != null) {
                BigDecimal whStock = stockLevelRepository.getStockByProductAndWarehouse(p.getProductId(), warehouseId);
                if (whStock == null || whStock.compareTo(BigDecimal.ZERO) <= 0) continue;
            }

            BigDecimal currentStock = p.getCurrentStock() != null ? p.getCurrentStock() : BigDecimal.ZERO;

            if (alertType != null && !"EXCESO".equals(alertType)) continue;
            if (severity != null && !"MEDIA".equals(severity)) continue;

            List<StockLevel> slList = stockLevelRepository.findByProductId(p.getProductId());
            List<String> warehouses = slList.stream()
                    .filter(sl -> sl.getCurrentStock().compareTo(BigDecimal.ZERO) > 0)
                    .map(sl -> sl.getWarehouse().getName())
                    .toList();

            alerts.add(AlertReportDTO.builder()
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
                    .build());
        }

        return alerts;
    }

    public List<WarehouseAnalysisDTO> generateWarehouseAnalysis(Long warehouseId) {
        List<Warehouse> warehouses;
        if (warehouseId != null) {
            warehouses = warehouseRepository.findAllById(List.of(warehouseId));
        } else {
            warehouses = warehouseRepository.findAll();
        }

        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<WarehouseAnalysisDTO> result = new ArrayList<>();

        for (Warehouse w : warehouses) {
            var whPage = stockLevelRepository.findAllStockByWarehouse(
                    Pageable.ofSize(Integer.MAX_VALUE), w.getId(), null);
            List<StockLevel> positiveStock = whPage.getContent().stream()
                    .filter(sl -> sl.getCurrentStock().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            int productCount = positiveStock.size();
            int criticalCount = 0;
            int lowStockCount = 0;

            for (StockLevel sl : positiveStock) {
                Product p = productMap.get(sl.getProduct().getId());
                if (p == null) continue;
                BigDecimal rp = p.getReorderPoint() != null ? p.getReorderPoint() : BigDecimal.ZERO;
                if (sl.getCurrentStock().compareTo(rp) <= 0) {
                    lowStockCount++;
                    if (sl.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0) {
                        criticalCount++;
                    }
                }
            }

            List<TopProductDTO> topByQuantity = positiveStock.stream()
                    .sorted((a, b) -> b.getCurrentStock().compareTo(a.getCurrentStock()))
                    .limit(10)
                    .map(sl -> {
                        Product p = productMap.get(sl.getProduct().getId());
                        return TopProductDTO.builder()
                                .productName(p != null ? p.getName() : "N/A")
                                .value(sl.getCurrentStock())
                                .build();
                    })
                    .collect(Collectors.toList());

            Map<String, Long> categoryCount = positiveStock.stream()
                    .collect(Collectors.groupingBy(
                            sl -> {
                                Product p = productMap.get(sl.getProduct().getId());
                                return p != null && p.getCategory() != null ? p.getCategory().getName() : "Sin categoría";
                            },
                            Collectors.counting()));

            long totalCat = categoryCount.values().stream().mapToLong(Long::longValue).sum();
            List<CategoryDistributionDTO> categoryDist = categoryCount.entrySet().stream()
                    .map(e -> CategoryDistributionDTO.builder()
                            .categoryName(e.getKey())
                            .quantity(e.getValue().intValue())
                            .percentage(totalCat > 0 ? (e.getValue().doubleValue() / totalCat) * 100.0 : 0.0)
                            .build())
                    .collect(Collectors.toList());

            double capacityUtilization = Math.min(100.0, (double) productCount / Math.max(1, 100) * 100);

            result.add(WarehouseAnalysisDTO.builder()
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
                    .build());
        }

        return result;
    }
}
