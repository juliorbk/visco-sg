package com.visco.backend.reports.services;

import com.visco.backend.reports.models.dtos.AlertReportDTO;
import com.visco.backend.reports.models.dtos.DailyReceiptReportDTO;
import com.visco.backend.reports.models.dtos.DailyReceiptReportKPIs;
import com.visco.backend.reports.models.dtos.MovementReportDTO;
import com.visco.backend.reports.models.dtos.StockReportDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.CategoryDistributionDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.TopProductDTO;
import com.visco.backend.reports.utils.DateUtils;
import com.visco.backend.reports.utils.NumberFormatter;
import java.awt.Color;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Exports report data to Excel (.xlsx) files using Apache POI.
 *
 * <p>Uses plain {@link XSSFWorkbook} (not SXSSF) because the streaming wrapper
 * leaks temp-file handling classes into Metaspace, which over time throws
 * {@code OutOfMemoryError: Metaspace} when many reports are generated
 * sequentially. The data set is bounded by
 * {@code app.reports.max-records-per-export} (default 50_000), which fits
 * comfortably in heap.
 */
@Slf4j
@Service
public class ExcelExportService {

  private static final XSSFColor PRIMARY = new XSSFColor(new Color(92, 18, 18), null);
  private static final XSSFColor ALT_ROW = new XSSFColor(new Color(245, 245, 247), null);
  private static final XSSFColor MUTED_TEXT = new XSSFColor(new Color(107, 114, 128), null);
  private static final XSSFColor WHITE = new XSSFColor(new Color(255, 255, 255), null);

  public void exportStockReportToExcel(
    List<StockReportDTO> data,
    String title,
    Map<String, String> metadata,
    OutputStream outputStream
  ) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Stock");
      int rowNum = 0;
      rowNum = buildHeader(wb, sheet, rowNum, title, metadata);
      rowNum = buildStockTable(wb, sheet, rowNum, data);
      writeStockOutput(wb, sheet, outputStream);
    } catch (Exception e) {
      throw new RuntimeException("Error generating Excel stock report", e);
    }
  }

  public void exportMovementReportToExcel(
    List<MovementReportDTO> data,
    String title,
    Map<String, String> metadata,
    OutputStream outputStream
  ) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Movements");
      int rowNum = 0;
      rowNum = buildHeader(wb, sheet, rowNum, title, metadata);
      rowNum = buildMovementTable(wb, sheet, rowNum, data);
      writeStockOutput(wb, sheet, outputStream);
    } catch (Exception e) {
      throw new RuntimeException("Error generating Excel movement report", e);
    }
  }

  public void exportAlertReportToExcel(
    List<AlertReportDTO> data,
    String title,
    Map<String, String> metadata,
    OutputStream outputStream
  ) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Warnings");
      int rowNum = 0;
      rowNum = buildHeader(wb, sheet, rowNum, title, metadata);
      buildAlertTable(wb, sheet, rowNum, data);
      writeStockOutput(wb, sheet, outputStream);
    } catch (Exception e) {
      throw new RuntimeException("Error generating Excel alert report", e);
    }
  }

  public void exportWarehouseAnalysisToExcel(
    List<WarehouseAnalysisDTO> data,
    String title,
    Map<String, String> metadata,
    OutputStream outputStream
  ) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet summarySheet = wb.createSheet("Summary");
      buildHeader(wb, summarySheet, 0, title, metadata);
      buildWarehouseSummaryTable(wb, summarySheet, 1, data);

      for (WarehouseAnalysisDTO wh : data) {
        Sheet whSheet = wb.createSheet(wh.getWarehouseName());
        buildWarehouseDetailSheet(wb, whSheet, wh);
      }
      writeStockOutput(wb, summarySheet, outputStream);
    } catch (Exception e) {
      throw new RuntimeException(
        "Error generating Excel warehouse analysis report",
        e
      );
    }
  }

  private void writeStockOutput(Workbook wb, Sheet sheet, OutputStream outputStream) throws java.io.IOException {
    for (int i = 0; i < 8; i++) {
      sheet.autoSizeColumn(i);
    }
    wb.write(outputStream);
  }

  public void exportDailyReceiptReportToExcel(
    List<DailyReceiptReportDTO> data,
    DailyReceiptReportKPIs kpis,
    String title,
    Map<String, String> metadata,
    OutputStream outputStream
  ) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("Recepciones Diarias");
      int rowNum = 0;
      rowNum = buildHeader(wb, sheet, rowNum, title, metadata);
      rowNum = buildDailyReceiptKpiBlock(wb, sheet, rowNum, kpis);
      rowNum = buildDailyReceiptTable(wb, sheet, rowNum, data);
      for (int i = 0; i < 13; i++) sheet.autoSizeColumn(i);
      wb.write(outputStream);
    } catch (Exception e) {
      throw new RuntimeException("Error generating Excel daily receipt report", e);
    }
  }

  private int buildDailyReceiptKpiBlock(
    XSSFWorkbook wb, Sheet sheet, int rowNum, DailyReceiptReportKPIs kpis
  ) {
    XSSFFont kpiFont = wb.createFont();
    kpiFont.setBold(true);
    kpiFont.setFontHeightInPoints((short) 10);
    kpiFont.setColor(PRIMARY);

    XSSFFont valFont = wb.createFont();
    valFont.setFontHeightInPoints((short) 10);

    String[][] kpiData = {
      { "Total Recepciones", String.valueOf(kpis.getTotalReceipts()) },
      { "Total Ordenes", String.valueOf(kpis.getTotalOrders()) },
      { "Completadas", String.valueOf(kpis.getTotalCompleted()) },
      { "Parciales", String.valueOf(kpis.getTotalPartial()) },
      { "% Cumplimiento", NumberFormatter.formatPercent(kpis.getOverallCompletenessPct()) },
      { "Items Recibidos", String.valueOf(kpis.getTotalItemsReceived()) },
      { "Items Esperados", String.valueOf(kpis.getTotalItemsExpected()) },
    };

    for (String[] pair : kpiData) {
      Row row = sheet.createRow(rowNum++);
      Cell labelCell = row.createCell(0);
      labelCell.setCellValue(pair[0]);
      XSSFCellStyle labelStyle = wb.createCellStyle();
      labelStyle.setFont(kpiFont);
      labelCell.setCellStyle(labelStyle);
      Cell valCell = row.createCell(1);
      valCell.setCellValue(pair[1]);
      XSSFCellStyle valStyle = wb.createCellStyle();
      valStyle.setFont(valFont);
      valCell.setCellStyle(valStyle);
    }

    if (kpis.getTopSupplier() != null && !kpis.getTopSupplier().isEmpty()) {
      Row row = sheet.createRow(rowNum++);
      Cell l = row.createCell(0);
      l.setCellValue("Proveedor Top");
      XSSFCellStyle ls = wb.createCellStyle(); ls.setFont(kpiFont); l.setCellStyle(ls);
      Cell v = row.createCell(1);
      v.setCellValue(kpis.getTopSupplier());
      XSSFCellStyle vs = wb.createCellStyle(); vs.setFont(valFont); v.setCellStyle(vs);
    }
    if (kpis.getTopProduct() != null && !kpis.getTopProduct().isEmpty()) {
      Row row = sheet.createRow(rowNum++);
      Cell l = row.createCell(0);
      l.setCellValue("Producto Top");
      XSSFCellStyle ls = wb.createCellStyle(); ls.setFont(kpiFont); l.setCellStyle(ls);
      Cell v = row.createCell(1);
      v.setCellValue(kpis.getTopProduct());
      XSSFCellStyle vs = wb.createCellStyle(); vs.setFont(valFont); v.setCellStyle(vs);
    }

    rowNum++;
    return rowNum;
  }

  private int buildDailyReceiptTable(
    XSSFWorkbook wb, Sheet sheet, int rowNum, List<DailyReceiptReportDTO> data
  ) {
    XSSFCellStyle headerStyle = createHeaderStyle(wb);

    String[] headers = {
      "# Recepción", "Hora", "OC", "Proveedor", "RIF", "Estado",
      "Items", "Cant. Recibida", "Cant. Ordenada", "Cant. Acum.", "% Acum.",
      "Recibido por", "Notas"
    };
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    XSSFCellStyle dataStyle = createDataStyle(wb);
    XSSFCellStyle altStyle = createAltDataStyle(wb, dataStyle);

    int count = 0;
    for (DailyReceiptReportDTO item : data) {
      Row row = sheet.createRow(rowNum++);
      CellStyle style = (count++ % 2 == 0) ? dataStyle : altStyle;
      setCellValue(row, 0, item.getReceiptNumber(), style);
      setCellValue(row, 1, DateUtils.formatDateTime(item.getReceivedAt()), style);
      setCellValue(row, 2, item.getPurchaseOrderNumber(), style);
      setCellValue(row, 3, item.getSupplierName(), style);
      setCellValue(row, 4, item.getSupplierRif() != null ? item.getSupplierRif() : "", style);
      setCellValue(row, 5, item.getStatus(), style);
      setCellValue(row, 6, String.valueOf(item.getItemCount()), style);
      setCellValue(row, 7, NumberFormatter.formatNumber(item.getTotalReceivedQty()), style);
      setCellValue(row, 8, NumberFormatter.formatNumber(item.getTotalOrderedQty()), style);
      setCellValue(row, 9, NumberFormatter.formatNumber(item.getCumulativeReceivedQty()), style);
      setCellValue(row, 10, NumberFormatter.formatPercent(
        item.getCumulativeCompletenessPct() == null
          ? 0.0
          : item.getCumulativeCompletenessPct().doubleValue()), style);
      setCellValue(row, 11, item.getReceivedBy(), style);
      setCellValue(row, 12, item.getNotes(), style);
    }

    sheet.createFreezePane(0, rowNum - data.size());
    return rowNum + 1;
  }

  private int buildHeader(
    XSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    String title,
    Map<String, String> metadata
  ) {
    XSSFCellStyle titleStyle = wb.createCellStyle();
    XSSFFont titleFont = wb.createFont();
    titleFont.setFontHeightInPoints((short) 16);
    titleFont.setBold(true);
    titleFont.setColor(PRIMARY);
    titleStyle.setFont(titleFont);
    titleStyle.setAlignment(HorizontalAlignment.LEFT);

    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(title);
    titleCell.setCellStyle(titleStyle);

    if (metadata != null) {
      XSSFCellStyle metaStyle = wb.createCellStyle();
      XSSFFont metaFont = wb.createFont();
      metaFont.setFontHeightInPoints((short) 9);
      metaFont.setColor(MUTED_TEXT);
      metaStyle.setFont(metaFont);

      StringBuilder metaText = new StringBuilder();
      for (var entry : metadata.entrySet()) {
        if (metaText.length() > 0) metaText.append(" | ");
        metaText.append(entry.getKey()).append(": ").append(entry.getValue());
      }
      Row metaRow = sheet.createRow(rowNum++);
      Cell metaCell = metaRow.createCell(0);
      metaCell.setCellValue(metaText.toString());
      metaCell.setCellStyle(metaStyle);
    }

    rowNum++;
    return rowNum;
  }

  private int buildStockTable(
    XSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    List<StockReportDTO> data
  ) {
    XSSFCellStyle headerStyle = createHeaderStyle(wb);

    String[] headers = {
      "Product", "SKU", "Code", "Current Stock", "Pending Stock",
      "Reorder Point", "Status", "Category",
    };
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    XSSFCellStyle dataStyle = createDataStyle(wb);
    XSSFCellStyle altStyle = createAltDataStyle(wb, dataStyle);

    int count = 0;
    for (StockReportDTO item : data) {
      Row row = sheet.createRow(rowNum++);
      CellStyle style = (count++ % 2 == 0) ? dataStyle : altStyle;
      setCellValue(row, 0, item.getProductName(), style);
      setCellValue(row, 1, item.getSku(), style);
      setCellValue(row, 2, item.getInternalCode(), style);
      setCellValue(row, 3, NumberFormatter.formatNumber(item.getCurrentStock()), style);
      setCellValue(row, 4, NumberFormatter.formatNumber(item.getPendingStock()), style);
      setCellValue(row, 5, NumberFormatter.formatNumber(item.getReorderPoint()), style);
      setCellValue(row, 6, item.getStatus(), style);
      setCellValue(row, 7, item.getCategory(), style);
    }

    sheet.createFreezePane(0, rowNum - data.size());
    return rowNum + 1;
  }

  private int buildMovementTable(
    XSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    List<MovementReportDTO> data
  ) {
    XSSFCellStyle headerStyle = createHeaderStyle(wb);

    String[] headers = {
      "Date", "Type", "Product", "SKU", "Quantity", "Warehouse", "User", "Reference",
    };
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    XSSFCellStyle dataStyle = createDataStyle(wb);
    XSSFCellStyle altStyle = createAltDataStyle(wb, dataStyle);

    int count = 0;
    for (MovementReportDTO item : data) {
      Row row = sheet.createRow(rowNum++);
      CellStyle style = (count++ % 2 == 0) ? dataStyle : altStyle;
      setCellValue(row, 0, DateUtils.formatDate(item.getMovementDate()), style);
      setCellValue(row, 1, item.getMovementType(), style);
      setCellValue(row, 2, item.getProductName(), style);
      setCellValue(row, 3, item.getSku(), style);
      setCellValue(row, 4, NumberFormatter.formatNumber(item.getQuantity()), style);
      setCellValue(row, 5, item.getWarehouseName(), style);
      setCellValue(row, 6, item.getUserName(), style);
      setCellValue(row, 7, item.getReference(), style);
    }

    sheet.createFreezePane(0, rowNum - data.size());
    return rowNum + 1;
  }

  private int buildAlertTable(
    XSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    List<AlertReportDTO> data
  ) {
    XSSFCellStyle headerStyle = createHeaderStyle(wb);

    String[] headers = {
      "Product", "SKU", "Stock", "Reorder Point", "Alert Type", "Severity", "Action",
    };
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    XSSFCellStyle criticalStyle = createAlertStyle(wb, new Color(254, 242, 242));
    XSSFCellStyle warningStyle = createAlertStyle(wb, new Color(255, 248, 235));
    XSSFCellStyle infoStyle = createAlertStyle(wb, new Color(240, 253, 244));

    for (AlertReportDTO item : data) {
      Row row = sheet.createRow(rowNum++);
      CellStyle style = switch (item.getSeverity()) {
        case "CRITICAL" -> criticalStyle;
        case "HIGH" -> warningStyle;
        default -> infoStyle;
      };
      setCellValue(row, 0, item.getProductName(), style);
      setCellValue(row, 1, item.getSku(), style);
      setCellValue(row, 2, NumberFormatter.formatNumber(item.getCurrentStock()), style);
      setCellValue(row, 3, NumberFormatter.formatNumber(item.getReorderPoint()), style);
      setCellValue(row, 4, item.getAlertType(), style);
      setCellValue(row, 5, item.getSeverity(), style);
      setCellValue(row, 6, item.getRecommendedAction(), style);
    }

    return rowNum + 1;
  }

  private int buildWarehouseSummaryTable(
    XSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    List<WarehouseAnalysisDTO> data
  ) {
    XSSFCellStyle headerStyle = createHeaderStyle(wb);

    String[] headers = {
      "Warehouse", "Products", "Total Values", "Utilization", "Critical Stock", "Low Stock",
    };
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    XSSFCellStyle dataStyle = createDataStyle(wb);
    XSSFCellStyle altStyle = createAltDataStyle(wb, dataStyle);

    int count = 0;
    for (WarehouseAnalysisDTO item : data) {
      Row row = sheet.createRow(rowNum++);
      CellStyle style = (count++ % 2 == 0) ? dataStyle : altStyle;
      setCellValue(row, 0, item.getWarehouseName(), style);
      setCellValue(row, 1, NumberFormatter.formatNumber(item.getProductCount()), style);
      setCellValue(row, 2, NumberFormatter.formatCurrency(item.getTotalValue()), style);
      setCellValue(row, 3, NumberFormatter.formatPercent(item.getCapacityUtilization()), style);
      setCellValue(row, 4, NumberFormatter.formatNumber(item.getCriticalProducts()), style);
      setCellValue(row, 5, NumberFormatter.formatNumber(item.getLowStockProducts()), style);
    }

    return rowNum + 1;
  }

  private int buildWarehouseDetailSheet(
    XSSFWorkbook wb,
    Sheet sheet,
    WarehouseAnalysisDTO wh
  ) {
    int rowNum = 0;

    XSSFCellStyle titleStyle = wb.createCellStyle();
    XSSFFont titleFont = wb.createFont();
    titleFont.setFontHeightInPoints((short) 14);
    titleFont.setBold(true);
    titleFont.setColor(PRIMARY);
    titleStyle.setFont(titleFont);

    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("Analysis for: " + wh.getWarehouseName());
    titleCell.setCellStyle(titleStyle);
    rowNum++;

    XSSFCellStyle labelStyle = wb.createCellStyle();
    XSSFFont labelFont = wb.createFont();
    labelFont.setBold(true);
    labelFont.setFontHeightInPoints((short) 10);
    labelStyle.setFont(labelFont);

    String[][] infoData = {
      { "Products", NumberFormatter.formatNumber(wh.getProductCount()) },
      { "Total Value", NumberFormatter.formatCurrency(wh.getTotalValue()) },
      { "Utilization", NumberFormatter.formatPercent(wh.getCapacityUtilization()) },
      { "Critical Stock", NumberFormatter.formatNumber(wh.getCriticalProducts()) },
      { "Low Stock", NumberFormatter.formatNumber(wh.getLowStockProducts()) },
    };

    for (String[] row : infoData) {
      Row r = sheet.createRow(rowNum++);
      Cell labelCell = r.createCell(0);
      labelCell.setCellValue(row[0]);
      labelCell.setCellStyle(labelStyle);
      Cell valueCell = r.createCell(1);
      valueCell.setCellValue(row[1]);
    }

    rowNum++;

    if (wh.getTopByQuantity() != null && !wh.getTopByQuantity().isEmpty()) {
      XSSFCellStyle sectionStyle = wb.createCellStyle();
      XSSFFont sectionFont = wb.createFont();
      sectionFont.setBold(true);
      sectionFont.setFontHeightInPoints((short) 11);
      sectionStyle.setFont(sectionFont);

      Row sectRow = sheet.createRow(rowNum++);
      Cell sectCell = sectRow.createCell(0);
      sectCell.setCellValue("Top 10 Products by Qty");
      sectCell.setCellStyle(sectionStyle);

      XSSFCellStyle hStyle = createHeaderStyle(wb);
      Row hRow = sheet.createRow(rowNum++);
      Cell hc1 = hRow.createCell(0);
      hc1.setCellValue("Producto");
      hc1.setCellStyle(hStyle);
      Cell hc2 = hRow.createCell(1);
      hc2.setCellValue("Stock");
      hc2.setCellStyle(hStyle);

      XSSFCellStyle dStyle = createDataStyle(wb);
      for (TopProductDTO top : wh.getTopByQuantity()) {
        Row r = sheet.createRow(rowNum++);
        setCellValue(r, 0, top.getProductName(), dStyle);
        setCellValue(r, 1, NumberFormatter.formatNumber(top.getValue()), dStyle);
      }
    }

    if (wh.getCategoryDistribution() != null && !wh.getCategoryDistribution().isEmpty()) {
      rowNum++;
      XSSFCellStyle sectionStyle = wb.createCellStyle();
      XSSFFont sectionFont = wb.createFont();
      sectionFont.setBold(true);
      sectionFont.setFontHeightInPoints((short) 11);
      sectionStyle.setFont(sectionFont);

      Row sectRow = sheet.createRow(rowNum++);
      Cell sectCell = sectRow.createCell(0);
      sectCell.setCellValue("Distribution by category");
      sectCell.setCellStyle(sectionStyle);

      XSSFCellStyle hStyle = createHeaderStyle(wb);
      Row hRow = sheet.createRow(rowNum++);
      Cell hc1 = hRow.createCell(0);
      hc1.setCellValue("Category");
      hc1.setCellStyle(hStyle);
      Cell hc2 = hRow.createCell(1);
      hc2.setCellValue("Quantity");
      hc2.setCellStyle(hStyle);
      Cell hc3 = hRow.createCell(2);
      hc3.setCellValue("%");
      hc3.setCellStyle(hStyle);

      XSSFCellStyle dStyle = createDataStyle(wb);
      for (CategoryDistributionDTO cat : wh.getCategoryDistribution()) {
        Row r = sheet.createRow(rowNum++);
        setCellValue(r, 0, cat.getCategoryName(), dStyle);
        setCellValue(r, 1, NumberFormatter.formatNumber(cat.getQuantity()), dStyle);
        setCellValue(r, 2, NumberFormatter.formatPercent(cat.getPercentage()), dStyle);
      }
    }

    sheet.createFreezePane(0, 1);
    return rowNum;
  }

  private XSSFCellStyle createHeaderStyle(XSSFWorkbook wb) {
    XSSFCellStyle style = wb.createCellStyle();
    XSSFFont font = wb.createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 10);
    font.setColor(WHITE);
    style.setFont(font);
    style.setFillForegroundColor(PRIMARY);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    setBorders(style);
    return style;
  }

  private XSSFCellStyle createDataStyle(XSSFWorkbook wb) {
    XSSFCellStyle style = wb.createCellStyle();
    XSSFFont font = wb.createFont();
    font.setFontHeightInPoints((short) 9);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.LEFT);
    setBorders(style);
    return style;
  }

  private XSSFCellStyle createAltDataStyle(XSSFWorkbook wb, XSSFCellStyle base) {
    XSSFCellStyle style = wb.createCellStyle();
    style.cloneStyleFrom(base);
    style.setFillForegroundColor(ALT_ROW);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private XSSFCellStyle createAlertStyle(XSSFWorkbook wb, Color bgColor) {
    XSSFCellStyle style = wb.createCellStyle();
    XSSFFont font = wb.createFont();
    font.setFontHeightInPoints((short) 9);
    style.setFont(font);
    style.setFillForegroundColor(new XSSFColor(bgColor, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    setBorders(style);
    return style;
  }

  private void setBorders(CellStyle style) {
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
  }

  private void setCellValue(Row row, int col, String value, CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(value != null ? value : "");
    cell.setCellStyle(style);
  }
}
