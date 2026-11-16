package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.air.Craft;
import com.example.fxsuite.avatar.air.Crafts;
import com.example.fxsuite.avatar.air.Extent;
import com.example.fxsuite.avatar.air.Livery;
import com.example.fxsuite.avatar.look.CraftModel;
import com.example.fxsuite.avatar.look.Stall;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Locale;

/**
 * The aircraft on a bench, one at a time ({@link Crafts}): made to a size, painted in a livery,
 * its rotors and propellers turning; on a floor ruled every 10 m (every metre, for a drone), a
 * person stood by it to tell how big it is. Drag to turn round it, right-drag to move across it,
 * scroll to come nearer.
 *
 * <p>{@code -Daircraft.pick=heli} (the start of a kind's name) picks what is on show to start with,
 * {@code -Daircraft.livery=bomber} its livery, {@code -Daircraft.size=30} its size in metres;
 * {@code -Daircraft.yaw}, {@code -Daircraft.pitch} and {@code -Daircraft.distance} where it is seen
 * from; {@code -Daircraft.turning=false} holds its rotors still. {@code -Dbench.snapshot=out.png}
 * saves the window after {@code -Dbench.settle} ms and exits.
 */
public final class AircraftBench extends Application {

    private final PerspectiveCamera eye = new PerspectiveCamera(true);
    private final Translate target = new Translate();
    private final Rotate yaw = new Rotate(-140, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(-15, Rotate.X_AXIS);
    private final Translate back = new Translate(0, 0, -5000);
    private final Group shown = new Group();
    private final Group floor = new Group();
    private final Group person = new Group(Stall.person());
    private final ComboBox<Crafts.Kind> kinds = new ComboBox<>();
    private final ComboBox<Livery> liveries = new ComboBox<>();
    private final Spinner<Double> size = new Spinner<>();
    private final CheckBox showSkeleton = new CheckBox("Its skeleton: the lines it is drawn by");
    private final CheckBox showDressed = new CheckBox("Dressed: its skin and its parts");
    private final CheckBox turning = new CheckBox("Its rotors and propellers turning");
    private final Slider power = new Slider(0, 1, 0.35);
    private final CheckBox showFloor = new CheckBox("The floor, ruled every 10 m (every metre, for a small one)");
    private final CheckBox showPerson = new CheckBox(String.format(Locale.ROOT, "A person, %.2f m tall, beside it",
            Stall.PERSON / 100));
    private final Label figures = new Label();
    /** A slider for each control its shape is made by (a styled body's width, its haunch), by name. */
    private final VBox shaping = new VBox(6);
    private final java.util.Map<String, Slider> shapers = new java.util.LinkedHashMap<>();
    /** A slider for each control its hinges turn on (a duct's tilt, a turret's traverse). */
    private final VBox hinged = new VBox(6);
    private CraftModel model;
    private double whole = 5000;
    private double lastX;
    private double lastY;

    @Override
    public void start(final Stage stage) {
        eye.setNearClip(5);
        eye.setFarClip(1_000_000);
        eye.setFieldOfView(40);
        eye.getTransforms().addAll(target, yaw, pitch, back);
        final AmbientLight ambient = new AmbientLight(Color.web("#8e969f"));
        final DirectionalLight sun = new DirectionalLight(Color.web("#a19d94"));
        sun.setDirection(new javafx.geometry.Point3D(0.4, 1, 0.6));
        final Group scene3d = new Group(floor, shown, person, eye, ambient, sun);
        final SubScene view = new SubScene(scene3d, 860, 700, true, SceneAntialiasing.BALANCED);
        view.setFill(Color.web("#c9dbe8"));
        view.setCamera(eye);
        final Pane holder = new Pane(view);
        view.widthProperty().bind(holder.widthProperty());
        view.heightProperty().bind(holder.heightProperty());
        orbit(view);

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
        final java.util.List<java.util.function.Predicate<String>> matches = java.util.List.of(
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
        liveries.setOnAction(e -> rebuild(false));
        size.valueProperty().addListener((o, was, now) -> rebuild(true));
        turning.setSelected(!"false".equalsIgnoreCase(System.getProperty("aircraft.turning")));
        showSkeleton.setSelected(Boolean.getBoolean("aircraft.skeleton"));
        showDressed.setSelected(!"false".equalsIgnoreCase(System.getProperty("aircraft.dressed")));
        showSkeleton.setOnAction(e -> rebuild(false));
        showDressed.setOnAction(e -> rebuild(false));
        showFloor.setSelected(!"false".equalsIgnoreCase(System.getProperty("aircraft.floor")));
        showFloor.setOnAction(e -> floor.setVisible(showFloor.isSelected()));
        floor.setVisible(showFloor.isSelected());
        showPerson.setSelected(!"false".equalsIgnoreCase(System.getProperty("aircraft.person")));
        showPerson.setOnAction(e -> person.setVisible(showPerson.isSelected()));
        person.setVisible(showPerson.isSelected());
        power.setPrefWidth(170);
        figures.setFont(Font.font("Monospaced", 12));
        figures.setWrapText(true);

        final VBox controls = new VBox(10,
                EngineBench.heading("On show"), kinds, liveries,
                EngineBench.label("Two kinds of body, and the parts fixed to them: a fuselage lofted through its"
                        + " sections (an airship's envelope, an aeroplane's, a helicopter's pod and boom, an engine's"
                        + " nacelle); a box with its edges rounded (a drone's body, a boxy helicopter, a gondola);"
                        + " wings, fins, rotors, wheels and struts as solid pieces. An airship is both."),
                new HBox(8, new Label("Size (m)"), size), shaping,
                showDressed, showSkeleton, turning, new HBox(8, new Label("Power"), power), hinged,
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
        root.setCenter(holder);
        root.setStyle("-fx-background-color: #f4f5f7;");
        final Scene scene = new Scene(root, 1260, 860);

        rebuild(true);
        yaw.setAngle(Double.parseDouble(System.getProperty("aircraft.yaw", "-140")));
        pitch.setAngle(-Double.parseDouble(System.getProperty("aircraft.pitch", "15")));
        if (System.getProperty("aircraft.distance") != null) {
            back.setZ(-Double.parseDouble(System.getProperty("aircraft.distance")) * 100);
        }
        new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(final long now) {
                final double seconds = last < 0 ? 0 : Math.min(0.1, (now - last) / 1e9);
                last = now;
                if (model != null && turning.isSelected()) {
                    model.turn(seconds, power.getValue());
                }
            }
        }.start();
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
        final java.util.Map<String, Double> asked = new java.util.HashMap<>();
        for (final String pair : System.getProperty("aircraft.shape", "").split(",")) {
            final String[] kv = pair.split("=");
            if (kv.length == 2) {
                asked.put(kv[0].trim().toLowerCase(Locale.ROOT), Double.parseDouble(kv[1].trim()));
            }
        }
        shapers.clear();
        shaping.getChildren().clear();
        for (final com.example.fxsuite.avatar.air.BodyStyle.Control control : kind.controls()) {
            final Slider slider = new Slider(control.least(), control.most(),
                    Math.clamp(asked.getOrDefault(control.name(), control.usual()), control.least(), control.most()));
            slider.setPrefWidth(170);
            slider.valueChangingProperty().addListener((o, was, now) -> {
                if (!now) {
                    rebuild(false);
                }
            });
            slider.valueProperty().addListener((o, was, now) -> {
                if (!slider.isValueChanging()) {
                    rebuild(false);
                }
            });
            shapers.put(control.name(), slider);
            shaping.getChildren().add(new HBox(8, new Label(Character.toUpperCase(control.name().charAt(0))
                    + control.name().substring(1)), slider));
        }
    }

    /** The size spinner as {@code kind} may be made: from its least to its most, at its usual. */
    private void sized(final Crafts.Kind kind) {
        final double step = kind.most() <= 10 ? 0.1 : 1;
        size.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(kind.least(), kind.most(), kind.usual(),
                step));
        size.setEditable(true);
        size.setPrefWidth(110);
    }

    /** Made again, as the controls say; seen whole again, if {@code frame}d afresh. */
    private void rebuild(final boolean frame) {
        final Crafts.Kind kind = kinds.getValue();
        final double metres = Math.clamp(size.getValue(), kind.least(), kind.most());
        final java.util.Map<String, Double> shape = new java.util.HashMap<>();
        shapers.forEach((control, slider) -> shape.put(control, slider.getValue()));
        final Craft craft = kind.build(metres, shape);
        model = new CraftModel(craft, liveries.getValue(), showDressed.isSelected(), showSkeleton.isSelected(),
                Math.max(0.6, metres * 100 / 700));
        hinges(craft);
        final Extent box = craft.extent();
        // stood on the floor, as it would be parked: its lowest point a hair above it
        final Group placed = new Group(model.node());
        placed.setTranslateY(box.minUp() - 2);
        shown.getChildren().setAll(placed);
        final double length = box.length();
        whole = Math.max(box.height(), Math.max(box.width(), length)) * 1.8 + 300;
        person.setTranslateX(box.maxAcross() + Math.clamp(0.3 * length, 50, 150));
        person.setTranslateZ((box.minAlong() + box.maxAlong()) / 2);
        if (frame) {
            target.setX((box.minAcross() + box.maxAcross()) / 2);
            target.setY(-(box.height()) / 2);
            target.setZ((box.minAlong() + box.maxAlong()) / 2);
            back.setZ(-whole);
        }
        ruleFloor(box);
        figures.setText(String.format(Locale.ROOT,
                "%s%n%.1f m long, %.1f m across, %.1f m high%n%d skins, %d pieces, %d turning%n%,d triangles",
                craft.name(), length / 100, box.width() / 100, box.height() / 100, count(craft, 0), count(craft, 1),
                count(craft, 2), model.triangles()));
    }

    /**
     * A slider for each control {@code craft}'s hinges turn on, from its least to its most, at rest to
     * start with (or as {@code -Daircraft.set=turret traverse=40,turret elevation=-30} says).
     */
    private void hinges(final Craft craft) {
        final java.util.Map<String, Double> asked = new java.util.HashMap<>();
        for (final String pair : System.getProperty("aircraft.set", "").split(",")) {
            final String[] kv = pair.split("=");
            if (kv.length == 2) {
                asked.put(kv[0].trim().toLowerCase(Locale.ROOT), Double.parseDouble(kv[1].trim()));
            }
        }
        hinged.getChildren().clear();
        com.example.fxsuite.avatar.air.Hinge.controls(craft).forEach((control, range) -> {
            final Slider slider = new Slider(range[0], range[1],
                    Math.clamp(asked.getOrDefault(control.toLowerCase(Locale.ROOT), 0.0), range[0], range[1]));
            slider.setPrefWidth(170);
            slider.valueProperty().addListener((o, was, now) -> model.set(control, now.doubleValue()));
            model.set(control, slider.getValue());
            hinged.getChildren().add(new HBox(8, new Label(Character.toUpperCase(control.charAt(0)) + control.substring(1)),
                    slider));
        });
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
    private void ruleFloor(final Extent box) {
        final double rule = Math.max(box.length(), box.width()) < 500 ? 100 : 1000;
        final double reach = Math.ceil((Math.max(box.width(), box.length()) / 2 + 3 * rule) / rule) * rule;
        final Box ground = new Box(reach * 2, 2, reach * 2);
        ground.setTranslateY(1.5);
        ground.setTranslateX((box.minAcross() + box.maxAcross()) / 2);
        ground.setTranslateZ((box.minAlong() + box.maxAlong()) / 2);
        ground.setMaterial(matte("#9fae8f"));
        floor.getChildren().setAll(ground);
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
    }

    /** Dragged, it turns round what is on show; right-dragged, moves across it; scrolled, nearer or further. */
    private void orbit(final SubScene view) {
        view.setOnMousePressed(e -> {
            lastX = e.getSceneX();
            lastY = e.getSceneY();
        });
        view.setOnMouseDragged(e -> {
            final double dx = e.getSceneX() - lastX;
            final double dy = e.getSceneY() - lastY;
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            if (e.getButton() == MouseButton.SECONDARY || e.isShiftDown()) {
                final double scale = -back.getZ() / 900;
                final double turn = Math.toRadians(yaw.getAngle());
                target.setX(target.getX() - (dx * Math.cos(turn)) * scale);
                target.setZ(target.getZ() + (dx * Math.sin(turn)) * scale);
                target.setY(target.getY() - dy * scale);
            } else {
                yaw.setAngle(yaw.getAngle() + dx * 0.4);
                pitch.setAngle(Math.clamp(pitch.getAngle() - dy * 0.4, -89, 89));
            }
        });
        view.setOnScroll(e -> back.setZ(Math.clamp(back.getZ() * Math.pow(1.0015, -e.getDeltaY()), -whole * 4,
                -Math.max(30, whole / 40))));
    }

    private static PhongMaterial matte(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        material.setSpecularColor(Color.web("#3a3f46"));
        return material;
    }
}
