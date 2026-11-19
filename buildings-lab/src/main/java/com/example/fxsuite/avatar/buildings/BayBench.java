package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.bench.kit.Bus;
import com.example.fxsuite.avatar.bench.kit.Orbit;
import com.example.fxsuite.avatar.bench.kit.Player;
import com.example.fxsuite.avatar.bench.kit.Shell;
import com.example.fxsuite.avatar.bench.kit.Workshop;
import com.example.fxsuite.avatar.lifts.Built;
import com.example.fxsuite.avatar.lifts.Mover;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The bay's buildings bench: the bay's office towers ({@link Mbfc#standing}) where they stand in it, each turned as it
 * is, their lifts at work as they go in time; their figures read out; played, the whole bay laid with them standing in
 * it ({@link BayWorld}), the avatar starting before the way in of the one chosen. Put together of the bench kit's
 * parts, as the tower bench is.
 *
 * <p>{@code -Dbay.start=2} which tower to start at (1 to 3); {@code -Dbay.time=12} (seconds) where in time it starts;
 * {@code -Dbay.yaw}, {@code -Dbay.pitch}, {@code -Dbay.distance} (m) where it is seen from; {@code -Dbench.play=true}
 * plays it at once; {@code -Dbench.snapshot=out.png} saves the window after {@code -Dbench.settle} ms and exits.
 */
public final class BayBench extends Application {

    private final Bus bus = new Bus();
    private final Shell shell = new Shell("Buildings lab: Marina Bay", bus);
    private final Orbit view = Orbit.standard();
    private final Group frames = new Group();
    private final Group moving = new Group();
    private final Label clock = new Label();
    private final ComboBox<Double> rate = new ComboBox<>();
    private final ComboBox<String> first = new ComboBox<>();
    private final List<Standing> towers = Mbfc.standing();
    private List<Built> built;
    private final List<Node> parts = new ArrayList<>();
    private final List<Rotate> turns = new ArrayList<>();
    /** Which tower each moving part is of, and which of its movers. */
    private final List<int[]> whose = new ArrayList<>();
    private final Player player = new Player(this::world, List.of());
    private double time = Double.parseDouble(System.getProperty("bay.time", "0"));
    private boolean running = true;
    private boolean playWhenShown = Boolean.getBoolean("bench.play");

    private record Asked() {
    }

    /** What the workshop made: each tower as built, its frame drawn where it stands; their moving parts drawn. */
    private record Made(List<Built> built, List<Node> frames, List<Node> parts, List<int[]> whose) {
    }

    private final Workshop<Asked, Made> workshop = new Workshop<>("bay", bus, Asked.class, this::make);

    @Override
    public void start(final Stage stage) {
        view.add(ground(), frames, moving);
        bus.subscribe(Made.class, this::show, Bus.SCREEN);
        final Button pause = new Button("Pause");
        pause.setOnAction(e -> {
            running = !running;
            pause.setText(running ? "Pause" : "Go on");
        });
        rate.getItems().setAll(0.25, 0.5, 1.0, 2.0, 5.0, 10.0);
        rate.setValue(1.0);
        final Button play = new Button("Play");
        play.setStyle("-fx-font-weight: bold;");
        play.setOnAction(e -> player.play());
        shell.bar(List.of(pause, new Label("Speed ×"), rate, clock, play));
        first.getItems().setAll(towers.stream().map(t -> t.tower().name()).toList());
        final int chosen = Integer.getInteger("bay.start", 1);
        first.getSelectionModel().select(Math.clamp(chosen, 1, towers.size()) - 1);
        final VBox side = new VBox(10, new Label("Start before"), first, player.panel());
        side.setPadding(new Insets(8));
        shell.side(new Label("The bay's towers"), side, 420);
        shell.middle(view.node());
        bus.publish(new Asked());
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

    /** The bay with its towers, to play, starting at the one chosen; none until they are built. */
    private BayWorld world() {
        return built == null ? null
                : new BayWorld(towers, built, Math.max(0, first.getSelectionModel().getSelectedIndex()));
    }

    /** On the workshop's worker: each tower built and drawn where it stands; its moving parts drawn. */
    private Made make(final Asked a) {
        final List<Built> made = new ArrayList<>();
        final List<Node> drawn = new ArrayList<>();
        final List<Node> moving = new ArrayList<>();
        final List<int[]> of = new ArrayList<>();
        for (int k = 0; k < towers.size(); k++) {
            final Standing t = towers.get(k);
            final Built b = t.built();
            made.add(b);
            drawn.add(BuildingModel.of(t.placed(b.drawn())));
            for (int m = 0; m < b.movers().size(); m++) {
                final Group part = new Group(BuildingModel.of(b.movers().get(m).solids()));
                part.getTransforms().add(new Rotate(0, Rotate.Y_AXIS));
                moving.add(part);
                of.add(new int[] {k, m});
            }
        }
        return new Made(made, drawn, moving, of);
    }

    /** On the screen's thread: the towers shown, looked at from over the middle of them; played at once if asked. */
    private void show(final Made m) {
        built = m.built();
        frames.getChildren().setAll(m.frames());
        parts.clear();
        turns.clear();
        whose.clear();
        parts.addAll(m.parts());
        whose.addAll(m.whose());
        m.parts().forEach(p -> turns.add((Rotate) p.getTransforms().getFirst()));
        moving.getChildren().setAll(m.parts());
        place();
        final StringBuilder figures = new StringBuilder();
        for (int k = 0; k < towers.size(); k++) {
            final Place at = towers.get(k).at();
            figures.append(String.format(Locale.ROOT, "%s at %.0f, %.0f m, turned %.1f°%n%s%n%n",
                    towers.get(k).tower().name(), at.x() / 100, at.z() / 100, at.yaw(), built.get(k).figures()));
        }
        bus.publish(new Bus.Figures(figures.toString().strip()));
        bus.publish(new Bus.Problems(""));
        double x = 0;
        double z = 0;
        for (final Standing t : towers) {
            x += t.at().x() / towers.size();
            z += t.at().z() / towers.size();
        }
        view.lookAt(x, -10_000, z);
        view.from("bay", 200, 18, 520);
        view.zoom(300, 300_000);
        if (playWhenShown) {
            playWhenShown = false;
            player.play();
        }
    }

    /** Each moving part where its way has it now, where its tower stands, turned as it faces. */
    private void place() {
        if (built != null) {
            for (int k = 0; k < parts.size(); k++) {
                final int[] w = whose.get(k);
                final Mover mover = built.get(w[0]).movers().get(w[1]);
                final Place p = towers.get(w[0]).placed(mover.route().at(time));
                final Node part = parts.get(k);
                part.setTranslateX(p.x());
                part.setTranslateY(-p.height());
                part.setTranslateZ(p.z());
                turns.get(k).setAngle(p.yaw());
            }
        }
        clock.setText(String.format(Locale.ROOT, "%.1f s", time));
    }

    /** Level ground round the towers, a kilometre each way. */
    private Box ground() {
        double x = 0;
        double z = 0;
        double h = 0;
        for (final Standing t : towers) {
            x += t.at().x() / towers.size();
            z += t.at().z() / towers.size();
            h += t.at().height() / towers.size();
        }
        final Box ground = new Box(100_000, 2, 100_000);
        ground.setTranslateX(x);
        ground.setTranslateY(-h + 1.5);
        ground.setTranslateZ(z);
        final PhongMaterial grass = new PhongMaterial(Color.web("#9fae8f"));
        grass.setSpecularColor(Color.web("#3a3f46"));
        ground.setMaterial(grass);
        return ground;
    }
}
