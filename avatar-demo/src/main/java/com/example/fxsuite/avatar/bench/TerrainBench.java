package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.build.Terrains;
import com.example.fxsuite.avatar.sim.Survey;
import com.example.fxsuite.avatar.sim.Terrain;
import com.example.fxsuite.avatar.sim.Vec2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Terrains worked on as maps ({@link Terrain}): the map drawn flat over the land surveyed from it
 * ({@link TerrainMap}), its loops and spot heights moved, added to and taken from with the mouse; its
 * text beside it, to be written in and applied; what is wrong with it, how high and low the land
 * goes, and how long it took to survey. Surveyed apart from the screen, so the map keeps up.
 *
 * <p>{@code -Dterrain.file=path} opens a terrain's text to start with, or {@code -Dterrain.map=meadow}
 * one of the maps drawn so far ({@link Terrains}; else the valley); {@code -Dterrain.lines=2} draws the contour lines every so many metres.
 * {@code -Dbench.snapshot=out.png} saves the window after {@code -Dbench.settle} ms and exits.
 */
public final class TerrainBench extends Application {

    /** The maps drawn so far, by name, as text. */
    private static final Map<String, Supplier<String>> MAPS = new LinkedHashMap<>();

    static {
        MAPS.put("valley", () -> Terrains.VALLEY);
        MAPS.put("meadow", () -> Terrains.meadow().text());
    }

    private final TerrainMap map = new TerrainMap();
    private final TextArea text = new TextArea();
    private final Label figures = new Label();
    private final Label problems = new Label();
    private final ComboBox<Double> apart = new ComboBox<>();
    private final ExecutorService surveyor = Executors.newSingleThreadExecutor(r -> {
        final Thread thread = new Thread(r, "terrain survey");
        thread.setDaemon(true);
        return thread;
    });
    /** Which survey is the latest asked for: any other, finished late, is not shown. */
    private final AtomicLong asked = new AtomicLong();
    private Terrain terrain;
    private Stage stage;

    @Override
    public void start(final Stage stage) throws IOException {
        this.stage = stage;
        final String file = System.getProperty("terrain.file");
        final String named = System.getProperty("terrain.map", "valley").toLowerCase(Locale.ROOT);
        final String start = file != null ? Files.readString(Path.of(file), StandardCharsets.UTF_8)
                : MAPS.getOrDefault(named, MAPS.get("valley")).get();
        final ComboBox<String> maps = new ComboBox<>();
        maps.getItems().setAll(MAPS.keySet());
        maps.setValue(file != null ? null : MAPS.containsKey(named) ? named : "valley");
        maps.setPromptText("Map");
        maps.setOnAction(e -> {
            if (maps.getValue() != null) {
                text.setText(MAPS.get(maps.getValue()).get());
                applyText();
                map.fit();
            }
        });

        text.setFont(Font.font("Monospaced", 12));
        text.setWrapText(false);
        text.setText(start);
        VBox.setVgrow(text, Priority.ALWAYS);
        final Button apply = new Button("Apply text");
        apply.setOnAction(e -> applyText());
        final Button open = new Button("Open…");
        open.setOnAction(e -> open());
        final Button save = new Button("Save as…");
        save.setOnAction(e -> save());
        final Button fit = new Button("Fit");
        fit.setOnAction(e -> map.fit());
        apart.getItems().setAll(0.5, 1.0, 2.0, 5.0, 10.0, 20.0);
        apart.setValue(Double.parseDouble(System.getProperty("terrain.lines", "1")));
        apart.setOnAction(e -> survey());
        figures.setFont(Font.font("Monospaced", 12));
        problems.setWrapText(true);
        problems.setStyle("-fx-text-fill: #9b1c1c;");
        final Label help = new Label("Drag a point or a spot height to move it; double click a line to add a point;"
                + " right click a point to take it out; drag elsewhere to move the map, the wheel to zoom.");
        help.setWrapText(true);
        final VBox side = new VBox(8,
                new HBox(8, maps, apply, open, save, fit),
                new HBox(8, new Label("Contour lines every (m)"), apart),
                text, figures, problems, help);
        side.setPadding(new Insets(10));
        side.setPrefWidth(460);

        map.onEdit(edited -> {
            terrain = edited;
            text.setText(edited.text());
            survey();
        });
        map.onPick(this::showLine);

        final BorderPane root = new BorderPane(map);
        root.setRight(side);
        stage.setScene(new Scene(root, 1380, 880));
        stage.setTitle("Terrain bench");
        stage.show();
        applyText();
        SoundBench.snapshotIfAsked(stage.getScene());
    }

    /** Reads the text as the terrain, if it can be; if not, says why on which line, the terrain left as it was. */
    private void applyText() {
        try {
            terrain = Terrain.parse(text.getText());
        } catch (final IllegalArgumentException e) {
            problems.setText("! " + e.getMessage());
            problems.setStyle("-fx-text-fill: #9b1c1c;");
            return;
        }
        survey();
    }

    /** Shows the map at once, and surveys the land from it apart, to show when it is done. */
    private void survey() {
        if (terrain == null) {
            return;
        }
        final Terrain shown = terrain;
        this.map.show(shown);
        final List<String> wrong = shown.problems();
        problems.setText(wrong.isEmpty() ? "nothing wrong with it" : "! " + String.join("\n! ", wrong));
        problems.setStyle(wrong.isEmpty() ? "-fx-text-fill: #2f6b2f;" : "-fx-text-fill: #9b1c1c;");
        final long round = asked.incrementAndGet();
        final double lines = apart.getValue() * 100;
        surveyor.submit(() -> {
            try {
                final long from = System.nanoTime();
                final Survey land = shown.survey();
                final long surveyed = System.nanoTime();
                final Image relief = TerrainMap.relief(land);
                final List<List<Vec2>> thin = TerrainMap.lines(land, lines, false);
                final List<List<Vec2>> bold = TerrainMap.lines(land, lines, true);
                final long drawn = System.nanoTime();
                Platform.runLater(() -> {
                    if (asked.get() != round) {
                        return;
                    }
                    this.map.surveyed(land, relief, thin, bold);
                    figures.setText(String.format(Locale.ROOT,
                            "%s: %.0f m each way, a height every %.1f m (%d by %d)%n"
                                    + "land from %.2f m to %.2f m; %s, %s%n"
                                    + "surveyed in %.0f ms, its map drawn in %.0f ms",
                            shown.name(), (shown.x1() - shown.x0()) / 200, shown.every() / 100, land.across(), land.along(),
                            land.lowest() / 100, land.highest() / 100, count(shown.contours().size(), "loop"), count(shown.spots().size(), "spot height"),
                            (surveyed - from) / 1e6, (drawn - surveyed) / 1e6));
                });
            } catch (final RuntimeException e) {
                Platform.runLater(() -> problems.setText("! could not survey it: " + e));
            }
        });
    }

    private static String count(final int n, final String one) {
        return n + " " + one + (n == 1 ? "" : "s");
    }

    /** Selects the line of the text that is loop {@code loop}. */
    private void showLine(final int loop) {
        final String all = text.getText();
        int seen = -1;
        int at = 0;
        for (final String line : all.split("\n", -1)) {
            if (line.strip().startsWith("contour ") && ++seen == loop) {
                text.selectRange(at, at + line.length());
                return;
            }
            at += line.length() + 1;
        }
    }

    private FileChooser chooser(final String title) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Terrain maps", "*.terrain", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        return chooser;
    }

    private void open() {
        final File file = chooser("Open a terrain").showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            text.setText(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            applyText();
            map.fit();
        } catch (final IOException e) {
            problems.setText("! could not read it: " + e.getMessage());
        }
    }

    private void save() {
        final FileChooser chooser = chooser("Save the terrain");
        if (terrain != null) {
            chooser.setInitialFileName(terrain.name() + ".terrain");
        }
        final File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), text.getText(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            problems.setText("! could not write it: " + e.getMessage());
        }
    }
}
