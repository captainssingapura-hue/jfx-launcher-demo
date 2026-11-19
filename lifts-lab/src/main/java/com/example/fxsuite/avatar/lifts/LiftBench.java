package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Bus;
import com.example.fxsuite.avatar.bench.kit.Orbit;
import com.example.fxsuite.avatar.bench.kit.Player;
import com.example.fxsuite.avatar.bench.kit.Shell;
import com.example.fxsuite.avatar.bench.kit.Workshop;
import com.example.fxsuite.avatar.look.BuildingModel;
import com.example.fxsuite.avatar.sim.Place;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The lifts bench: a scene of a lift (a paternoster, a shuttle lift, an escalator, or the yard with all three) in a
 * building made for it, at work as it goes in time, faster or slower or held still, to look at from any side; set as
 * its knobs are (so many floors, so high; its cabins, car or steps so big; so fast), built again away from the screen
 * as they are changed; its figures read out; and played, to ride it, with whoever is chosen to come. Put together of
 * the bench kit's parts.
 *
 * <p>{@code -Dlifts.scene=escalator} (the start of a scene's name) the scene to start with; {@code -Dlifts.set=rise=6,
 * slope=35} its knobs; {@code -Dlifts.time=12} (seconds) where in time it starts; {@code -Dlifts.rate=2} how fast it
 * goes; {@code -Dlifts.yaw}, {@code -Dlifts.pitch}, {@code -Dlifts.distance} (m) where it is seen from;
 * {@code -Dbench.play=true} plays it at once; {@code -Dbench.snapshot=out.png} saves the window after
 * {@code -Dbench.settle} ms and exits.
 */
public final class LiftBench extends Application {

    private final Bus bus = new Bus();
    private final Shell shell = new Shell("Lifts bench", bus);
    private final Orbit view = Orbit.standard();
    private final List<LiftScene> scenes = LiftScene.all();
    private final ComboBox<LiftScene> sceneBox = new ComboBox<>();
    /** Each scene's knobs as set, by scene and knob. */
    private final Map<String, Map<String, Double>> set = new HashMap<>();
    private final VBox knobs = new VBox(6);
    private final Group frame = new Group();
    private final Group moving = new Group();
    private final Label clock = new Label();
    private final ComboBox<Double> rate = new ComboBox<>();
    /** The scene as last built and shown, its moving parts' nodes in its movers' order. */
    private Built shown;
    private final List<Node> parts = new ArrayList<>();
    private final Player player = new Player(() -> shown == null ? null : new SceneWorld(shown), List.of());
    private double time = Double.parseDouble(System.getProperty("lifts.time", "0"));
    private boolean running = true;
    private boolean playWhenShown = Boolean.getBoolean("bench.play");

    /** What is asked of the workshop: a scene, set so; and whether to look at it afresh. */
    private record Asked(LiftScene scene, Map<String, Double> set, boolean frame) {
    }

    /** What the workshop made of it: the scene built, its frame drawn, each moving part drawn. */
    private record Made(Asked asked, Built built, Node frame, List<Node> parts) {
    }

    private final Workshop<Asked, Made> workshop = new Workshop<>("lifts bench", bus, Asked.class, this::make);

    @Override
    public void start(final Stage stage) {
        view.add(ground(), frame, moving);
        bus.subscribe(Made.class, this::show, Bus.SCREEN);
        sceneBox.getItems().setAll(scenes);
        sceneBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(final LiftScene scene) {
                return scene == null ? "" : scene.name();
            }

            @Override
            public LiftScene fromString(final String name) {
                return null;
            }
        });
        final String asked = System.getProperty("lifts.scene", "paternoster").toLowerCase(Locale.ROOT);
        sceneBox.setValue(scenes.stream().filter(s -> s.name().toLowerCase(Locale.ROOT).startsWith(asked)).findFirst()
                .orElse(scenes.getFirst()));
        setFromProperty(sceneBox.getValue());
        sceneBox.setOnAction(e -> chosen(true));
        final Button pause = new Button("Pause");
        pause.setOnAction(e -> {
            running = !running;
            pause.setText(running ? "Pause" : "Go on");
        });
        rate.getItems().setAll(0.25, 0.5, 1.0, 2.0, 5.0, 10.0);
        rate.setValue(Double.parseDouble(System.getProperty("lifts.rate", "1")));
        final Button play = new Button("Play");
        play.setStyle("-fx-font-weight: bold;");
        play.setOnAction(e -> player.play());
        shell.bar(List.of(new Label("Scene"), sceneBox, pause, new Label("Speed ×"), rate, clock, play));
        final ScrollPane adjusting = new ScrollPane(knobs);
        adjusting.setFitToWidth(true);
        knobs.setPadding(new Insets(8));
        final VBox playing = new VBox(10, player.panel());
        playing.setPadding(new Insets(8));
        final TabPane tabs = new TabPane(new Tab("Adjust", adjusting), new Tab("Play", playing));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        shell.side(new Label("The scene"), tabs, 400);
        shell.middle(view.node());
        chosen(true);
        new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(final long now) {
                if (last >= 0 && running) {
                    time += (now - last) / 1e9 * rate.getValue();
                    place();
                }
                last = now;
            }
        }.start();
        shell.show(stage, 1440, 900);
    }

    @Override
    public void stop() {
        workshop.close();
    }

    /** {@code -Dlifts.set=key=value,...}: the starting scene's knobs as the properties say. */
    private void setFromProperty(final LiftScene scene) {
        final String asked = System.getProperty("lifts.set");
        if (asked == null) {
            return;
        }
        final Map<String, Double> values = set.computeIfAbsent(scene.name(), n -> new HashMap<>());
        for (final String pair : asked.split(",")) {
            final String[] kv = pair.split("=");
            if (kv.length == 2) {
                values.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
            }
        }
    }

    /** The scene chosen, or its knobs changed: its knobs shown, it built again; looked at afresh, if {@code frame}d. */
    private void chosen(final boolean frame) {
        final LiftScene scene = sceneBox.getValue();
        if (frame) {
            knobs(scene);
        }
        bus.publish(new Asked(scene, Map.copyOf(set.getOrDefault(scene.name(), Map.of())), frame));
    }

    /** The scene's knobs, each a spinner from its least to its most; a reset to put them back. */
    private void knobs(final LiftScene scene) {
        final GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        final Map<String, Double> values = set.computeIfAbsent(scene.name(), n -> new HashMap<>());
        int row = 0;
        for (final Knob k : scene.knobs()) {
            final SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(k.least(), k.most(), k.of(values), k.step());
            final Spinner<Double> spinner = new Spinner<>(factory);
            spinner.setEditable(true);
            spinner.setPrefWidth(110);
            spinner.valueProperty().addListener((o, was, now) -> {
                if (now != null) {
                    values.put(k.key(), now);
                    chosen(false);
                }
            });
            grid.addRow(row++, new Label(k.label()), spinner, new Label(k.unit()));
        }
        final Button reset = new Button("As they mostly are");
        reset.setOnAction(e -> {
            values.clear();
            chosen(true);
        });
        knobs.getChildren().setAll(scene.knobs().isEmpty() ? new Label("Each lift as it mostly is: nothing to set.")
                : grid, reset);
    }

    /** On the workshop's worker: the scene built as set, its frame and moving parts drawn. */
    private Made make(final Asked a) {
        final Built built = a.scene().built(a.set());
        final List<Node> drawn = new ArrayList<>();
        for (final Mover m : built.movers()) {
            drawn.add(new Group(BuildingModel.of(m.solids())));
        }
        return new Made(a, built, BuildingModel.of(built.drawn()), drawn);
    }

    /** On the screen's thread: the scene built, shown in place of what was; looked at afresh if asked. */
    private void show(final Made m) {
        shown = m.built();
        frame.getChildren().setAll(m.frame());
        parts.clear();
        parts.addAll(m.parts());
        moving.getChildren().setAll(m.parts());
        place();
        bus.publish(new Bus.Figures(shown.figures()));
        bus.publish(new Bus.Problems(""));
        if (m.asked().frame()) {
            final double[] middle = shown.middle();
            view.lookAt(middle[0], -middle[1], middle[2]);
            view.from("lifts", shown.yaw(), 20, shown.size() * 1.8 / 100 + 10);
            view.zoom(300, shown.size() * 10);
        }
        if (playWhenShown) {
            playWhenShown = false;
            player.play();
        }
    }

    /** Each moving part where its way has it now. */
    private void place() {
        if (shown != null) {
            for (int k = 0; k < parts.size() && k < shown.movers().size(); k++) {
                final Place p = shown.movers().get(k).route().at(time);
                final Node part = parts.get(k);
                part.setTranslateX(p.x());
                part.setTranslateY(-p.height());
                part.setTranslateZ(p.z());
            }
        }
        clock.setText(String.format(Locale.ROOT, "%.1f s", time));
    }

    /** Level ground under it all, grass. */
    private static Box ground() {
        final Box ground = new Box(20_000, 2, 20_000);
        ground.setTranslateY(1.5);
        final PhongMaterial grass = new PhongMaterial(Color.web("#9fae8f"));
        grass.setSpecularColor(Color.web("#3a3f46"));
        ground.setMaterial(grass);
        return ground;
    }
}
