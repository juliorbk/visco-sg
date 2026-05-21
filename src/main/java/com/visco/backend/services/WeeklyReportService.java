package com.visco.backend.services;

import com.visco.backend.models.dtos.CriticalInventoryItemDTO;
import com.visco.backend.models.dtos.KpiStatsDTO;
import com.visco.backend.models.dtos.RecentOrderDTO;
import com.visco.backend.repositories.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyReportService {

  private final StatsService statsService;
  private final UserRepository userRepository;
  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String senderEmail;

  @Value("${report.recipients:}")
  private String reportRecipients;

  @Value("${report.max-critical-items:50}")
  private int maxCriticalItems;

  @Value("${report.max-recent-orders:50}")
  private int maxRecentOrders;

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(
    "dd/MM/yyyy"
  );

  @Scheduled(cron = "0 0 8 * * MON")
  @Transactional(readOnly = true)
  public void sendWeeklyReport() {
    log.info("Generating weekly report...");

    List<String> recipients = resolveRecipients();
    if (recipients.isEmpty()) {
      log.warn(
        "No recipients configured for weekly report. Add report.recipients to .env"
      );
      return;
    }

    try {
      KpiStatsDTO kpis = statsService.getKpis();
      List<RecentOrderDTO> recentOrders = statsService.getRecentOrders(
        maxRecentOrders
      );
      List<CriticalInventoryItemDTO> critical =
        statsService.getCriticalInventory();

      // Truncate critical items if needed
      if (critical.size() > maxCriticalItems) {
        log.warn(
          "Critical inventory items {} exceed max {}. Truncating.",
          critical.size(),
          maxCriticalItems
        );
        critical = critical.subList(0, maxCriticalItems);
      }

      byte[] excelBytes = buildExcelFile(kpis, recentOrders, critical);
      LocalDateTime now = LocalDateTime.now();
      String weekStart = now
        .minusDays(now.getDayOfWeek().getValue() - 1)
        .format(DATE_FMT);
      String weekEnd = now
        .minusDays(now.getDayOfWeek().getValue() - 7)
        .format(DATE_FMT);

      String subject = String.format(
        "📊 Reporte Semanal Visco Orinoco — %s (Semana %s a %s)",
        now.format(DATE_FMT),
        weekStart,
        weekEnd
      );

      for (String recipient : recipients) {
        sendEmailWithAttachment(recipient.trim(), subject, excelBytes);
      }

      log.info("Weekly report sent to {} recipient(s)", recipients.size());
    } catch (Exception e) {
      log.error(
        "Error generating/sending weekly report: {}",
        e.getMessage(),
        e
      );
    }
  }

  @Transactional(readOnly = true)
  public void sendReportNow() {
    sendWeeklyReport();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private List<String> resolveRecipients() {
    if (reportRecipients != null && !reportRecipients.isBlank()) {
      return List.of(reportRecipients.split(","));
    }
    return userRepository.findActiveAdminAndManagerEmails();
  }

  private byte[] buildExcelFile(
    KpiStatsDTO kpis,
    List<RecentOrderDTO> orders,
    List<CriticalInventoryItemDTO> critical
  ) throws IOException {
    try (
      XSSFWorkbook wb = new XSSFWorkbook();
      ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ) {
      // FIXED: XSFSheet changed to XSSFSheet
      Sheet sheet = wb.createSheet("Reporte Semanal");
      int rowNum = 0;

      rowNum = buildHeader(sheet, rowNum, kpis);
      rowNum = buildKpiTable(sheet, rowNum, kpis);

      if (!critical.isEmpty()) {
        rowNum = buildCriticalInventorySection(sheet, rowNum, critical);
      } else {
        rowNum = buildNoIssuesRow(
          sheet,
          rowNum,
          "✅ All products above reorder point"
        );
      }

      if (!orders.isEmpty()) {
        rowNum = buildOrdersSection(sheet, rowNum, orders);
      }

      rowNum = buildFooter(sheet, rowNum);

      // Auto-size columns
      sheet.setColumnWidth(0, 7000); // Widened slightly to fit names better
      sheet.setColumnWidth(1, 4000);
      sheet.setColumnWidth(2, 4000);
      sheet.setColumnWidth(3, 4000);
      sheet.setColumnWidth(4, 4000);

      wb.write(baos);
      return baos.toByteArray();
    }
  }

  private int buildHeader(Sheet sheet, int rowNum, KpiStatsDTO kpis) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
    LocalDateTime weekEnd = weekStart.plusDays(6);

    CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
    titleStyle.setFont(createFont(sheet.getWorkbook(), 16, true, "5C1212"));
    titleStyle.setAlignment(HorizontalAlignment.CENTER);

    CellStyle subtitleStyle = sheet.getWorkbook().createCellStyle();
    subtitleStyle.setFont(createFont(sheet.getWorkbook(), 11, false, "666666"));
    subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

    Row r1 = sheet.createRow(rowNum++);
    Cell c1 = r1.createCell(0);
    c1.setCellValue("📊 REPORTE SEMANAL VISCO ORINOCO");
    c1.setCellStyle(titleStyle);
    sheet.addMergedRegion(
      new CellRangeAddress(r1.getRowNum(), r1.getRowNum(), 0, 4)
    );

    Row r2 = sheet.createRow(rowNum++);
    Cell c2 = r2.createCell(0);
    c2.setCellValue(
      String.format(
        "Semana del %s al %s",
        weekStart.format(DATE_FMT),
        weekEnd.format(DATE_FMT)
      )
    );
    c2.setCellStyle(subtitleStyle);
    sheet.addMergedRegion(
      new CellRangeAddress(r2.getRowNum(), r2.getRowNum(), 0, 4)
    );

    Row r3 = sheet.createRow(rowNum++);
    Cell c3 = r3.createCell(0);
    c3.setCellValue(
      "Generado: " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    );
    c3.setCellStyle(subtitleStyle);
    sheet.addMergedRegion(
      new CellRangeAddress(r3.getRowNum(), r3.getRowNum(), 0, 4)
    );

    rowNum++; // Blank row
    return rowNum;
  }

  private int buildKpiTable(Sheet sheet, int rowNum, KpiStatsDTO kpis) {
    // FIXED: Proper background color application
    XSSFCellStyle headerStyle = (XSSFCellStyle) sheet
      .getWorkbook()
      .createCellStyle();
    headerStyle.setFont(createFont(sheet.getWorkbook(), 11, true, "FFFFFF"));
    headerStyle.setFillForegroundColor(
      new XSSFColor(new Color(92, 18, 18), null)
    ); // #5C1212
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    headerStyle.setAlignment(HorizontalAlignment.CENTER);
    setBorder(headerStyle);

    CellStyle dataStyle = sheet.getWorkbook().createCellStyle();
    dataStyle.setAlignment(HorizontalAlignment.CENTER);
    setBorder(dataStyle);

    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {
      "Órdenes Totales",
      "Unidades en Stock",
      "Gasto Mensual",
      "Cumplimiento",
      "Estado",
    };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    Row dataRow = sheet.createRow(rowNum++);
    dataRow.createCell(0).setCellValue(kpis.getTotalOrders());
    dataRow
      .createCell(1)
      .setCellValue(
        kpis.getTotalInventoryUnits() != null
          ? kpis.getTotalInventoryUnits().intValue()
          : 0
      );
    dataRow.createCell(2).setCellValue(formatMoney(kpis.getMonthlySpend()));
    dataRow
      .createCell(3)
      .setCellValue(String.format("%.1f%%", kpis.getFulfillmentRate()));
    dataRow
      .createCell(4)
      .setCellValue(
        kpis.getFulfillmentRate() >= 90 ? "✅ Bueno" : "⚠️ Revisar"
      );

    for (int i = 0; i < headers.length; i++) {
      dataRow.getCell(i).setCellStyle(dataStyle);
    }

    rowNum++; // Blank row
    return rowNum;
  }

  private int buildCriticalInventorySection(
    Sheet sheet,
    int rowNum,
    List<CriticalInventoryItemDTO> items
  ) {
    CellStyle sectionTitleStyle = sheet.getWorkbook().createCellStyle();
    sectionTitleStyle.setFont(
      createFont(sheet.getWorkbook(), 12, true, "DC2626")
    );

    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(
      "⚠️ INVENTARIO CRÍTICO (" + items.size() + " productos)"
    );
    titleCell.setCellStyle(sectionTitleStyle);

    CellStyle headerStyle = createHeaderStyle(
      sheet.getWorkbook(),
      new Color(229, 229, 229)
    ); // #E5E5E5
    CellStyle criticalStyle = createAlertStyle(
      sheet.getWorkbook(),
      new Color(254, 242, 242)
    ); // #FEF2F2
    CellStyle warningStyle = createAlertStyle(
      sheet.getWorkbook(),
      new Color(255, 248, 235)
    ); // #FFF8EB

    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = {
      "Producto",
      "SKU",
      "Stock Actual",
      "Punto Reorden",
      "Severidad",
    };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (CriticalInventoryItemDTO item : items) {
      Row row = sheet.createRow(rowNum++);
      boolean isCritical = "CRITICAL".equals(item.getSeverity());
      CellStyle rowStyle = isCritical ? criticalStyle : warningStyle;

      row.createCell(0).setCellValue(item.getProductName());
      row.createCell(1).setCellValue(item.getSku());
      row.createCell(2).setCellValue(item.getCurrentStock().intValue());
      row.createCell(3).setCellValue(item.getReorderPoint().intValue());
      row.createCell(4).setCellValue(isCritical ? "🔴 CRÍTICO" : "🟡 ALERTA");

      for (int i = 0; i < 5; i++) {
        row.getCell(i).setCellStyle(rowStyle);
      }
    }

    rowNum++; // Blank row
    return rowNum;
  }

  private int buildOrdersSection(
    Sheet sheet,
    int rowNum,
    List<RecentOrderDTO> orders
  ) {
    CellStyle sectionTitleStyle = sheet.getWorkbook().createCellStyle();
    sectionTitleStyle.setFont(
      createFont(sheet.getWorkbook(), 12, true, "185FA5")
    );

    Row titleRow = sheet.createRow(rowNum++);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(
      "📦 ÓRDENES RECIENTES (" + orders.size() + " últimas)"
    );
    titleCell.setCellStyle(sectionTitleStyle);

    CellStyle headerStyle = createHeaderStyle(
      sheet.getWorkbook(),
      new Color(229, 229, 229)
    );
    CellStyle dataStyle = sheet.getWorkbook().createCellStyle();
    setBorder(dataStyle);

    Row headerRow = sheet.createRow(rowNum++);
    String[] headers = { "N° Orden", "Proveedor", "Fecha", "Monto", "Estado" };
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (RecentOrderDTO order : orders) {
      Row row = sheet.createRow(rowNum++);
      row.createCell(0).setCellValue(order.getOrderNumber());
      row.createCell(1).setCellValue(order.getSupplierName());
      row
        .createCell(2)
        .setCellValue(
          order.getCreatedAt() != null
            ? order.getCreatedAt().format(DATE_FMT)
            : "—"
        );
      row.createCell(3).setCellValue(formatMoney(order.getAmount()));
      row.createCell(4).setCellValue(order.getStatus().name());

      for (int i = 0; i < 5; i++) {
        row.getCell(i).setCellStyle(dataStyle);
      }
    }

    rowNum++; // Blank row
    return rowNum;
  }

  private int buildNoIssuesRow(Sheet sheet, int rowNum, String message) {
    XSSFCellStyle style = (XSSFCellStyle) sheet.getWorkbook().createCellStyle();
    style.setFont(createFont(sheet.getWorkbook(), 11, false, "15803D"));
    style.setFillForegroundColor(new XSSFColor(new Color(240, 253, 244), null)); // #F0FDF4
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);

    Row row = sheet.createRow(rowNum++);
    Cell cell = row.createCell(0);
    cell.setCellValue(message);
    cell.setCellStyle(style);
    sheet.addMergedRegion(
      new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, 4)
    );

    rowNum++;
    return rowNum;
  }

  private int buildFooter(Sheet sheet, int rowNum) {
    rowNum += 2; // Extra space
    CellStyle footerStyle = sheet.getWorkbook().createCellStyle();
    footerStyle.setFont(createFont(sheet.getWorkbook(), 9, false, "9CA3AF"));
    footerStyle.setAlignment(HorizontalAlignment.CENTER);

    Row footerRow = sheet.createRow(rowNum);
    Cell footerCell = footerRow.createCell(0);
    footerCell.setCellValue(
      "© " +
        LocalDateTime.now().getYear() +
        " Visco Orinoco — Sistema de Gestión Empresarial"
    );
    footerCell.setCellStyle(footerStyle);
    sheet.addMergedRegion(
      new CellRangeAddress(footerRow.getRowNum(), footerRow.getRowNum(), 0, 4)
    );

    return rowNum + 1;
  }

  private void sendEmailWithAttachment(
    String to,
    String subject,
    byte[] excelBytes
  ) {
    try {
      MimeMessage msg = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
      helper.setFrom(senderEmail);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(
        "Adjunto: Reporte Semanal en Excel\n\nVer archivo para detalle completo.",
        false
      );
      helper.addAttachment(
        "Reporte_Semanal_" +
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
          ".xlsx",
        () -> new java.io.ByteArrayInputStream(excelBytes),
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      );
      mailSender.send(msg);
    } catch (MessagingException e) {
      log.error("❌ Failed to send report to {}: {}", to, e.getMessage());
    }
  }

  // ── Style Helpers ──────────────────────────────────────────────────────────

  // FIXED: Standardize hex strings to standard hex codes (e.g. "5C1212" without "FF") and use XSSFFont
  private Font createFont(
    Workbook wb,
    int size,
    boolean bold,
    String hexColor
  ) {
    XSSFFont font = (XSSFFont) wb.createFont();
    font.setFontHeightInPoints((short) size);
    font.setBold(bold);
    font.setColor(new XSSFColor(Color.decode("#" + hexColor), null));
    return font;
  }

  // FIXED: Apply passed background colors properly
  private CellStyle createHeaderStyle(Workbook wb, Color bgColor) {
    XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
    style.setFont(createFont(wb, 10, true, "000000"));
    style.setFillForegroundColor(new XSSFColor(bgColor, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    setBorder(style);
    return style;
  }

  // FIXED: Apply passed background colors properly
  private CellStyle createAlertStyle(Workbook wb, Color bgColor) {
    XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(bgColor, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.LEFT);
    setBorder(style);
    return style;
  }

  // Extracted repetitive border assignments to a cleaner helper
  private void setBorder(CellStyle style) {
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
  }

  private String formatMoney(BigDecimal amount) {
    if (amount == null) return "$0.00";
    return (
      "$" +
      amount
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString()
        .replaceAll("(\\d)(?=(\\d{3})+\\.)", "$1,")
    );
  }
}
