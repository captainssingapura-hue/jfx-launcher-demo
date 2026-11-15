package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.build.Plan;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

import java.util.Locale;
import java.util.Map;

/**
 * The plans of a composed building's stacked parts ({@code build.Church}: its nave, apse, tower),
 * flat, a part at a time: which part picked, its plan drawn ({@link PlanView}), its figures read out.
 */
final class PartsPlans extends BorderPane {

    private final ComboBox<String> parts = new ComboBox<>();
    private final PlanView view = new PlanView();
    private final Label figures = new Label();
    private final Map<String, Plan> plans;

    PartsPlans(final Map<String, Plan> plans) {
        this.plans = plans;
        parts.getItems().setAll(plans.keySet());
        parts.setValue(parts.getItems().getFirst());
        parts.setOnAction(e -> show());
        final HBox top = new HBox(8, new Label("Part"), parts);
        top.setPadding(new Insets(10));
        setTop(top);
        setCenter(view);
        figures.setFont(Font.font("Monospaced", 12));
        figures.setPadding(new Insets(8, 10, 10, 10));
        setBottom(figures);
        show();
    }

    private void show() {
        final Plan.Layout layout = plans.get(parts.getValue()).layout();
        view.show(layout);
        final StringBuilder out = new StringBuilder(String.format(Locale.ROOT,
                "%s: %.0f m² of floor, its outline %.1f m long; %d columns (%d round its curves), %d beams",
                parts.getValue(), layout.area() / 10_000, layout.perimeter() / 100, layout.allColumns().size(),
                layout.perimeterColumns().size(), layout.beams().size() + layout.ties().size()));
        out.append(layout.problems().isEmpty() ? "\nnothing wrong with it" : "");
        layout.problems().forEach(p -> out.append("\n! ").append(p));
        figures.setText(out.toString());
    }
}
