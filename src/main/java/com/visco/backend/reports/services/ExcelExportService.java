package com.visco.backend.reports.services;

import com.visco.backend.reports.models.dtos.AlertReportDTO;
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
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExcelExportService {

  private static final XSSFColor PRIMARY = new XSSFColor(
    new Color(92, 18, 18),
    null
  );
  private static final XSSFColor HEADER_BG = new XSSFColor(
    new Color(229, 229, 229),
    null
  );
  private static final XSSFColor ALT_ROW = new XSSFColor(
    new Color(245, 245, 247),
    null
  );

  public void exportStockReportToExcel(
    List<StockReportDTO> data,
    String title,
    Map<String, String> metadata,
    OutputStream outputStream
  ) {
    try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
      Sheet sheet = wb.createSheet("Stock");
      int rowNum = 0;
      rowNum = buildHeader(wb, sheet, rowNum, title, metadata);
      rowNum = buildStockTable(wb, sheet, rowNum, data);

      // Usando setColumnWidth porque autoSizeColumn no funciona bien con SXSSF (filas en disco)
      sheet.setColumnWidth(0, 5000);
      sheet.setColumnWidth(1, 4000);
      sheet.setColumnWidth(2, 3000);
      sheet.setColumnWidth(3, 3000);
      sheet.setColumnWidth(4, 3000);
      sheet.setColumnWidth(5, 3000);
      sheet.setColumnWidth(6, 3000);
      sheet.setColumnWidth(7, 3000);

      wb.write(outputStream);
      wb.dispose(); // Importante: elimina los archivos temporales del disco
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
    try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
      Sheet sheet = wb.createSheet("Movements");
      int rowNum = 0;
      rowNum = buildHeader(wb, sheet, rowNum, title, metadata);
      rowNum = buildMovementTable(wb, sheet, rowNum, data);

      sheet.setColumnWidth(0, 4000);
      sheet.setColumnWidth(1, 3000);
      sheet.setColumnWidth(2, 5000);
      sheet.setColumnWidth(3, 3000);
      sheet.setColumnWidth(4, 3000);
      sheet.setColumnWidth(5, 4000);
      sheet.setColumnWidth(6, 3000);
      sheet.setColumnWidth(7, 3000);

      wb.write(outputStream);
      wb.dispose();
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
    try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
      Sheet sheet = wb.createSheet("Warnings");
      int rowNum = 0;
      rowNum = buildHeader(wb, sheet, rowNum, title, metadata);
      rowNum = buildAlertTable(wb, sheet, rowNum, data);

      sheet.setColumnWidth(0, 5000);
      sheet.setColumnWidth(1, 3000);
      sheet.setColumnWidth(2, 3000);
      sheet.setColumnWidth(3, 3000);
      sheet.setColumnWidth(4, 3000);
      sheet.setColumnWidth(5, 4000);

      wb.write(outputStream);
      wb.dispose();
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
    try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
      Sheet summarySheet = wb.createSheet("Summary");
      buildHeader(wb, summarySheet, 0, title, metadata);
      buildWarehouseSummaryTable(wb, summarySheet, 1, data);

      for (WarehouseAnalysisDTO wh : data) {
        Sheet whSheet = wb.createSheet(wh.getWarehouseName());
        buildWarehouseDetailSheet(wb, whSheet, wh);
      }

      wb.write(outputStream);
      wb.dispose();
    } catch (Exception e) {
      throw new RuntimeException(
        "Error generating Excel warehouse analysis report",
        e
      );
    }
  }

  private int buildHeader(
    SXSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    String title,
    Map<String, String> metadata
  ) {
    // Obtenemos el XSSFWorkbook base para poder usar estilos y fuentes XSSF
    XSSFCellStyle titleStyle = (XSSFCellStyle) wb
      .getXSSFWorkbook()
      .createCellStyle();
    XSSFFont titleFont = wb.getXSSFWorkbook().createFont();
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
      XSSFCellStyle metaStyle = (XSSFCellStyle) wb
        .getXSSFWorkbook()
        .createCellStyle();
      XSSFFont metaFont = wb.getXSSFWorkbook().createFont();
      metaFont.setFontHeightInPoints((short) 9);
      metaFont.setColor(new XSSFColor(new Color(107, 114, 128), null));
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
    SXSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    List<StockReportDTO> data
  ) {
    XSSFCellStyle headerStyle = createHeaderStyle(wb);

    String[] headers = {
      "Product",
      "SKU",
      "Code",
      "Current Stock",
      "Pending Stock",
      "Reorder Point",
      "Status",
      "Category",
    };
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    XSSFCellStyle dataStyle = createDataStyle(wb);
    XSSFCellStyle altStyle = createAltDataStyle(wb);

    int count = 0;
    for (StockReportDTO item : data) {
      Row row = sheet.createRow(rowNum++);
      CellStyle style = (count++ % 2 == 0) ? dataStyle : altStyle;
      setCellValue(row, 0, item.getProductName(), style);
      setCellValue(row, 1, item.getSku(), style);
      setCellValue(row, 2, item.getInternalCode(), style);
      setCellValue(
        row,
        3,
        NumberFormatter.formatNumber(item.getCurrentStock()),
        style
      );
      setCellValue(
        row,
        4,
        NumberFormatter.formatNumber(item.getPendingStock()),
        style
      );
      setCellValue(
        row,
        5,
        NumberFormatter.formatNumber(item.getReorderPoint()),
        style
      );
      setCellValue(row, 6, item.getStatus(), style);
      setCellValue(row, 7, item.getCategory(), style);
    }

    sheet.createFreezePane(0, rowNum - data.size());
    return rowNum + 1;
  }

  private int buildMovementTable(
    SXSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    List<MovementReportDTO> data
  ) {
    XSSFCellStyle headerStyle = createHeaderStyle(wb);

    String[] headers = {
      "Date",
      "Type",
      "Product",
      "SKU",
      "Quantity",
      "Warehouse",
      "User",
      "Reference",
    };
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    XSSFCellStyle dataStyle = createDataStyle(wb);
    XSSFCellStyle altStyle = createAltDataStyle(wb);

    int count = 0;
    for (MovementReportDTO item : data) {
      Row row = sheet.createRow(rowNum++);
      CellStyle style = (count++ % 2 == 0) ? dataStyle : altStyle;
      setCellValue(row, 0, DateUtils.formatDate(item.getMovementDate()), style);
      setCellValue(row, 1, item.getMovementType(), style);
      setCellValue(row, 2, item.getProductName(), style);
      setCellValue(row, 3, item.getSku(), style);
      setCellValue(
        row,
        4,
        NumberFormatter.formatNumber(item.getQuantity()),
        style
      );
      setCellValue(row, 5, item.getWarehouseName(), style);
      setCellValue(row, 6, item.getUserName(), style);
      setCellValue(row, 7, item.getReference(), style);
    }

    sheet.createFreezePane(0, rowNum - data.size());
    return rowNum + 1;
  }

  private int buildAlertTable(
    SXSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    List<AlertReportDTO> data
  ) {
    XSSFCellStyle headerStyle = createHeaderStyle(wb);

    String[] headers = {
      "Product",
      "SKU",
      "Stock",
      "Reorder Point",
      "Alert Type",
      "Severity",
      "Action",
    };
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    XSSFCellStyle criticalStyle = createAlertStyle(
      wb,
      new Color(254, 242, 242)
    );
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
      setCellValue(
        row,
        2,
        NumberFormatter.formatNumber(item.getCurrentStock()),
        style
      );
      setCellValue(
        row,
        3,
        NumberFormatter.formatNumber(item.getReorderPoint()),
        style
      );
      setCellValue(row, 4, item.getAlertType(), style);
      setCellValue(row, 5, item.getSeverity(), style);
      setCellValue(row, 6, item.getRecommendedAction(), style);
    }

    return rowNum + 1;
  }

  private int buildWarehouseSummaryTable(
    SXSSFWorkbook wb,
    Sheet sheet,
    int rowNum,
    List<WarehouseAnalysisDTO> data
  ) {
    XSSFCellStyle headerStyle = createHeaderStyle(wb);

    String[] headers = {
      "Warehouse",
      "Products",
      "Total Values",
      "Utilization",
      "Critical Stock",
      "Low Stock",
    };
    Row headerRow = sheet.createRow(rowNum++);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    XSSFCellStyle dataStyle = createDataStyle(wb);
    XSSFCellStyle altStyle = createAltDataStyle(wb);

    int count = 0;
    for (WarehouseAnalysisDTO item : data) {
      Row row = sheet.createRow(rowNum++);
      CellStyle style = (count++ % 2 == 0) ? dataStyle : altStyle;
      setCellValue(row, 0, item.getWarehouseName(), style);
      setCellValue(
        row,
        1,
        NumberFormatter.formatNumber(item.getProductCount()),
        style
      );
      setCellValue(
        row,
        2,
        NumberFormatter.formatCurrency(item.getTotalValue()),
        style
      );
      setCellValue(
        row,
        3,
        NumberFormatter.formatPercent(item.getCapacityUtilization()),
        style
      );
      setCellValue(
        row,
        4,
        NumberFormatter.formatNumber(item.getCriticalProducts()),
        style
      );
      setCellValue(
        row,
        5,
        NumberFormatter.formatNumber(item.getLowStockProducts()),
        style
      );
    }

    sheet.setColumnWidth(0, 4000);
    sheet.setColumnWidth(1, 3000);
    sheet.setColumnWidth(2, 3000);
    sheet.setColumnWidth(3, 3000);
    sheet.setColumnWidth(4, 3000);
    sheet.setColumnWidth(5, 3000);
    return rowNum + 1;
  }

  private int buildWarehouseDetailSheet(
    SXSSFWorkbook wb,
    Sheet sheet,
    WarehouseAnalysisDTO wh
  ) {
    int rowNum = 0;

    XSSFCellStyle titleStyle = (XSSFCellStyle) wb
      .getXSSFWorkbook()
      .createCellStyle();
    XSSFFont titleFont = wb.getXSSFWorkbook().createFont();
    titleFont.setFontHeightInPoints((short) 14);
    titleFont.setBold(true);
    titleFont.setColor(PRIMARY);
    titleStyle.setFont(titleFont);

    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue("Analysis for: " + wh.getWarehouseName());
    titleCell.setCellStyle(titleStyle);
    rowNum++;

    XSSFCellStyle labelStyle = (XSSFCellStyle) wb
      .getXSSFWorkbook()
      .createCellStyle();
    XSSFFont labelFont = wb.getXSSFWorkbook().createFont();
    labelFont.setBold(true);
    labelFont.setFontHeightInPoints((short) 10);
    labelStyle.setFont(labelFont);

    String[][] infoData = {
      { "Products", NumberFormatter.formatNumber(wh.getProductCount()) },
      { "Total Value", NumberFormatter.formatCurrency(wh.getTotalValue()) },
      {
        "Utilization",
        NumberFormatter.formatPercent(wh.getCapacityUtilization()),
      },
      {
        "Critical Stock",
        NumberFormatter.formatNumber(wh.getCriticalProducts()),
      },
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
      XSSFCellStyle sectionStyle = (XSSFCellStyle) wb
        .getXSSFWorkbook()
        .createCellStyle();
      XSSFFont sectionFont = wb.getXSSFWorkbook().createFont();
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
        setCellValue(
          r,
          1,
          NumberFormatter.formatNumber(top.getValue()),
          dStyle
        );
      }
    }

    if (
      wh.getCategoryDistribution() != null &&
      !wh.getCategoryDistribution().isEmpty()
    ) {
      rowNum++;
      XSSFCellStyle sectionStyle = (XSSFCellStyle) wb
        .getXSSFWorkbook()
        .createCellStyle();
      XSSFFont sectionFont = wb.getXSSFWorkbook().createFont();
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
        setCellValue(
          r,
          1,
          NumberFormatter.formatNumber(cat.getQuantity()),
          dStyle
        );
        setCellValue(
          r,
          2,
          NumberFormatter.formatPercent(cat.getPercentage()),
          dStyle
        );
      }
    }

    sheet.setColumnWidth(0, 5000);
    sheet.setColumnWidth(1, 4000);
    sheet.setColumnWidth(2, 3000);
    sheet.createFreezePane(0, 1);
    return rowNum;
  }

  private XSSFCellStyle createHeaderStyle(SXSSFWorkbook wb) {
    XSSFCellStyle style = (XSSFCellStyle) wb
      .getXSSFWorkbook()
      .createCellStyle();
    XSSFFont font = wb.getXSSFWorkbook().createFont();
    font.setBold(true);
    font.setFontHeightInPoints((short) 10);
    font.setColor(new XSSFColor(new Color(255, 255, 255), null));
    style.setFont(font);
    style.setFillForegroundColor(PRIMARY);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    setBorders(style);
    return style;
  }

  private XSSFCellStyle createDataStyle(SXSSFWorkbook wb) {
    XSSFCellStyle style = (XSSFCellStyle) wb
      .getXSSFWorkbook()
      .createCellStyle();
    XSSFFont font = wb.getXSSFWorkbook().createFont();
    font.setFontHeightInPoints((short) 9);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.LEFT);
    setBorders(style);
    return style;
  }

  private XSSFCellStyle createAltDataStyle(SXSSFWorkbook wb) {
    XSSFCellStyle style = createDataStyle(wb);
    style.setFillForegroundColor(ALT_ROW);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return style;
  }

  private XSSFCellStyle createAlertStyle(SXSSFWorkbook wb, Color bgColor) {
    XSSFCellStyle style = (XSSFCellStyle) wb
      .getXSSFWorkbook()
      .createCellStyle();
    XSSFFont font = wb.getXSSFWorkbook().createFont();
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
