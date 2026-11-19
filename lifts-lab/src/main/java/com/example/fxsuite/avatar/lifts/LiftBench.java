package com.example.fxsuite.avatar.lifts;

import com.example.fxsuite.avatar.bench.kit.Bus;
import com.example.fxsuite.avatar.bench.kit.Orbit;
import com.example.fxsuite.avatar.bench.kit.Player;
import com.example.fxsuite.avatar.bench.kit.Shell;
import com.example.fxsuite.avatar.look.BuildingModel;
import com.example.fxsuite.avatar.sim.Place;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The lifts bench: the lifts yard ({@link LiftYard}) on show, each lift at work as it goes in time, faster or slower or
 * held still, to look at from any side; its figures read out; and played, to ride them, with whoever is chosen to come.
 * Put together of the bench kit's parts.
 *
 * <p>{@code -Dlifts.time=12} (seconds) where in time it starts; {@code -Dlifts.rate=2} how fast it goes;
 * {@code -Dlifts.yaw}, {@code -Dlifts.pitch}, {@code -Dlifts.distance} (m) where it is seen from; {@code -Dbench.play=true}
 * plays it at once; {@code -Dbench.snapshot=out.png} saves the window after {@code -Dbench.settle} ms and exits.
 */
public final class LiftBench extends Application {

    private final Bus bus = new Bus();
    private final Shell shell = new Shell("Lifts bench", bus);
    private final Orbit view = Orbit.standard();
    private final LiftYard yard = new LiftYard();
    private final Player player = new Player(() -> yard, List.of());
    private final List<Mover> movers = yard.movers();
    private final List<Group> moving = new ArrayList<>();
    private final Label clock = new Label();
    private final ComboBox<Double> rate = new ComboBox<>();
    private double time = Double.parseDouble(System.getProperty("lifts.time", "0"));
    private boolean running = true;

    @Override
    public void start(final Stage stage) {
        view.add(ground(), BuildingModel.of(yard.drawn()));
        for (final Mover m : movers) {
            final Group part = new Group(BuildingModel.of(m.solids()));
            moving.add(part);
            view.add(part);
        }
        place();
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
        shell.bar(List.of(pause, new Label("Speed ×"), rate, clock, play));
        final VBox side = new VBox(10, player.panel());
        side.setPadding(new Insets(4, 0, 0, 0));
        shell.side(new Label("The lifts yard"), side, 380);
        shell.middle(view.node());
        bus.publish(new Bus.Figures(figures()));
        view.lookAt(0, -700, 0);
        view.from("lifts", -35, 20, 45);
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
        shell.show(stage, 1400, 900);
        if (Boolean.getBoolean("bench.play")) {
            player.play();
        }
    }

    /** Each moving part where its way has it now. */
    private void place() {
        for (int k = 0; k < movers.size(); k++) {
            final Place p = movers.get(k).route().at(time);
            final Group part = moving.get(k);
            part.setTranslateX(p.x());
            part.setTranslateY(-p.height());
            part.setTranslateZ(p.z());
        }
        clock.setText(String.format(Locale.ROOT, "%.1f s", time));
    }

    /** What its lifts come to: how big, how fast, how long round. */
    private String figures() {
        final Paternoster p = yard.paternoster;
        final ShuttleLift s = yard.shuttle;
        final Escalator e = yard.escalator;
        return String.format(Locale.ROOT,
                "Paternoster: %d cabins round a loop %.1f m long, at %.2f m/s; round in %.0f s; a cabin past a landing"
                        + " every %.1f s each way%n"
                        + "Shuttle lift: %d floors, at %.1f m/s top speed; round its timetable in %.1f s, %.1f s at each"
                        + " stop%n"
                        + "Escalator: %.1f m up at %.0f°, %.1f m along; %d steps, each rising %.1f cm; %.2f m/s",
                p.cabins(), p.loop() / 100, p.speed() / 100, p.round(), p.round() / p.cabins(),
                s.floors().size(), s.speed() / 100, s.cycle(), ShuttleLift.dwell(),
                e.rise() / 100, e.slope(), e.length() / 100, e.steps(), e.riser(), e.speed() / 100);
    }

    /** Level ground under it all, grass. */
    private static Box ground() {
        final Box ground = new Box(2 * LiftYard.GROUND, 2, 2 * LiftYard.GROUND);
        ground.setTranslateY(1.5);
        final PhongMaterial grass = new PhongMaterial(Color.web("#9fae8f"));
        grass.setSpecularColor(Color.web("#3a3f46"));
        ground.setMaterial(grass);
        return ground;
    }
}
