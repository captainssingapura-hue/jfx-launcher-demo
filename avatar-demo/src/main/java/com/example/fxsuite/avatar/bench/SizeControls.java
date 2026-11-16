package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.build.Assembly;
import com.example.fxsuite.avatar.build.Building;
import com.example.fxsuite.avatar.build.Church;
import com.example.fxsuite.avatar.build.Plan;
import com.example.fxsuite.avatar.build.Temple;

import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * How big a building drawn to a size is built: the church's bays, width, walls, ridge, tower, belfry
 * and spire; the temple hall's bays across and deep, its middle bay, columns and platform. Each
 * changed, the building is built again to it and handed on; a size it cannot stand at is said, and
 * the building left as it was. Each building's sizes kept, so it comes back as it was left.
 *
 * <p>{@code -Dbuilding.size=bays=8,width=12} (counts, or metres) sizes them to start with: the
 * church's {@code bays}, {@code bay}, {@code width}, {@code walls}, {@code ridge}, {@code tower},
 * {@code belfry}, {@code spire}; the hall's {@code across}, {@code middle}, {@code deep},
 * {@code columns}, {@code platform}.
 */
final class SizeControls extends VBox {

    /** A building built again to a size: put together, to dress, and its plans. */
    record Built(String name, Assembly.Result assembled, Building building, Map<String, Plan> plans,
                 com.example.fxsuite.avatar.build.Tower tower) {
    }

    /** One of its sizes: what it is called, its key, from and to, by how much, as it is first. */
    private record Measure(String title, String key, double min, double max, double step, double first) {
    }

    private static final String CHURCH = "town church";
    private static final String TEMPLE = "temple hall";
    private static final String DECK = "void deck tower";
    private static final String HOUSE = com.example.fxsuite.avatar.build.EarthHouse.NAME;

    private final Label problem = new Label();
    private final Map<String, List<Spinner<Double>>> kept = new HashMap<>();
    private final Map<String, VBox> panels = new HashMap<>();
    private Consumer<Built> built = b -> {
    };

    SizeControls() {
        super(8);
        problem.setWrapText(true);
        problem.setStyle("-fx-text-fill: #b42318;");
    }

    /** Whether {@code name} is built to a size here. */
    static boolean sized(final String name) {
        return CHURCH.equals(name) || TEMPLE.equals(name) || DECK.equals(name) || HOUSE.equals(name);
    }

    /** Says who to hand each building built again to. */
    void onBuilt(final Consumer<Built> then) {
        built = then;
    }

    /** Its sizes, if {@code name} is built to a size; nothing, if not. */
    void show(final String name) {
        getChildren().clear();
        if (!sized(name)) {
            return;
        }
        final boolean first = !panels.containsKey(name);
        getChildren().add(panels.computeIfAbsent(name, this::panel));
        // sized from the command line: built so at once
        if (first && kept.get(name).stream().anyMatch(s -> s.getProperties().containsKey("asked"))) {
            build(name);
        }
    }

    private VBox panel(final String name) {
        final com.example.fxsuite.avatar.build.EarthHouse.Size house = com.example.fxsuite.avatar.build.EarthHouse.Size.STANDARD;
        final List<Measure> measures = HOUSE.equals(name) ? List.of(
                new Measure("Wide (m)", "width", 3, 14, 0.1, house.width() / 100),
                new Measure("Walls high (m)", "walls", 1.8, 4.5, 0.05, house.walls() / 100),
                new Measure("Walls thick (m)", "thick", 0.25, 1, 0.05, house.thick() / 100),
                new Measure("Roof rises (m)", "rise", 0.4, 6, 0.1, house.rise() / 100),
                new Measure("Eave reaches (m)", "eaves", 0, 1.2, 0.05, house.eaves() / 100),
                new Measure("Door wide (m)", "door", 0.6, 1.8, 0.05, house.door() / 100),
                new Measure("Door high (m)", "doorhigh", 1.4, 4.3, 0.05, house.doorHigh() / 100),
                new Measure("Window wide (m; 0, none)", "window", 0, 1.6, 0.05, house.window() / 100),
                new Measure("Chimney (1, 0 none)", "chimney", 0, 1, 1, house.chimney() ? 1 : 0))
                : DECK.equals(name) ? List.of(
                new Measure("Deck at floor", "floor", 2, com.example.fxsuite.avatar.build.Towers.DECK_TOWER_FLOORS - 3, 1,
                        com.example.fxsuite.avatar.build.Towers.DECK_FLOOR),
                new Measure("Deck high (m)", "deck", 3, 15, 0.2, com.example.fxsuite.avatar.build.Towers.DECK_HEIGHT / 100),
                new Measure("Wing enclosed (bays)", "wing", 0, 5, 1, com.example.fxsuite.avatar.build.Towers.DECK_WING))
                : CHURCH.equals(name) ? List.of(
                new Measure("Bays", "bays", 3, 16, 1, Church.Size.STANDARD.bays()),
                new Measure("Bay long (m)", "bay", 3, 8, 0.5, Church.Size.STANDARD.bay() / 100),
                new Measure("Nave wide (m)", "width", 6, 20, 0.5, Church.Size.STANDARD.width() / 100),
                new Measure("Walls high (m)", "walls", 6, 20, 0.5, Church.Size.STANDARD.eaves() / 100),
                new Measure("Ridge high (m)", "ridge", 8, 35, 0.5, Church.Size.STANDARD.ridge() / 100),
                new Measure("Tower square (m)", "tower", 4, 18, 0.5, Church.Size.STANDARD.tower() / 100),
                new Measure("Belfry's top (m)", "belfry", 12, 70, 1, Church.Size.STANDARD.belfry() / 100),
                new Measure("Spire's tip (m)", "spire", 18, 120, 1, Church.Size.STANDARD.tip() / 100))
                : List.of(
                new Measure("Bays across", "across", 3, 11, 2, Temple.Size.STANDARD.bays()),
                new Measure("Middle bay (m)", "middle", 4, 9, 0.2, Temple.Size.STANDARD.middle() / 100),
                new Measure("Bays deep", "deep", 3, 5, 1, Temple.Size.STANDARD.deep()),
                new Measure("Columns high (m)", "columns", 3, 8, 0.25, Temple.Size.STANDARD.column() / 100),
                new Measure("Platform high (m)", "platform", 0.3, 3, 0.1, Temple.Size.STANDARD.platform() / 100));
        final Map<String, Double> asked = asked();
        final List<Spinner<Double>> spinners = new ArrayList<>();
        final VBox panel = new VBox(6, EngineBench.heading("Its size"),
                EngineBench.label(HOUSE.equals(name)
                        ? "So wide across outside, its walls so high at the foot of its eave and so thick; its roof"
                        + " rising so high over its eave, which reaches so far past its walls; its door so wide and so"
                        + " high (to walk in upright, over 2 m); its window so wide, or none; a chimney or none."
                        : DECK.equals(name)
                        ? "Its void deck: which floor it is, how high, and how many bays of it at its west end are"
                        + " enclosed (none: open all through). Open, its columns stand on the whole grid; round its"
                        + " edge a railing; on it, a pool and plants, as tall as it lets."
                        : CHURCH.equals(name)
                        ? "Its nave so many bays long and so wide, its walls and ridge so high; its tower so square,"
                        + " its belfry and spire so high. Its apse as wide as its nave; its windows as tall as its"
                        + " walls allow, as wide as ever, and its doorway a person's size, whatever its size."
                        : "So many bays across, the middle one widest, those out from it narrower; so many deep;"
                        + " its columns and platform so high. Its bracket sets and eaves as they are; its roof as"
                        + " steep."));
        for (final Measure m : measures) {
            final SpinnerValueFactory.DoubleSpinnerValueFactory values =
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(m.min(), m.max(), m.first(), m.step());
            values.setConverter(new StringConverter<>() {
                @Override
                public String toString(final Double value) {
                    return value == null ? "" : String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.?0+$", "");
                }

                @Override
                public Double fromString(final String text) {
                    try {
                        return Double.parseDouble(text.trim());
                    } catch (NumberFormatException e) {
                        return values.getValue();
                    }
                }
            });
            final Spinner<Double> spinner = new Spinner<>(values);
            spinner.setEditable(true);
            spinner.setPrefWidth(100);
            if (asked.containsKey(m.key())) {
                values.setValue(asked.get(m.key()));
                spinner.getProperties().put("asked", true);
            }
            values.valueProperty().addListener((seen, was, now) -> build(name));
            spinners.add(spinner);
            panel.getChildren().add(EngineBench.row(m.title(), spinner));
        }
        panel.getChildren().add(problem);
        kept.put(name, spinners);
        return panel;
    }

    /** Built again to its sizes as they are now, and handed on; or what is wrong with them, said. */
    private void build(final String name) {
        final List<Double> v = kept.get(name).stream().map(Spinner::getValue).toList();
        try {
            if (HOUSE.equals(name)) {
                final com.example.fxsuite.avatar.build.EarthHouse house = com.example.fxsuite.avatar.build.EarthHouse.of(
                        new com.example.fxsuite.avatar.build.EarthHouse.Size(v.get(0) * 100, v.get(1) * 100,
                                v.get(2) * 100, v.get(3) * 100, v.get(4) * 100, v.get(5) * 100, v.get(6) * 100,
                                v.get(7) * 100, v.get(8) >= 0.5));
                problem.setText("");
                built.accept(new Built(name, null, house.building(), null, null));
            } else if (DECK.equals(name)) {
                final com.example.fxsuite.avatar.build.Tower tower = com.example.fxsuite.avatar.build.Towers.voidDeck(
                        (int) Math.round(v.get(0)), v.get(1) * 100, (int) Math.round(v.get(2)));
                problem.setText("");
                built.accept(new Built(name, null, BuildingBench.dressed(tower), null, tower));
            } else if (CHURCH.equals(name)) {
                final Church church = Church.of(new Church.Size((int) Math.round(v.get(0)), v.get(1) * 100, v.get(2) * 100,
                        v.get(3) * 100, v.get(4) * 100, v.get(5) * 100, v.get(6) * 100, v.get(7) * 100));
                problem.setText("");
                built.accept(new Built(name, church.assembled(), church.building(), church.plans(), null));
            } else {
                final Temple temple = Temple.of(new Temple.Size((int) Math.round(v.get(0)), v.get(1) * 100,
                        (int) Math.round(v.get(2)), v.get(3) * 100, v.get(4) * 100));
                problem.setText("");
                built.accept(new Built(name, temple.assembled(), temple.building(), temple.plans(), null));
            }
        } catch (IllegalArgumentException e) {
            problem.setText("Cannot be built so: " + e.getMessage().replaceFirst("^an? (church|temple hall|void deck|earth house) ", "")
                    + ". Shown as it was.");
        }
    }

    /** The sizes asked for on the command line ({@code -Dbuilding.size=bays=8,width=12}), by key. */
    private static Map<String, Double> asked() {
        final Map<String, Double> asked = new LinkedHashMap<>();
        for (final String pair : System.getProperty("building.size", "").split(",")) {
            final String[] kv = pair.split("=");
            if (kv.length == 2) {
                try {
                    asked.put(kv[0].trim().toLowerCase(Locale.ROOT), Double.parseDouble(kv[1].trim()));
                } catch (NumberFormatException ignored) {
                    // not a number: left as it is
                }
            }
        }
        return asked;
    }
}
