package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.roads.Earthworks;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.roads.Strip;

import javafx.geometry.HPos;
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
import java.util.function.Consumer;
import java.util.function.DoubleFunction;

/**
 * A kind of road across ({@link Section}) as controls on the roads bench: how many lanes each way, how wide each
 * kind of strip it has, how much its lanes and verges fall, how steep it may be, what it asks of the ground. Each
 * change made to the kind at once, handed on whole ({@link #KindPanel}); the kind's text written again from it.
 */
final class KindPanel extends VBox {

    private final Consumer<Section> changed;
    private final GridPane grid = new GridPane();
    private final Label title = new Label();
    private Section shown;
    /** Whether the controls are being filled in, not changed by hand. */
    private boolean filling;
    private int row;

    /** Controls handing each kind as changed to {@code changed}. */
    KindPanel(final Consumer<Section> changed) {
        this.changed = changed;
        title.setFont(Font.font("System", FontWeight.BOLD, 13));
        grid.setHgap(8);
        grid.setVgap(5);
        final ColumnConstraints names = new ColumnConstraints();
        names.setHalignment(HPos.LEFT);
        names.setMinWidth(170);
        grid.getColumnConstraints().setAll(names, new ColumnConstraints());
        setSpacing(8);
        setPadding(new Insets(8));
        getChildren().setAll(title, grid);
    }

    /** {@code section}'s controls, filled in from it. */
    void show(final Section section) {
        filling = true;
        shown = section;
        title.setText(section.name());
        grid.getChildren().clear();
        row = 0;
        heading("Across");
        if (section.lanes() > 0) {
            whole("Lanes each way", 1, 8, section.lanes(), n -> shown.withLanes(n));
        }
        for (final Strip.Use use : section.uses()) {
            final String name = capitalised(use.written());
            decimal(name + " width (m)", 0.05, 20, 0.05, section.width(use) / 100, v -> shown.withWidth(use, v * 100));
        }
        if (section.lanes() > 0) {
            percent("Lanes' crossfall (%)", -8, 8, 0.25, section.fall(Strip.Use.LANE),
                    v -> shown.withFall(Strip.Use.LANE, v / 100));
        }
        if (section.uses().contains(Strip.Use.VERGE)) {
            percent("Verges' fall (%)", -15, 15, 0.5, section.fall(Strip.Use.VERGE),
                    v -> shown.withFall(Strip.Use.VERGE, v / 100));
        }
        heading("Along");
        percent("Steepest grade (%)", 0.5, 25, 0.5, section.grade(), v -> shown.withLimits(v / 100, shown.vertical()));
        decimal("Vertical length (m)", 0, 1_000, 5, section.vertical() / 100,
                v -> shown.withLimits(shown.grade(), v * 100));
        heading("Ground");
        final Earthworks e = section.earthworks();
        decimal("Banks, across per up", 0.25, 6, 0.25, e.fill(), v -> earthworks(v, null, null, null, null, null));
        decimal("Cuttings, across per up", 0.25, 6, 0.25, e.cut(), v -> earthworks(null, v, null, null, null, null));
        decimal("Ditch width (m)", 0, 5, 0.1, e.ditchWide() / 100, v -> earthworks(null, null, v * 100, null, null, null));
        decimal("Ditch depth (m)", 0, 3, 0.1, e.ditchDeep() / 100, v -> earthworks(null, null, null, v * 100, null, null));
        decimal("Highest bank (m)", 0.5, 40, 0.5, e.bank() / 100, v -> earthworks(null, null, null, null, v * 100, null));
        decimal("Deepest cutting (m)", 0.5, 40, 0.5, e.cutting() / 100,
                v -> earthworks(null, null, null, null, null, v * 100));
        filling = false;
    }

    /** The kind shown, its earthworks changed in whichever of these is given. */
    private Section earthworks(final Double fill, final Double cut, final Double ditchWide, final Double ditchDeep,
                               final Double bank, final Double cutting) {
        final Earthworks e = shown.earthworks();
        return shown.withEarthworks(new Earthworks(fill != null ? fill : e.fill(), cut != null ? cut : e.cut(),
                ditchWide != null ? ditchWide : e.ditchWide(), ditchDeep != null ? ditchDeep : e.ditchDeep(),
                bank != null ? bank : e.bank(), cutting != null ? cutting : e.cutting()));
    }

    private void heading(final String text) {
        final Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 11));
        label.setPadding(new Insets(row == 0 ? 0 : 6, 0, 0, 0));
        grid.add(label, 0, row++, 2, 1);
    }

    private void whole(final String name, final int least, final int most, final int value,
                       final java.util.function.IntFunction<Section> change) {
        final Spinner<Integer> spinner = new Spinner<>(least, most, value);
        spinner.setPrefWidth(110);
        spinner.valueProperty().addListener((o, was, now) -> apply(() -> change.apply(now)));
        grid.addRow(row++, new Label(name), spinner);
    }

    private void decimal(final String name, final double least, final double most, final double step, final double value,
                        final DoubleFunction<Section> change) {
        final Spinner<Double> spinner = new Spinner<>();
        final SpinnerValueFactory.DoubleSpinnerValueFactory values =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(least, most, value, step);
        values.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(final Double v) {
                return v == null ? "" : String.format(Locale.ROOT, "%.2f", v);
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
        spinner.setValueFactory(values);
        spinner.setEditable(true);
        spinner.setPrefWidth(110);
        // typed in: taken as soon as the field is left
        spinner.focusedProperty().addListener((o, was, now) -> {
            if (!now) {
                spinner.increment(0);
            }
        });
        spinner.valueProperty().addListener((o, was, now) -> apply(() -> change.apply(now)));
        grid.addRow(row++, new Label(name), spinner);
    }

    private void percent(final String name, final double least, final double most, final double step,
                         final double fraction, final DoubleFunction<Section> change) {
        decimal(name, least, most, step, fraction * 100, change);
    }

    /** A change by hand: the kind changed so, if it can be, and handed on. */
    private void apply(final java.util.function.Supplier<Section> change) {
        if (filling) {
            return;
        }
        try {
            shown = change.get();
            changed.accept(shown);
        } catch (final IllegalArgumentException e) {
            // a change the kind cannot take: left as it was
        }
    }

    private static String capitalised(final String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
