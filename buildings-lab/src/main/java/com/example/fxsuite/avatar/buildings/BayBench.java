package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.bench.kit.Bus;
import com.example.fxsuite.avatar.bench.kit.ModelView;
import com.example.fxsuite.avatar.bench.kit.Player;
import com.example.fxsuite.avatar.bench.kit.Shell;
import com.example.fxsuite.avatar.bench.kit.Workshop;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.lifts.Built;
import com.example.fxsuite.avatar.sim.Place;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The bay's buildings bench: the bay's office towers ({@link Mbfc#standing}, {@link Orq#standing},
 * {@link AsiaSquare#standing}) where they stand in it, each turned as it is, as they are built: still, a model to look at,
 * their lifts standing shut as they are when played; their figures read out. Played, the whole bay laid with them
 * standing in it ({@link BayWorld}), the avatar starting before the way in of the one chosen: what moves is seen there,
 * not here. Put together of the bench kit's parts, as the tower bench is.
 *
 * <p>{@code -Dbay.start=2} which tower to start at; {@code -Dbay.yaw}, {@code -Dbay.pitch}, {@code -Dbay.distance} (m)
 * where it is seen from; {@code -Dbench.play=true} plays it at once; {@code -Dbench.snapshot=out.png} saves the window
 * after {@code -Dbench.settle} ms and exits.
 */
public final class BayBench extends Application {

    private final Bus bus = new Bus();
    private final Shell shell = new Shell("Buildings lab: Marina Bay", bus);
    private final ModelView view = new ModelView();
    private final ComboBox<String> first = new ComboBox<>();
    private final List<Standing> towers = java.util.stream.Stream.of(Mbfc.standing(), Orq.standing(),
            AsiaSquare.standing()).flatMap(List::stream).toList();
    private List<Built> built;
    private final Player player = new Player(this::world, List.of());
    private boolean playWhenShown = Boolean.getBoolean("bench.play");

    private record Asked() {
    }

    /** What the workshop made: each tower as built, its model drawn where it stands. */
    private record Made(List<Built> built, List<ModelView.Model> models) {
    }

    private final Workshop<Asked, Made> workshop = new Workshop<>("bay", bus, Asked.class, this::make);

    @Override
    public void start(final Stage stage) {
        ground();
        bus.subscribe(Made.class, this::show, Bus.SCREEN);
        final Button play = new Button("Play");
        play.setStyle("-fx-font-weight: bold;");
        play.setOnAction(e -> player.play());
        shell.bar(List.of(play));
        first.getItems().setAll(towers.stream().map(t -> t.tower().name()).toList());
        final int chosen = Integer.getInteger("bay.start", 1);
        first.getSelectionModel().select(Math.clamp(chosen, 1, towers.size()) - 1);
        final VBox side = new VBox(10, new Label("Start before"), first, player.panel());
        side.setPadding(new Insets(8));
        shell.side(new Label("The bay's towers"), side, 420);
        shell.middle(view.node());
        bus.publish(new Asked());
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

    /** On the workshop's worker: each tower built and drawn where it stands, its lifts standing shut. */
    private Made make(final Asked a) {
        final List<Built> made = new ArrayList<>();
        final List<ModelView.Model> drawn = new ArrayList<>();
        for (final Standing t : towers) {
            final Built b = t.built();
            made.add(b);
            final List<Solid> still = new ArrayList<>(b.drawn());
            still.addAll(t.tower().landingsShut());
            drawn.add(ModelView.Model.of(t.placed(still)));
        }
        return new Made(made, drawn);
    }

    /** On the screen's thread: the towers shown, looked at from over the middle of them; played at once if asked. */
    private void show(final Made m) {
        built = m.built();
        view.show(m.models());
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
        view.lookAt(x, 10_000, z);
        view.from("bay", 200, 18, 520);
        view.zoom(300, 300_000);
        if (playWhenShown) {
            playWhenShown = false;
            player.play();
        }
    }

    /** Level ground round the towers, a kilometre each way. */
    private void ground() {
        double x = 0;
        double z = 0;
        double h = 0;
        for (final Standing t : towers) {
            x += t.at().x() / towers.size();
            z += t.at().z() / towers.size();
            h += t.at().height() / towers.size();
        }
        view.ground(x, h, z, 100_000);
    }
}
