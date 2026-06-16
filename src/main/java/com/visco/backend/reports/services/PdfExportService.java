package com.visco.backend.reports.services;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.visco.backend.reports.models.dtos.AlertReportDTO;
import com.visco.backend.reports.models.dtos.MovementReportDTO;
import com.visco.backend.reports.models.dtos.StockReportDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.CategoryDistributionDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.TopProductDTO;
import com.visco.backend.reports.utils.ChartGenerator;
import com.visco.backend.reports.utils.DateUtils;
import com.visco.backend.reports.utils.NumberFormatter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
/**
 * Exports report data to PDF documents using iText 7.
 *
 * <p>All iText resources (PdfWriter, PdfDocument, Document) are managed in a
 * single try-with-resources chain so they are released even if a header /
 * table / chart builder throws. The chart ByteArrayOutputStream is also
 * closed explicitly to be consistent with the rest of the codebase.
 */
public class PdfExportService {

    private static final DeviceRgb PRIMARY = new DeviceRgb(92, 18, 18);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(245, 245, 247);
    private static final DeviceRgb DARK_TEXT = new DeviceRgb(55, 65, 81);

    /**
     * Writes a stock inventory report as a PDF document to the given output stream.
     */
    public void exportStockReportToPdf(List<StockReportDTO> data, String title,
                                        Map<String, String> metadata, OutputStream outputStream) {
        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4.rotate())) {
            document.setMargins(20, 20, 20, 20);
            addHeader(document, title, metadata);
            addStockTable(document, data);
            addFooter(document);
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF stock report", e);
        }
    }

    /**
     * Writes a movement history report as a PDF document to the given output stream.
     */
    public void exportMovementReportToPdf(List<MovementReportDTO> data, String title,
                                           Map<String, String> metadata, OutputStream outputStream) {
        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4.rotate())) {
            document.setMargins(20, 20, 20, 20);
            addHeader(document, title, metadata);
            addMovementTable(document, data);
            addFooter(document);
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF movement report", e);
        }
    }

    /**
     * Writes an inventory alert report as a PDF document to the given output stream.
     */
    public void exportAlertReportToPdf(List<AlertReportDTO> data, String title,
                                        Map<String, String> metadata, OutputStream outputStream) {
        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4.rotate())) {
            document.setMargins(20, 20, 20, 20);
            addHeader(document, title, metadata);
            addAlertTable(document, data);
            addFooter(document);
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF alert report", e);
        }
    }

    /**
     * Writes a warehouse analysis report (summary + per-warehouse sections with charts) as a PDF.
     */
    public void exportWarehouseAnalysisToPdf(List<WarehouseAnalysisDTO> data, String title,
                                              Map<String, String> metadata, OutputStream outputStream) {
        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4.rotate())) {
            document.setMargins(20, 20, 20, 20);
            addHeader(document, title, metadata);
            for (WarehouseAnalysisDTO wh : data) {
                addWarehouseSection(document, wh);
            }
            addFooter(document);
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF warehouse analysis report", e);
        }
    }

    private void addHeader(Document document, String title, Map<String, String> metadata) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont();
        PdfFont regularFont = PdfFontFactory.createFont();

        Paragraph titlePara = new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(18)
                .setFontColor(PRIMARY)
                .setTextAlignment(TextAlignment.LEFT);
        document.add(titlePara);

        if (metadata != null) {
            StringBuilder metaText = new StringBuilder();
            for (var entry : metadata.entrySet()) {
                if (metaText.length() > 0) metaText.append(" | ");
                metaText.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            document.add(new Paragraph(metaText.toString())
                    .setFont(regularFont)
                    .setFontSize(8)
                    .setFontColor(DARK_TEXT)
                    .setMarginBottom(10));
        }
    }

    private void addFooter(Document document) throws IOException {
        PdfFont regularFont = PdfFontFactory.createFont();
        document.add(new Paragraph("Generado: " + DateUtils.formatDateTime(LocalDateTime.now()))
                .setFont(regularFont)
                .setFontSize(7)
                .setFontColor(DARK_TEXT)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(10));
    }

    private void addStockTable(Document document, List<StockReportDTO> data) throws IOException {
        PdfFont regularFont = PdfFontFactory.createFont();
        PdfFont boldFont = PdfFontFactory.createFont();

        document.add(new Paragraph("Resumen de Stock")
                .setFont(boldFont).setFontSize(12).setFontColor(PRIMARY).setMarginTop(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2, 2, 2, 2, 2}))
                .useAllAvailableWidth();
        addHeaderCell(table, "Producto");
        addHeaderCell(table, "SKU");
        addHeaderCell(table, "Stock");
        addHeaderCell(table, "Pendiente");
        addHeaderCell(table, "Pto Reorden");
        addHeaderCell(table, "Estado");
        addHeaderCell(table, "Categoría");

        for (StockReportDTO item : data) {
            addCell(table, item.getProductName(), regularFont);
            addCell(table, item.getSku(), regularFont);
            addCell(table, NumberFormatter.formatNumber(item.getCurrentStock()), regularFont);
            addCell(table, NumberFormatter.formatNumber(item.getPendingStock()), regularFont);
            addCell(table, NumberFormatter.formatNumber(item.getReorderPoint()), regularFont);
            addCell(table, item.getStatus(), regularFont);
            addCell(table, item.getCategory(), regularFont);
        }

        document.add(table);
    }

    private void addMovementTable(Document document, List<MovementReportDTO> data) throws IOException {
        PdfFont regularFont = PdfFontFactory.createFont();
        PdfFont boldFont = PdfFontFactory.createFont();

        document.add(new Paragraph("Historial de Movimientos")
                .setFont(boldFont).setFontSize(12).setFontColor(PRIMARY).setMarginTop(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 3, 2, 2, 2, 2}))
                .useAllAvailableWidth();
        addHeaderCell(table, "Fecha");
        addHeaderCell(table, "Tipo");
        addHeaderCell(table, "Producto");
        addHeaderCell(table, "Cantidad");
        addHeaderCell(table, "Almacén");
        addHeaderCell(table, "Usuario");
        addHeaderCell(table, "Referencia");

        for (MovementReportDTO item : data) {
            addCell(table, DateUtils.formatDate(item.getMovementDate()), regularFont);
            addCell(table, item.getMovementType(), regularFont);
            addCell(table, item.getProductName(), regularFont);
            addCell(table, NumberFormatter.formatNumber(item.getQuantity()), regularFont);
            addCell(table, item.getWarehouseName(), regularFont);
            addCell(table, item.getUserName(), regularFont);
            addCell(table, item.getReference(), regularFont);
        }

        document.add(table);
    }

    private void addAlertTable(Document document, List<AlertReportDTO> data) throws IOException {
        PdfFont regularFont = PdfFontFactory.createFont();
        PdfFont boldFont = PdfFontFactory.createFont();

        document.add(new Paragraph("Alertas de Inventario")
                .setFont(boldFont).setFontSize(12).setFontColor(PRIMARY).setMarginTop(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2, 3}))
                .useAllAvailableWidth();
        addHeaderCell(table, "Producto");
        addHeaderCell(table, "SKU");
        addHeaderCell(table, "Stock");
        addHeaderCell(table, "Tipo Alerta");
        addHeaderCell(table, "Severidad");
        addHeaderCell(table, "Acción Recomendada");

        for (AlertReportDTO item : data) {
            addCell(table, item.getProductName(), regularFont);
            addCell(table, item.getSku(), regularFont);
            addCell(table, NumberFormatter.formatNumber(item.getCurrentStock()), regularFont);
            addCell(table, item.getAlertType(), regularFont);
            addCell(table, item.getSeverity(), regularFont);
            addCell(table, item.getRecommendedAction(), regularFont);
        }

        document.add(table);
    }

    private void addWarehouseSection(Document document, WarehouseAnalysisDTO wh) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont();
        PdfFont regularFont = PdfFontFactory.createFont();

        document.add(new Paragraph("Almacén: " + wh.getWarehouseName())
                .setFont(boldFont).setFontSize(14).setFontColor(PRIMARY).setMarginTop(15));

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2, 2, 2}))
                .useAllAvailableWidth();
        addHeaderCell(infoTable, "Productos");
        addHeaderCell(infoTable, "Valor Total");
        addHeaderCell(infoTable, "Utilización");
        addHeaderCell(infoTable, "Stock Crítico");
        addHeaderCell(infoTable, "Stock Bajo");

        addCell(infoTable, NumberFormatter.formatNumber(wh.getProductCount()), regularFont);
        addCell(infoTable, NumberFormatter.formatCurrency(wh.getTotalValue()), regularFont);
        addCell(infoTable, NumberFormatter.formatPercent(wh.getCapacityUtilization()), regularFont);
        addCell(infoTable, NumberFormatter.formatNumber(wh.getCriticalProducts()), regularFont);
        addCell(infoTable, NumberFormatter.formatNumber(wh.getLowStockProducts()), regularFont);
        document.add(infoTable);

        if (wh.getCategoryDistribution() != null && !wh.getCategoryDistribution().isEmpty()) {
            try {
                BufferedImage chart = ChartGenerator.createPieChart(
                        "Distribución por Categoría", wh.getCategoryDistribution(), 400, 300);
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(chart, "png", baos);
                    Image img = new Image(com.itextpdf.io.image.ImageDataFactory.create(baos.toByteArray()));
                    img.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    img.setMaxWidth(400);
                    document.add(img);
                }
            } catch (Exception e) {
                log.warn("Could not generate pie chart for warehouse {}", wh.getWarehouseName(), e);
            }
        }

        if (wh.getTopByQuantity() != null && !wh.getTopByQuantity().isEmpty()) {
            try {
                BufferedImage chart = ChartGenerator.createBarChart(
                        "Top Productos", "Producto", "Stock", wh.getTopByQuantity(), 400, 300);
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(chart, "png", baos);
                    Image img = new Image(com.itextpdf.io.image.ImageDataFactory.create(baos.toByteArray()));
                    img.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    img.setMaxWidth(400);
                    document.add(img);
                }
            } catch (Exception e) {
                log.warn("Could not generate bar chart for warehouse {}", wh.getWarehouseName(), e);
            }
        }
    }

    private void addHeaderCell(Table table, String text) {
        Cell cell = new Cell().add(new Paragraph(text))
                .setBackgroundColor(PRIMARY)
                .setFontColor(WHITE)
                .setBold()
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(4);
        table.addHeaderCell(cell);
    }

    private void addCell(Table table, String text, PdfFont font) {
        table.addCell(new Cell().add(new Paragraph(text != null ? text : ""))
                .setFont(font)
                .setFontSize(7)
                .setPadding(3)
                .setBorder(new SolidBorder(LIGHT_GRAY, 0.5f)));
    }
}
