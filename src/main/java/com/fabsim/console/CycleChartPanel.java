package com.fabsim.console;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;

public final class CycleChartPanel extends JPanel {

    private final XYSeries series = new XYSeries("Cycle time");
    private final JFreeChart chart;
    private int cycleIndex = 0;

    public CycleChartPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        chart = ChartFactory.createXYLineChart(
                null,
                "Cycle number",
                "Seconds",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, new Color(47, 127, 224));
        renderer.setSeriesStroke(0, new BasicStroke(2f));
        plot.setRenderer(renderer);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(380, 180));
        chartPanel.setOpaque(false);
        add(chartPanel, BorderLayout.CENTER);
    }

    public void addCycle(double seconds) {
        cycleIndex++;
        series.add(cycleIndex, seconds);
    }


    public void reset() {
        series.clear();
        cycleIndex = 0;
    }

    public void applyTheme(boolean darkMode) {
        Color bg = darkMode ? new Color(22, 24, 29) : new Color(248, 249, 251);
        Color grid = darkMode ? new Color(70, 74, 84) : new Color(220, 223, 230);
        Color text = darkMode ? new Color(200, 203, 210) : new Color(70, 74, 84);

        chart.setBackgroundPaint(bg);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(bg);
        plot.setDomainGridlinePaint(grid);
        plot.setRangeGridlinePaint(grid);
        plot.getDomainAxis().setLabelPaint(text);
        plot.getDomainAxis().setTickLabelPaint(text);
        plot.getRangeAxis().setLabelPaint(text);
        plot.getRangeAxis().setTickLabelPaint(text);
    }
}
