package com.example.fxsuite.avatar.buildings;

import com.example.fxsuite.avatar.bench.kit.Bus;
import com.example.fxsuite.avatar.bench.kit.ModelView;
import com.example.fxsuite.avatar.bench.kit.Player;
import com.example.fxsuite.avatar.bench.kit.Shell;
import com.example.fxsuite.avatar.bench.kit.Workshop;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.lifts.Built;
import com.example.fxsuite.avatar.lifts.Knob;
import com.example.fxsuite.avatar.lifts.Mover;
import com.example.fxsuite.avatar.lifts.SceneWorld;

import javafx.application.Application;
import javafx.geometry.Insets;
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
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The buildings lab's bench: an office tower made from a few figures ({@link OfficeTower#generic}), so wide and deep,
 * its corners cut and notched, so many storeys so high over a lobby so high, its office floors in so many zones each
 * with its bank of lifts, the higher faster; cut away over any floor to be looked into, its glass shown or not; still,
 * a model to look at, its lifts' cars and doors where they stand at the start; its figures read out. Played, to walk
 * into it and ride its lifts: what moves is seen there, not here. Put together of the bench kit's parts.
 *
 * <p>Or a real tower, described from its figures ({@link #TOWERS}): its own size and lifts, shown as the knobs say.
 *
 * <p>{@code -Dtower.model="MBFC Tower 1"} which tower; {@code -Dtower.set=floors=40,zones=3} its knobs;
 * {@code -Dtower.yaw}, {@code -Dtower.pitch}, {@code -Dtower.distance} (m) where it is seen from; {@code -Dbench.play=true}
 * plays it at once; {@code -Dbench.snapshot=out.png} saves the window after {@code -Dbench.settle} ms and exits.
 */
public final class TowerBench extends Application {

    /** What can be set about the tower. */
    static final List<Knob> KNOBS = List.of(
            new Knob("width", "Width", "m", 30, 70, 0.5, 52),
            new Knob("depth", "Depth", "m", 30, 70, 0.5, 52),
            new Knob("cut", "Corners cut", "m", 0, 12, 0.5, 8),
            new Knob("notch", "Corner notched", "m", 0, 12, 0.5, 6),
            new Knob("lobby", "Lobby height", "m", 4, 12, 0.1, 9.5),
            new Knob("floors", "Floors over the lobby", "", 10, 60, 1, 32),
            new Knob("storey", "Storey", "m", 3.6, 5, 0.05, 4.65),
            new Knob("zones", "Lift zones", "", 1, 5, 1, 2),
            new Knob("cars", "Cars a zone", "", 2, 8, 1, 8),
            new Knob("fastest", "Fastest lift", "m/s", 2.5, 10, 0.1, 6),
            new Knob("upto", "Shown up to floor", "", 0, 60, 1, 60),
            new Knob("glass", "Glass (1 shows it)", "", 0, 1, 1, 1));

    private final Bus bus = new Bus();
    private final Shell shell = new Shell("Buildings lab: office tower", bus);
    private final ModelView view = new ModelView();
    private final Map<String, Double> set = new HashMap<>();
    private Built shown;
    private final Player player = new Player(() -> shown == null ? null : new SceneWorld(shown), List.of());
    private boolean framed;
    private boolean playWhenShown = Boolean.getBoolean("bench.play");

    /** The towers to choose from: the one made from the knobs' figures, and those described from a real one's. */
    static final Map<String, java.util.function.Supplier<OfficeTower>> TOWERS = new java.util.LinkedHashMap<>();

    static {
        TOWERS.put("generic", null);
        TOWERS.put("MBFC Tower 1", Mbfc::tower1);
        TOWERS.put("MBFC Tower 2", Mbfc::tower2);
        TOWERS.put("MBFC Tower 3", Mbfc::tower3);
        TOWERS.put("ORQ North Tower", Orq::north);
        TOWERS.put("ORQ South Tower", Orq::south);
        TOWERS.put("Asia Square Tower 1", AsiaSquare::tower1);
        TOWERS.put("Asia Square Tower 2", AsiaSquare::tower2);
    }

    /** Which tower is chosen; the knobs ({@code set}), for the generic one's figures and how it is shown. */
    private record Asked(String model, Map<String, Double> set) {
    }

    private final ComboBox<String> model = new ComboBox<>();
    private final List<Spinner<Double>> figures = new ArrayList<>();

    /** What the workshop made: the tower as a scene; it drawn, still, its lifts where they stand at the start. */
    private record Made(Built built, ModelView.Model model) {
    }

    private final Workshop<Asked, Made> workshop = new Workshop<>("buildings lab", bus, Asked.class, this::make);

    /** The tower as {@code set} says. */
    static OfficeTower tower(final Map<String, Double> set) {
        return OfficeTower.generic(KNOBS.get(0).of(set) * 100, KNOBS.get(1).of(set) * 100, KNOBS.get(2).of(set) * 100,
                KNOBS.get(3).of(set) * 100, KNOBS.get(4).of(set) * 100, (int) Math.round(KNOBS.get(5).of(set)),
                KNOBS.get(6).of(set) * 100, (int) Math.round(KNOBS.get(7).of(set)),
                (int) Math.round(KNOBS.get(8).of(set)), KNOBS.get(9).of(set) * 100);
    }

    @Override
    public void start(final Stage stage) {
        bus.subscribe(Made.class, this::show, Bus.SCREEN);
        final String asked = System.getProperty("tower.set");
        if (asked != null) {
            for (final String pair : asked.split(",")) {
                final String[] kv = pair.split("=");
                if (kv.length == 2) {
                    set.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
                }
            }
        }
        final Button play = new Button("Play");
        play.setStyle("-fx-font-weight: bold;");
        play.setOnAction(e -> player.play());
        shell.bar(List.of(play));
        final GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        int row = 0;
        for (final Knob k : KNOBS) {
            final Spinner<Double> spinner = new Spinner<>(
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(k.least(), k.most(), k.of(set), k.step()));
            spinner.setEditable(true);
            spinner.setPrefWidth(110);
            spinner.valueProperty().addListener((o, was, now) -> {
                if (now != null) {
                    set.put(k.key(), now);
                    request();
                }
            });
            grid.addRow(row++, new Label(k.label()), spinner, new Label(k.unit()));
            if (!k.key().equals("upto") && !k.key().equals("glass")) {
                figures.add(spinner);
            }
        }
        // which tower: the generic one's figures set by the knobs, a real one's its own
        model.getItems().setAll(TOWERS.keySet());
        final String chosen = System.getProperty("tower.model", "generic");
        model.setValue(TOWERS.containsKey(chosen) ? chosen : "generic");
        model.valueProperty().addListener((o, was, now) -> {
            framed = false;
            request();
        });
        grid.add(new Label("Tower"), 0, row);
        grid.add(model, 1, row, 2, 1);
        final ScrollPane adjusting = new ScrollPane(new VBox(8, grid));
        adjusting.setFitToWidth(true);
        adjusting.setPadding(new Insets(8));
        final VBox playing = new VBox(10, player.panel());
        playing.setPadding(new Insets(8));
        final TabPane tabs = new TabPane(new Tab("Adjust", adjusting), new Tab("Play", playing));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        shell.side(new Label("The tower"), tabs, 420);
        shell.middle(view.node());
        request();
        shell.show(stage, 1440, 900);
    }

    @Override
    public void stop() {
        workshop.close();
    }

    private void request() {
        final boolean generic = TOWERS.get(model.getValue()) == null;
        figures.forEach(s -> s.setDisable(!generic));
        bus.publish(new Asked(model.getValue(), Map.copyOf(set)));
    }

    /** On the workshop's worker: the tower built as chosen and set, drawn. */
    private Made make(final Asked a) {
        final java.util.function.Supplier<OfficeTower> real = TOWERS.get(a.model());
        final OfficeTower tower = real == null ? tower(a.set()) : real.get();
        final int upTo = (int) Math.round(KNOBS.get(10).of(a.set()));
        final Built built = tower.built(upTo, KNOBS.get(11).of(a.set()) > 0.5);
        // cut away over a storey: its floor over it left off, and its lifts' parts over it too
        final double cut = upTo >= tower.storeys() - 1 ? Double.POSITIVE_INFINITY : tower.levels().get(upTo + 1);
        final List<Solid> still = new ArrayList<>(built.drawn());
        for (final Mover m : built.movers()) {
            if (m.route().at(0).height() < cut) {
                still.addAll(m.solidsAt(0));
            }
        }
        return new Made(built, ModelView.Model.of(still));
    }

    /** On the screen's thread: the tower shown in place of what was; looked at afresh the first time. */
    private void show(final Made m) {
        shown = m.built();
        view.show(m.model());
        bus.publish(new Bus.Figures(shown.figures()));
        bus.publish(new Bus.Problems(""));
        if (!framed) {
            framed = true;
            final double[] middle = shown.middle();
            view.lookAt(middle[0], middle[1], middle[2]);
            view.from("tower", shown.yaw(), 20, shown.size() * 1.8 / 100 + 10);
            view.zoom(300, shown.size() * 10);
        }
        if (playWhenShown) {
            playWhenShown = false;
            player.play();
        }
    }
}
