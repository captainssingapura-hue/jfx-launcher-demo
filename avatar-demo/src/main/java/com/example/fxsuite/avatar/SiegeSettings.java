package com.example.fxsuite.avatar;

import com.example.fxsuite.avatar.sim.Siege;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;

/**
 * The temple siege's settings, before it starts ({@link Siege}): how many airships at once, how often
 * they come and how much sooner each time, how fast and how big, how often they bomb and how likely
 * they are to bomb other buildings on the way; how tough the temple is and how tough they are; what
 * comes of the hall falling; and the seed their ways are drawn from, a new one each time, to be
 * changed, or kept to play the same siege again.
 */
final class SiegeSettings {

    private SiegeSettings() {
    }

    /** A setting on a slider: its name, from and to, in steps of, and how its value reads. */
    private record Setting(String name, double from, double to, double step, double value, DoubleFunction<String> reads) {
    }

    /**
     * The settings, as {@code was} had them (but a new seed), to change and start from: {@code start}
     * takes the siege as set, {@code back} goes back to the choice of demos.
     */
    static Scene scene(final Siege was, final Consumer<Siege> start, final Runnable back) {
        final List<Setting> settings = List.of(
                new Setting("Airships at once", 1, Siege.MOST_SHIPS, 1, was.ships(), v -> String.format("%.0f", v)),
                new Setting("One comes every", Siege.SOONEST, 120, 1, Math.min(120, was.every()),
                        v -> String.format("%.0f s", v)),
                new Setting("Each sooner than the last by", 0, Siege.FASTEST * 100, 1, was.faster() * 100,
                        v -> String.format("%.0f%%", v)),
                new Setting("Their speed", Siege.SLOWEST, Siege.QUICKEST, 1, was.speed(), v -> String.format("%.0f m/s", v)),
                new Setting("Their size", Siege.SMALLEST, Siege.LARGEST, 0.25, was.size(),
                        v -> String.format("%.2f× (%.0f m long)", v, 18 * v)),
                new Setting("A bomb every", Siege.QUICKEST_BOMBS, Siege.SLOWEST_BOMBS, 0.25, was.bombEvery(),
                        v -> String.format("%.2f s", v)),
                new Setting("Chance to bomb a building passed", 0, 100, 5, was.stray() * 100, v -> String.format("%.0f%%", v)),
                new Setting("Temple toughness", Siege.WEAKEST, Siege.TOUGHEST, 0.1, was.templeToughness(),
                        v -> String.format("%.1f×", v)),
                new Setting("Airship toughness", Siege.WEAKEST, Siege.TOUGHEST, 0.1, was.shipToughness(),
                        v -> String.format("%.1f×", v)),
                new Setting("Hall rebuilt after", 1, Siege.LATEST, 1, was.rebuildAfter(), v -> String.format("%.0f s", v)));

        final GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(10);
        final ColumnConstraints names = new ColumnConstraints(260);
        names.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().addAll(names, new ColumnConstraints(320), new ColumnConstraints(170));
        final List<Slider> sliders = new ArrayList<>();
        int row = 0;
        for (final Setting setting : settings) {
            final Slider slider = new Slider(setting.from(), setting.to(), setting.value());
            slider.setBlockIncrement(setting.step());
            slider.setMajorTickUnit(setting.step());
            slider.setMinorTickCount(0);
            slider.setSnapToTicks(true);
            final Label value = label(setting.reads().apply(setting.value()), "#f4f6f8");
            slider.valueProperty().addListener((o, a, b) -> value.setText(setting.reads().apply(snap(setting, b.doubleValue()))));
            grid.addRow(row++, label(setting.name(), "#aab4bf"), slider, value);
            sliders.add(slider);
        }
        // what comes of the hall falling; how long it takes to rebuild, only if it is
        final ChoiceBox<Siege.Fallen> fallen = new ChoiceBox<>();
        fallen.getItems().addAll(Siege.Fallen.values());
        fallen.setValue(was.fallen());
        fallen.setConverter(new StringConverter<>() {
            @Override
            public String toString(final Siege.Fallen f) {
                return f == null ? "" : f.title();
            }

            @Override
            public Siege.Fallen fromString(final String s) {
                return null;
            }
        });
        final Slider rebuild = sliders.getLast();
        rebuild.disableProperty().bind(fallen.valueProperty().isNotEqualTo(Siege.Fallen.REBUILT));
        grid.addRow(row++, label("When the hall falls", "#aab4bf"), fallen);
        // the seed: a new one each time, to be kept to play the same siege again
        final TextField seed = new TextField(Long.toString(ThreadLocalRandom.current().nextLong(1, 1_000_000)));
        final Button another = new Button("Another");
        another.setOnAction(e -> seed.setText(Long.toString(ThreadLocalRandom.current().nextLong(1, 1_000_000))));
        grid.addRow(row, label("Seed (the same, the same siege)", "#aab4bf"), new HBox(8, seed, another));

        final Label title = new Label("Temple siege");
        title.setStyle("-fx-text-fill: #f4f6f8; -fx-font-size: 30px; -fx-font-weight: bold;");
        final Label hint = label("Airships come in from the edge of the world without end, to bomb the temple's hall"
                + " until it falls. Set the siege, then start.  Enter starts  ·  Esc goes back", "#aab4bf");
        final Label problem = label("", "#e57373");
        final Button go = new Button("Start the siege");
        go.setDefaultButton(true);
        final Button cancel = new Button("Back");
        cancel.setCancelButton(true);
        final HBox buttons = new HBox(14, cancel, go);
        buttons.setAlignment(Pos.CENTER);
        go.setOnAction(e -> {
            try {
                start.accept(new Siege((int) Math.round(snap(settings.get(0), sliders.get(0).getValue())),
                        snap(settings.get(1), sliders.get(1).getValue()), snap(settings.get(2), sliders.get(2).getValue()) / 100,
                        snap(settings.get(3), sliders.get(3).getValue()), snap(settings.get(4), sliders.get(4).getValue()),
                        snap(settings.get(5), sliders.get(5).getValue()), snap(settings.get(6), sliders.get(6).getValue()) / 100,
                        snap(settings.get(7), sliders.get(7).getValue()), snap(settings.get(8), sliders.get(8).getValue()),
                        fallen.getValue(), snap(settings.get(9), sliders.get(9).getValue()),
                        Long.parseLong(seed.getText().trim())));
            } catch (final IllegalArgumentException ex) {
                problem.setText("Not a siege: " + ex.getMessage());
            }
        });
        cancel.setOnAction(e -> back.run());

        final VBox page = new VBox(16, title, hint, grid, problem, buttons);
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(40));
        page.setStyle("-fx-background-color: linear-gradient(to bottom, #1b222b, #11161c);");
        final Scene scene = new Scene(page, 1280, 800);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                back.run();
            }
        });
        return scene;
    }

    /** {@code value} on {@code setting}'s steps, within it. */
    private static double snap(final Setting setting, final double value) {
        final double snapped = setting.from() + Math.round((value - setting.from()) / setting.step()) * setting.step();
        return Math.clamp(Double.parseDouble(String.format(Locale.ROOT, "%.4f", snapped)), setting.from(), setting.to());
    }

    private static Label label(final String text, final String colour) {
        final Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + colour + "; -fx-font-size: 15px;");
        label.setWrapText(true);
        return label;
    }
}
