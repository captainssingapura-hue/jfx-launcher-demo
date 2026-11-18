package com.example.fxsuite.avatar.bench;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A scene's knobs on the roads bench ({@link RoadScenes.Knob}): how long the loop is, how round its bends, how high its
 * viaduct. Each change handed on at once, by the knob's name ({@link #ScenePanel}); nothing shown for a scene with none.
 */
final class ScenePanel extends VBox {

    private final BiConsumer<String, Double> changed;
    private final GridPane grid = new GridPane();
    private final Label title = new Label();
    /** Whether the controls are being filled in, not changed by hand. */
    private boolean filling;

    /** Controls handing each knob as set to {@code changed}, by its name. */
    ScenePanel(final BiConsumer<String, Double> changed) {
        this.changed = changed;
        title.setFont(Font.font("System", FontWeight.BOLD, 13));
        grid.setHgap(8);
        grid.setVgap(5);
        final ColumnConstraints names = new ColumnConstraints();
        names.setMinWidth(170);
        grid.getColumnConstraints().setAll(names, new ColumnConstraints());
        setSpacing(8);
        setPadding(new Insets(8, 8, 0, 8));
    }

    /** {@code scene}'s knobs, set as {@code set} says, or as they are at first; nothing if it has none. */
    void show(final RoadScenes.Scene scene, final Map<String, Double> set) {
        filling = true;
        grid.getChildren().clear();
        if (scene.knobs().isEmpty()) {
            getChildren().clear();
            filling = false;
            return;
        }
        title.setText("Scene: " + scene.name());
        int row = 0;
        for (final RoadScenes.Knob knob : scene.knobs()) {
            final SpinnerValueFactory.DoubleSpinnerValueFactory values = new SpinnerValueFactory.DoubleSpinnerValueFactory(
                    knob.least(), knob.most(), set.getOrDefault(knob.name(), knob.value()), knob.step());
            values.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(final Double v) {
                    return v == null ? "" : String.format(Locale.ROOT, "%.1f", v);
                }

                @Override
                public Double fromString(final String s) {
                    try {
                        return Double.parseDouble(s.trim());
                    } catch (final NumberFormatException e) {
                        return values.getValue();
                    }
                }
            });
            final Spinner<Double> spinner = new Spinner<>(values);
            spinner.setEditable(true);
            spinner.setPrefWidth(110);
            spinner.focusedProperty().addListener((o, was, now) -> {
                if (!now) {
                    spinner.increment(0);
                }
            });
            spinner.valueProperty().addListener((o, was, now) -> {
                if (!filling) {
                    changed.accept(knob.name(), now);
                }
            });
            grid.addRow(row++, new Label(knob.name()), spinner);
        }
        getChildren().setAll(title, grid);
        filling = false;
    }
}
