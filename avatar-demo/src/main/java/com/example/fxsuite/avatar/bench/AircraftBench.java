package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.air.Craft;
import com.example.fxsuite.avatar.air.Crafts;
import com.example.fxsuite.avatar.air.Extent;
import com.example.fxsuite.avatar.air.Livery;
import com.example.fxsuite.avatar.bench.kit.ModelView;
import com.example.fxsuite.avatar.bench.kit.Orbit;
import com.example.fxsuite.avatar.look.CraftModel;
import com.example.fxsuite.avatar.look.Stall;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The aircraft on a bench, one at a time ({@link Crafts}): made to a size, painted in a livery, posed on its hinges
 * (a duct tilted, a turret traversed, a door open); still, a model to look at, its rotors and propellers where they
 * stand, nothing in it moving; on a floor ruled every 10 m (every metre, for a drone), a person stood by it to tell how
 * big it is. Seen through the bench kit's model view ({@link ModelView}): drag to turn round it, right-drag to move
 * across it, scroll to come nearer. Flown, it is the game's.
 *
 * <p>{@code -Daircraft.pick=heli} (the start of a kind's name) picks what is on show to start with,
 * {@code -Daircraft.livery=bomber} its livery, {@code -Daircraft.size=30} its size in metres,
 * {@code -Daircraft.set=turret traverse=40,turret elevation=-30} its hinges' pose;
 * {@code -Daircraft.yaw}, {@code -Daircraft.pitch} and {@code -Daircraft.distance} where it is seen
 * from. {@code -Dbench.snapshot=out.png} saves the window after {@code -Dbench.settle} ms and exits.
 */
public final class AircraftBench extends Application {

    private final ModelView view = new ModelView(new Orbit(Color.web("#c9dbe8"), Color.web("#8e969f"),
            Color.web("#a19d94"), new javafx.geometry.Point3D(0.4, 1, 0.6), 5, 1_000_000));
    private final ComboBox<Crafts.Kind> kinds = new ComboBox<>();
    private final ComboBox<Livery> liveries = new ComboBox<>();
    private final Spinner<Double> size = new Spinner<>();
    private final CheckBox showSkeleton = new CheckBox("Its skeleton: the lines it is drawn by");
    private final CheckBox showDressed = new CheckBox("Dressed: its skin and its parts");
    private final CheckBox showFloor = new CheckBox("The floor, ruled every 10 m (every metre, for a small one)");
    private final CheckBox showPerson = new CheckBox(String.format(Locale.ROOT, "A person, %.2f m tall, beside it",
            Stall.PERSON / 100));
    private final Label figures = new Label();
    /** A slider for each control its shape is made by (a styled body's width, its haunch), by name. */
    private final VBox shaping = new VBox(6);
    private final java.util.Map<String, Slider> shapers = new java.util.LinkedHashMap<>();
    /** A slider for each control its hinges turn on (a duct's tilt, a turret's traverse): where it is posed. */
    private final VBox hinged = new VBox(6);
    private final java.util.Map<String, Slider> hingeSliders = new java.util.LinkedHashMap<>();
    /** The aircraft as made: what it is, not how it is drawn. */
    private Craft craft;
    /** How thick its skeleton's lines are drawn, cm: by its size. */
    private double thickness;

    @Override
    public void start(final Stage stage) {
        kinds.getItems().setAll(Crafts.Kind.values());
        kinds.setConverter(new StringConverter<>() {
            @Override
            public String toString(final Crafts.Kind kind) {
                return kind == null ? "" : kind.title();
            }

            @Override
            public Crafts.Kind fromString(final String name) {
                return null;
            }
        });
        liveries.getItems().setAll(Livery.all());
        liveries.setConverter(new StringConverter<>() {
            @Override
            public String toString(final Livery livery) {
                return livery == null ? "" : livery.name();
            }

            @Override
            public Livery fromString(final String name) {
                return null;
            }
        });
        final String wanted = System.getProperty("aircraft.pick", "").toLowerCase(Locale.ROOT);
        // its whole title first, then the start of it, then the start of any word in it
        final List<java.util.function.Predicate<String>> matches = List.of(
                title -> title.equals(wanted), title -> title.startsWith(wanted), title -> title.contains(" " + wanted));
        kinds.setValue(matches.stream().flatMap(match -> java.util.Arrays.stream(Crafts.Kind.values())
                        .filter(k -> !wanted.isEmpty() && match.test(k.title().toLowerCase(Locale.ROOT))))
                .findFirst().orElse(Crafts.Kind.AIRSHIP));
        final String paint = System.getProperty("aircraft.livery", "").toLowerCase(Locale.ROOT);
        liveries.setValue(Livery.all().stream()
                .filter(l -> !paint.isEmpty() && l.name().toLowerCase(Locale.ROOT).startsWith(paint)).findFirst()
                .orElse(usualLivery(kinds.getValue())));
        sized(kinds.getValue());
        shapers(kinds.getValue());
        if (System.getProperty("aircraft.size") != null) {
            size.getValueFactory().setValue(Double.parseDouble(System.getProperty("aircraft.size")));
        }
        kinds.setOnAction(e -> {
            sized(kinds.getValue());
            shapers(kinds.getValue());
            liveries.setValue(usualLivery(kinds.getValue()));
            rebuild(true);
        });
        liveries.setOnAction(e -> pose());
        size.valueProperty().addListener((o, was, now) -> rebuild(true));
        showSkeleton.setSelected(Boolean.getBoolean("aircraft.skeleton"));
        showDressed.setSelected(!"false".equalsIgnoreCase(System.getProperty("aircraft.dressed")));
        showSkeleton.setOnAction(e -> pose());
        showDressed.setOnAction(e -> pose());
        showFloor.setSelected(!"false".equalsIgnoreCase(System.getProperty("aircraft.floor")));
        showFloor.setOnAction(e -> pose());
        showPerson.setSelected(!"false".equalsIgnoreCase(System.getProperty("aircraft.person")));
        showPerson.setOnAction(e -> pose());
        figures.setFont(Font.font("Monospaced", 12));
        figures.setWrapText(true);

        final VBox controls = new VBox(10,
                EngineBench.heading("On show"), kinds, liveries,
                EngineBench.label("Two kinds of body, and the parts fixed to them: a fuselage lofted through its"
                        + " sections (an airship's envelope, an aeroplane's, a helicopter's pod and boom, an engine's"
                        + " nacelle); a box with its edges rounded (a drone's body, a boxy helicopter, a gondola);"
                        + " wings, fins, rotors, wheels and struts as solid pieces. An airship is both."),
                new HBox(8, new Label("Size (m)"), size), shaping,
                showDressed, showSkeleton, EngineBench.label("Posed on its hinges:"), hinged,
                showFloor, showPerson,
                EngineBench.label("Drag to turn round it; right-drag (or Shift-drag) to move across it; scroll to"
                        + " come nearer."),
                EngineBench.heading("Its figures"), figures);
        controls.setPadding(new Insets(14));
        controls.setPrefWidth(370);
        final ScrollPane scroll = new ScrollPane(controls);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(390);
        scroll.setMinWidth(390);
        final BorderPane root = new BorderPane();
        root.setLeft(scroll);
        root.setCenter(view.node());
        root.setStyle("-fx-background-color: #f4f5f7;");
        final Scene scene = new Scene(root, 1260, 860);

        rebuild(true);
        view.turned(Double.parseDouble(System.getProperty("aircraft.yaw", "-140")),
                Double.parseDouble(System.getProperty("aircraft.pitch", "15")));
        if (System.getProperty("aircraft.distance") != null) {
            view.distance(Double.parseDouble(System.getProperty("aircraft.distance")) * 100);
        }
        stage.setTitle("Aircraft bench");
        stage.setScene(scene);
        stage.show();
        SoundBench.snapshotIfAsked(scene);
    }

    /** What {@code kind} is painted in to start with: a bomber as one, a gunship olive drab, the rest as a patrol's. */
    private static Livery usualLivery(final Crafts.Kind kind) {
        return kind == Crafts.Kind.BOMBER ? Livery.BOMBER
                : kind.name().startsWith("GUNSHIP") ? Livery.OLIVE : Livery.PATROL;
    }

    /**
     * A slider for each control {@code kind}'s shape is made by, from its least to its most, as drawn to
     * start with (or as {@code -Daircraft.shape=width=1.2,haunch=0.02} says); each change makes it again.
     */
    private void shapers(final Crafts.Kind kind) {
        final java.util.Map<String, Double> asked = asked("aircraft.shape");
        shapers.clear();
        shaping.getChildren().clear();
        for (final com.example.fxsuite.avatar.air.BodyStyle.Control control : kind.controls()) {
            final Slider slider = new Slider(control.least(), control.most(),
                    Math.clamp(asked.getOrDefault(control.name(), control.usual()), control.least(), control.most()));
            slider.setPrefWidth(170);
            whenSet(slider, () -> rebuild(false));
            shapers.put(control.name(), slider);
            shaping.getChildren().add(new HBox(8, new Label(Character.toUpperCase(control.name().charAt(0))
                    + control.name().substring(1)), slider));
        }
    }

    /** {@code then} done when {@code slider} is set: let go after dragging, or moved otherwise; not while dragged. */
    private static void whenSet(final Slider slider, final Runnable then) {
        slider.valueChangingProperty().addListener((o, was, now) -> {
            if (!now) {
                then.run();
            }
        });
        slider.valueProperty().addListener((o, was, now) -> {
            if (!slider.isValueChanging()) {
                then.run();
            }
        });
    }

    /** {@code -D<property>=name=value,...}, by name in lower case. */
    private static java.util.Map<String, Double> asked(final String property) {
        final java.util.Map<String, Double> asked = new java.util.HashMap<>();
        for (final String pair : System.getProperty(property, "").split(",")) {
            final String[] kv = pair.split("=");
            if (kv.length == 2) {
                asked.put(kv[0].trim().toLowerCase(Locale.ROOT), Double.parseDouble(kv[1].trim()));
            }
        }
        return asked;
    }

    /** The size spinner as {@code kind} may be made: from its least to its most, at its usual. */
    private void sized(final Crafts.Kind kind) {
        final double step = kind.most() <= 10 ? 0.1 : 1;
        size.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(kind.least(), kind.most(), kind.usual(),
                step));
        size.setEditable(true);
        size.setPrefWidth(110);
    }

    /** Made again, as the controls say, and drawn; seen whole again, if {@code frame}d afresh. */
    private void rebuild(final boolean frame) {
        final Crafts.Kind kind = kinds.getValue();
        final double metres = Math.clamp(size.getValue(), kind.least(), kind.most());
        final java.util.Map<String, Double> shape = new java.util.HashMap<>();
        shapers.forEach((control, slider) -> shape.put(control, slider.getValue()));
        craft = kind.build(metres, shape);
        thickness = Math.max(0.6, metres * 100 / 700);
        hinges(craft);
        final Extent box = craft.extent();
        final double whole = Math.max(box.height(), Math.max(box.width(), box.length())) * 1.8 + 300;
        view.zoom(Math.max(30, whole / 40), whole * 4);
        if (frame) {
            view.lookAt((box.minAcross() + box.maxAcross()) / 2, box.height() / 2,
                    (box.minAlong() + box.maxAlong()) / 2);
            view.distance(whole);
        }
        pose();
    }

    /**
     * A slider for each control {@code craft}'s hinges turn on, from its least to its most: where it stood, if it was
     * here before (made again, as it is resized), or at rest to start with, or as {@code -Daircraft.set} says.
     */
    private void hinges(final Craft craft) {
        final java.util.Map<String, Double> asked = asked("aircraft.set");
        final java.util.Map<String, Double> at = new java.util.HashMap<>();
        hingeSliders.forEach((control, slider) -> at.put(control, slider.getValue()));
        hingeSliders.clear();
        hinged.getChildren().clear();
        com.example.fxsuite.avatar.air.Hinge.controls(craft).forEach((control, range) -> {
            final double start = at.getOrDefault(control,
                    Math.clamp(asked.getOrDefault(control.toLowerCase(Locale.ROOT), 0.0), range[0], range[1]));
            final Slider slider = new Slider(range[0], range[1], start);
            slider.setPrefWidth(170);
            whenSet(slider, this::pose);
            hingeSliders.put(control, slider);
            hinged.getChildren().add(new HBox(8, new Label(Character.toUpperCase(control.charAt(0))
                    + control.substring(1)), slider));
        });
    }

    /**
     * The aircraft drawn afresh as it is posed and painted, stood on the floor as it would be parked, with the floor
     * and the person if they are shown: the view's models in place of what was.
     */
    private void pose() {
        if (craft == null) {
            return;
        }
        final Extent box = craft.extent();
        final double length = box.length();
        final CraftModel model = new CraftModel(craft, liveries.getValue(), showDressed.isSelected(),
                showSkeleton.isSelected(), thickness);
        hingeSliders.forEach((control, slider) -> model.set(control, slider.getValue()));
        // stood on the floor, as it would be parked: its lowest point a hair above it
        final Group placed = new Group(model.node());
        placed.setTranslateY(box.minUp() - 2);
        final List<ModelView.Model> shown = new ArrayList<>(List.of(ModelView.Model.of(placed)));
        if (showFloor.isSelected()) {
            shown.add(ModelView.Model.of(ruledFloor(box)));
        }
        if (showPerson.isSelected()) {
            final Node person = Stall.person();
            person.setTranslateX(box.maxAcross() + Math.clamp(0.3 * length, 50, 150));
            person.setTranslateZ((box.minAlong() + box.maxAlong()) / 2);
            shown.add(ModelView.Model.of(new Group(person)));
        }
        view.show(shown);
        figures.setText(String.format(Locale.ROOT,
                "%s%n%.1f m long, %.1f m across, %.1f m high%n%d skins, %d pieces, %d turning%n%,d triangles",
                craft.name(), length / 100, box.width() / 100, box.height() / 100, count(craft, 0), count(craft, 1),
                count(craft, 2), model.triangles()));
    }

    /** How many skins (0), pieces (1) or things turning (2) {@code craft} has, those on its hinges too. */
    private static int count(final Craft craft, final int what) {
        int n = switch (what) {
            case 0 -> craft.skins().size() + craft.spinners().stream().mapToInt(s -> s.skins().size()).sum();
            case 1 -> craft.pieces().size() + craft.spinners().stream().mapToInt(s -> s.pieces().size()).sum();
            default -> craft.spinners().size();
        };
        for (final com.example.fxsuite.avatar.air.Hinge hinge : craft.hinges()) {
            n += count(hinge.part(), what);
        }
        return n;
    }

    /** The floor under it, well beyond it all round, ruled every 10 m; every metre, for something small. */
    private static Group ruledFloor(final Extent box) {
        final double rule = Math.max(box.length(), box.width()) < 500 ? 100 : 1000;
        final double reach = Math.ceil((Math.max(box.width(), box.length()) / 2 + 3 * rule) / rule) * rule;
        final Box ground = new Box(reach * 2, 2, reach * 2);
        ground.setTranslateY(1.5);
        ground.setTranslateX((box.minAcross() + box.maxAcross()) / 2);
        ground.setTranslateZ((box.minAlong() + box.maxAlong()) / 2);
        ground.setMaterial(matte("#9fae8f"));
        final Group floor = new Group(ground);
        final PhongMaterial line = matte("#7d8b70");
        for (double at = -reach; at <= reach + 1e-6; at += rule) {
            final Box across = new Box(reach * 2, 1, rule / 80);
            across.setTranslateZ(at + ground.getTranslateZ());
            across.setTranslateX(ground.getTranslateX());
            final Box along = new Box(rule / 80, 1, reach * 2);
            along.setTranslateX(at + ground.getTranslateX());
            along.setTranslateZ(ground.getTranslateZ());
            across.setMaterial(line);
            along.setMaterial(line);
            floor.getChildren().addAll(across, along);
        }
        return floor;
    }

    private static PhongMaterial matte(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        material.setSpecularColor(Color.web("#3a3f46"));
        return material;
    }
}
