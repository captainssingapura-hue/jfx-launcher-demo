package com.example.acme.charts;

import com.example.acme.ui.Panels;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.Map;
import java.util.TreeMap;

/**
 * AcmeCharts 2.20261022.0: charts in the Acme style, built on AcmeUi.
 */
public final class Charts implements StatelessFunctionalObject {

    public String release() {
        return "AcmeCharts 2.20261022.0";
    }

    public BarChart<String, Number> bars(final String title, final Map<String, Integer> values) {
        final BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setTitle(new Panels().banner(title).getText());
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(220);
        final XYChart.Series<String, Number> series = new XYChart.Series<>();
        new TreeMap<>(values).forEach((name, value) -> series.getData().add(new XYChart.Data<>(name, value)));
        chart.getData().add(series);
        return chart;
    }
}
