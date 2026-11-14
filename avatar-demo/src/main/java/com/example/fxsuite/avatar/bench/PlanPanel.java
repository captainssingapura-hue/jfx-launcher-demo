package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.build.Plan;
import com.example.fxsuite.avatar.build.Plans;
import com.example.fxsuite.avatar.build.Plans.Shape;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

import java.util.Locale;

/**
 * A tower's floor plan, worked on: its shape picked ({@link Plans.Shape}), and how big, its bays,
 * and its one more thing (a corner's radius, an oval's squareness, an L's arm, a bow's radius) set
 * by sliders; drawn flat ({@link PlanView}); its figures read out: its floor, its outline's length,
 * its columns and beams, what is wrong with it.
 *
 * <p>{@code -Dplan.shape=oval} (the start of a shape's name) picks the shape to start with;
 * {@code -Dplan.width}, {@code -Dplan.depth}, {@code -Dplan.bay} and {@code -Dplan.extra}, in metres
 * (an oval's squareness as it is), how big.
 */
final class PlanPanel extends BorderPane {

    private final ComboBox<Shape> shapes = new ComboBox<>();
    private final Slider width = new Slider(20, 80, 48);
    private final Slider depth = new Slider(20, 80, 32);
    private final Slider bay = new Slider(6, 12, 9);
    private final Slider extra = new Slider(0, 1, 0);
    private final Label extraName = new Label();
    private final Label figures = new Label();
    private final PlanView view = new PlanView();
    private boolean setting;

    PlanPanel() {
        shapes.getItems().setAll(Shape.values());
        shapes.setConverter(new StringConverter<>() {
            @Override
            public String toString(final Shape shape) {
                return shape == null ? "" : shape.title();
            }

            @Override
            public Shape fromString(final String name) {
                return null;
            }
        });
        Shape shape = Shape.RECTANGLE;
        final String asked = System.getProperty("plan.shape");
        if (asked != null) {
            for (final Shape s : Shape.values()) {
                if (s.title().toLowerCase(Locale.ROOT).startsWith(asked.toLowerCase(Locale.ROOT))) {
                    shape = s;
                    break;
                }
            }
        }
        width.setValue(Double.parseDouble(System.getProperty("plan.width", "48")));
        depth.setValue(Double.parseDouble(System.getProperty("plan.depth", "32")));
        bay.setValue(Double.parseDouble(System.getProperty("plan.bay", "9")));
        shapes.setValue(shape);
        shaped(shape);
        if (System.getProperty("plan.extra") != null) {
            extra.setValue(Double.parseDouble(System.getProperty("plan.extra")));
        }
        shapes.setOnAction(e -> {
            shaped(shapes.getValue());
            redraw();
        });
        for (final Slider slider : new Slider[] {width, depth, bay, extra}) {
            slider.setPrefWidth(150);
            slider.valueProperty().addListener((o, was, now) -> redraw());
        }
        figures.setFont(Font.font("Monospaced", 12));
        final FlowPane controls = new FlowPane(16, 8, new HBox(8, new Label("Shape"), shapes),
                new HBox(6, new Label("Width (m)"), width), new HBox(6, new Label("Depth (m)"), depth),
                new HBox(6, new Label("Bay (m)"), bay), new HBox(6, extraName, extra));
        controls.setPadding(new Insets(10));
        setTop(controls);
        setCenter(view);
        figures.setPadding(new Insets(8, 10, 10, 10));
        setBottom(figures);
        redraw();
    }

    /** The sliders as {@code shape} has them: its depth only if it may differ from its width; its one more thing, if any. */
    private void shaped(final Shape shape) {
        setting = true;
        depth.setDisable(shape.even());
        extraName.setText(shape.extra() == null ? "(nothing more)" : shape.extra());
        extra.setDisable(shape.extra() == null);
        final boolean metres = shape != Shape.OVAL;
        extra.setMin(metres ? shape.least() / 100 : shape.least());
        extra.setMax(metres ? Math.max(shape.most(), shape.least() + 1) / 100 : shape.most());
        extra.setValue(metres ? shape.usual() / 100 : shape.usual());
        setting = false;
    }

    private void redraw() {
        if (setting) {
            return;
        }
        final Shape shape = shapes.getValue();
        final double e = shape == Shape.OVAL ? extra.getValue() : extra.getValue() * 100;
        final Plan.Layout layout = Plans.of(shape, new Plans.Size(width.getValue() * 100, depth.getValue() * 100,
                bay.getValue() * 100, e)).layout();
        view.show(layout);
        final StringBuilder out = new StringBuilder(String.format(Locale.ROOT,
                "%s: %.0f m² of floor, its outline %.1f m long; %d columns, %d beams", shape.title(),
                layout.area() / 10_000, layout.perimeter() / 100, layout.columns().size(), layout.beams().size()));
        if (shape.extra() != null) {
            out.append(String.format(Locale.ROOT, "; %s %.1f", shape.extra().toLowerCase(Locale.ROOT), extra.getValue()));
        }
        out.append(layout.problems().isEmpty() ? "\nnothing wrong with it" : "");
        layout.problems().forEach(p -> out.append("\n! ").append(p));
        figures.setText(out.toString());
    }
}
