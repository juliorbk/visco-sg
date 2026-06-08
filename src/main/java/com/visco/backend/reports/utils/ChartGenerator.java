package com.visco.backend.reports.utils;

import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.CategoryDistributionDTO;
import com.visco.backend.reports.models.dtos.WarehouseAnalysisDTO.TopProductDTO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
// Utility for generating JFreeChart-based pie, bar, gauge, and line charts for report PDFs.
public final class ChartGenerator {

    private static final Color PRIMARY = new Color(92, 18, 18);
    private static final Color ACCENT = new Color(160, 48, 42);
    private static final Color LIGHT = new Color(245, 245, 247);

    public static BufferedImage createPieChart(String title, List<CategoryDistributionDTO> data, int width, int height) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (CategoryDistributionDTO d : data) {
            dataset.setValue(d.getCategoryName(), d.getQuantity());
        }

        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint(0, PRIMARY);
        plot.setSectionPaint(1, ACCENT);
        plot.setSectionPaint(2, new Color(200, 150, 100));
        plot.setSectionPaint(3, new Color(100, 150, 200));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})"));
        plot.setShadowPaint(null);

        return chart.createBufferedImage(width, height);
    }

    public static BufferedImage createBarChart(String title, String categoryLabel, String valueLabel,
                                                List<TopProductDTO> data, int width, int height) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (TopProductDTO d : data) {
            dataset.addValue(d.getValue(), "Valor", truncateLabel(d.getProductName()));
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title, categoryLabel, valueLabel, dataset,
                PlotOrientation.HORIZONTAL, false, true, false);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, PRIMARY);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.15);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(LIGHT);
        plot.setRangeGridlinePaint(LIGHT);
        plot.setOutlinePaint(null);

        return chart.createBufferedImage(width, height);
    }

    public static BufferedImage createGaugeChart(String title, double value, int width, int height) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(Math.min(value, 100), "Utilización", "Capacidad");

        JFreeChart chart = ChartFactory.createBarChart(
                title, "", "%", dataset,
                PlotOrientation.VERTICAL, false, true, false);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, value > 80 ? Color.RED : value > 60 ? Color.ORANGE : PRIMARY);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.3);
        plot.getRangeAxis().setRange(0, 100);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(LIGHT);
        plot.setRangeGridlinePaint(LIGHT);
        plot.setOutlinePaint(null);

        return chart.createBufferedImage(width, height);
    }

    public static BufferedImage createLineChart(String title, List<BigDecimal> data, int width, int height) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < data.size(); i++) {
            dataset.addValue(data.get(i), "Stock", "Día " + (i + 1));
        }

        JFreeChart chart = ChartFactory.createLineChart(
                title, "Período", "Stock", dataset,
                PlotOrientation.VERTICAL, false, true, false);

        CategoryPlot plot = chart.getCategoryPlot();
        var renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, PRIMARY);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(LIGHT);
        plot.setRangeGridlinePaint(LIGHT);
        plot.setOutlinePaint(null);

        return chart.createBufferedImage(width, height);
    }

    private static String truncateLabel(String label) {
        if (label == null) return "";
        return label.length() > 25 ? label.substring(0, 22) + "..." : label;
    }
}
