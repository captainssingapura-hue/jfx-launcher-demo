package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.viewer.EstateLayout.Edge;
import com.example.fxsuite.viewer.EstateLayout.Placed;
import com.example.fxsuite.viewer.Highlighter.Highlight;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * The rendering engine both viewers share: draws an {@link EstateLayout} in 3D, lets the
 * user orbit, pan and zoom, and highlights what a selected release resolves to.
 *
 * <p>Each release is a slate: a lit block with a face on its front and another on its back,
 * so it reads from either side, and drawn bigger the bigger its artifact, on a log scale. The
 * base layer, releases with no internal dependencies, is at the back; every other release
 * sits one layer in front of its deepest dependency. Declared dependencies are dim lines. Selecting a release lights the graph its
 * resolution uses, dims the rest, and draws in amber the declared dependencies resolution
 * raised past.
 *
 * <p>The estate stays still and the camera moves, as an {@link Orbit} around a focus point:
 * drag to turn around it, right-drag to pan, scroll to zoom toward the cursor. Selecting a
 * release frames its graph. Keys: 1 angled, 2 front, 3 side, 4 top, 5 behind, F frames the
 * selection, R resets. Every change of view glides rather than jumps.
 *
 * <p>Tab (or W) switches to flying, as a spectator does: drag to look, W A S D to move along
 * the view, Space and Ctrl to rise and sink, Shift to go faster, the wheel to change speed.
 * Tab goes back to orbiting the point ahead; F, R and the preset views also return to
 * orbiting. Keys act only while the 3D view has focus, which a click in it gives. A ground
 * grid under the estate keeps up and down clear in either mode.
 */
public final class DependencyView {

    private static final double CARD_WIDTH = 204;
    private static final double CARD_HEIGHT = 60;
    private static final double SLATE_DEPTH = 16;
    /** How far a face stands off its slate, so the depth test never mixes the two. */
    private static final double FACE_GAP = 1.5;
    /** Room for the biggest slates, which are drawn at {@link LogScale#LARGEST} times the card. */
    private static final double COLUMN = CARD_WIDTH * LogScale.LARGEST + 30;
    private static final double ROW = CARD_HEIGHT * LogScale.LARGEST + 24;
    private static final double LAYER = 360;

    private static final double FIELD_OF_VIEW = 35;
    /** Looking down on the estate from the front left. */
    private static final double START_PITCH = -22;
    /** -Destate.yaw=<degrees> starts from another heading, such as 208 to look from behind. */
    private static final double START_YAW = Double.parseDouble(System.getProperty("estate.yaw", "28"));
    private static final long GLIDE_NANOS = 450_000_000L;

    private final VersionText text = new VersionText();
    private final SecurityMarks security;
    private final SizeProvider sizes;
    private final LogScale scale = new LogScale();
    private final Group content = new Group();
    private final Pane node;

    /** A release's slate: what is restyled when the selection changes. */
    private record Slate(Placed placed, PhongMaterial body, List<Region> faces) {
    }

    private final Map<Version<?>, Slate> cards = new HashMap<>();
    private final Map<Edge, Cylinder> edges = new LinkedHashMap<>();

    private final PhongMaterial edgeIdle = material("#5d6573");
    private final PhongMaterial edgeDim = material("#2b2f37");
    private final PhongMaterial edgeLit = material("#eef1f5");
    private final PhongMaterial edgeRaised = material("#f0b429");

    /** The camera and the light it carries, placed by {@link #orbit}. */
    private final Translate rigAt = new Translate();
    private final Rotate rigYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rigPitch = new Rotate(0, Rotate.X_AXIS);
    private final SubScene view;
    private Orbit orbit = new Orbit(Point3D.ZERO, 2300, START_YAW, START_PITCH);
    private AnimationTimer glide;
    /** Where the layout's centre sits: the world origin. */
    private Point3D offset = Point3D.ZERO;
    private Version<?> selected;

    private enum Mode { ORBIT, FLY }

    private Mode mode = Mode.ORBIT;
    private FlyCamera fly = FlyCamera.from(orbit);
    private Velocity velocity = Velocity.STILL;
    /** Flying speed, world units per second; Shift goes four times faster. */
    private double cruise = 360;
    private final java.util.Set<KeyCode> held = java.util.EnumSet.noneOf(KeyCode.class);
    private AnimationTimer flight;
    /** Where flying may go: a box around the estate, above the ground. */
    private Point3D roamLow = new Point3D(-5000, -5000, -5000);
    private Point3D roamHigh = new Point3D(5000, 5000, 5000);
    private double groundY;
    private final Label hud = new Label();
    private final Label crosshair = new Label("+");
    private final javafx.scene.control.Button modeButton = new javafx.scene.control.Button();

    private EstateLayout layout = new EstateLayout(List.of());
    private Consumer<Highlight> onSelect = highlight -> { };
    private Runnable onClear = () -> { };
    private Runnable onBackground = this::clear;
    private double lastX;
    private double lastY;

    /**
     * @param security which releases to flag for known vulnerabilities
     * @param sizes    how big each release's artifact is, which sets the size of its slate
     */
    DependencyView(final SecurityMarks security, final SizeProvider sizes) {
        this.security = security;
        this.sizes = sizes;
        final Group world = new Group(content);

        // soft light from above and to the left of the camera, carried with it
        final AmbientLight ambient = new AmbientLight(Color.web("#a9adb4"));
        final PointLight key = new PointLight(Color.web("#6e6e6e"));
        key.getTransforms().add(new Translate(-700, -900, -200));

        final PerspectiveCamera eye = new PerspectiveCamera(true);
        // a near clip well away from zero keeps depth precise enough, even across the whole
        // estate, for a face to stay in front of its own slate
        eye.setNearClip(20);
        eye.setFarClip(60_000);
        eye.setFieldOfView(FIELD_OF_VIEW);
        final Group rig = new Group(eye, key);
        rig.getTransforms().addAll(rigAt, rigYaw, rigPitch);

        view = new SubScene(new Group(world, ambient, rig), 0, 0, true, SceneAntialiasing.BALANCED);
        view.setFill(Color.web("#15171c"));
        view.setCamera(eye);
        hud.getStyleClass().add("hud");
        hud.setMouseTransparent(true);
        modeButton.getStyleClass().add("hud-button");
        modeButton.setFocusTraversable(false);
        final VBox overlay = new VBox(6, modeButton, hud);
        overlay.setPickOnBounds(false);
        overlay.relocate(14, 12);
        crosshair.getStyleClass().add("crosshair");
        crosshair.setMouseTransparent(true);

        node = new Pane(view, overlay, crosshair);
        view.widthProperty().bind(node.widthProperty());
        view.heightProperty().bind(node.heightProperty());
        crosshair.layoutXProperty().bind(node.widthProperty().subtract(crosshair.widthProperty()).divide(2));
        crosshair.layoutYProperty().bind(node.heightProperty().subtract(crosshair.heightProperty()).divide(2));
        node.setFocusTraversable(true);
        modeButton.setOnAction(e -> {
            toggleMode();
            node.requestFocus();
        });
        node.focusedProperty().addListener((obs, was, is) -> {
            if (!is) {
                held.clear();
            }
        });
        orbitAndZoom(view);
        keys();
        updateHud();
    }

    /** The node to place in a window. */
    public Node node() {
        return node;
    }

    public EstateLayout layout() {
        return layout;
    }

    /** Called with the highlight whenever a release is selected. */
    public void setOnSelect(final Consumer<Highlight> onSelect) {
        this.onSelect = onSelect;
    }

    /** Called whenever the selection is cleared. */
    public void setOnClear(final Runnable onClear) {
        this.onClear = onClear;
    }

    /** What a click on empty space or Esc does. Clears the selection unless replaced. */
    public void setOnBackground(final Runnable onBackground) {
        this.onBackground = onBackground;
    }

    /** Gives the 3D view keyboard focus once the window is up, so its keys work straight away. */
    public void installKeys(final Scene scene) {
        javafx.application.Platform.runLater(node::requestFocus);
    }

    /**
     * Keys act on the 3D view only while it has focus, so typing in the side panel never moves
     * the camera. Esc does the background action; R resets; F frames the selection; 1 to 5 are
     * preset views; 6 stands at ground level in front, flying; Tab switches orbit and fly.
     */
    private void keys() {
        node.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                // otherwise Tab moves focus to the side panel
                toggleMode();
                e.consume();
            }
        });
        node.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> onBackground.run();
                case R -> {
                    toOrbit();
                    resetView();
                }
                case F -> {
                    toOrbit();
                    glideTo(selected == null ? home() : framed(new Highlighter().of(layout, selected).members()));
                }
                case DIGIT1 -> presetView("angled");
                case DIGIT2 -> presetView("front");
                case DIGIT3 -> presetView("side");
                case DIGIT4 -> presetView("top");
                case DIGIT5 -> presetView("behind");
                case DIGIT6 -> toFly(entrance());
                case W, A, S, D, SPACE, CONTROL, SHIFT -> {
                    held.add(e.getCode());
                    if (mode == Mode.ORBIT && List.of(KeyCode.W, KeyCode.A, KeyCode.S, KeyCode.D).contains(e.getCode())) {
                        toFly(FlyCamera.from(orbit));
                    }
                }
                default -> { }
            }
            e.consume();
        });
        node.setOnKeyReleased(e -> held.remove(e.getCode()));
    }

    /** Draws a new set of releases, replacing whatever was shown, and fits the camera to it. */
    public void show(final EstateLayout layout) {
        this.layout = layout;
        cards.clear();
        edges.clear();
        content.getChildren().clear();

        for (final Edge edge : layout.edges()) {
            final Cylinder shape = edge(centre(edge.from()), centre(edge.to()));
            edges.put(edge, shape);
            content.getChildren().add(shape);
        }
        for (final Placed placed : layout.placed()) {
            content.getChildren().add(slate(placed));
        }
        content.getChildren().addAll(layerLabels());
        // nodes added to the 3D sub-scene after the window is shown are not styled on their own
        content.applyCss();

        offset = new Point3D(0, layout.maxRow() * ROW / 2, -layout.maxDepth() * LAYER / 2);
        content.getTransforms().setAll(new Translate(offset.getX(), offset.getY(), offset.getZ()));
        content.getChildren().add(0, ground(layout));
        stopGlide();
        stopFlight();
        mode = Mode.ORBIT;
        place(home());
        clear();
        if ("fly".equals(System.getProperty("estate.mode"))) {
            toFly(entrance());
        }
        updateHud();
    }

    /** Highlights what the release resolves to. Does nothing if it is not shown. */
    public void select(final Version<?> root) {
        if (!cards.containsKey(root)) {
            return;
        }
        final Highlight highlight = new Highlighter().of(layout, root);
        cards.forEach((version, slate) -> restyle(slate, version.equals(root) ? "selected"
                : highlight.members().contains(version) ? "member"
                : highlight.passedOver().contains(version) ? "passed" : "dim"));
        edges.forEach((edge, shape) -> shape.setMaterial(
                highlight.lit().contains(edge) ? edgeLit
                        : highlight.raised().contains(edge) ? edgeRaised : edgeDim));
        selected = root;
        if (mode == Mode.ORBIT) {
            glideTo(framed(highlight.members()));
        }
        onSelect.accept(highlight);
    }

    public void clear() {
        selected = null;
        cards.values().forEach(slate -> restyle(slate, null));
        edges.values().forEach(shape -> shape.setMaterial(edgeIdle));
        onClear.run();
    }

    /** The shown release written as {@code "Ledger 1.20261101.0"}, if there is one. */
    public Optional<Version<?>> find(final String named) {
        return layout.placed().stream().<Version<?>>map(Placed::version)
                .filter(v -> text.named(v).equals(named)).findFirst();
    }

    /** Back to the whole set, from the starting angle. */
    public void resetView() {
        glideTo(home());
    }

    // ---- the camera

    /** The whole set, from the starting view: {@code -Destate.view} if given, else angled. */
    private Orbit home() {
        final Orbit start = preset(System.getProperty("estate.view", "angled"));
        return framed(layout.placed().stream().<Version<?>>map(Placed::version).toList())
                .withAngles(start.yaw(), start.pitch());
    }

    /** The current view turned to a named angle. */
    private Orbit preset(final String name) {
        return switch (name) {
            case "front" -> orbit.withAngles(0, 0);                         // layers stacked like a deck
            case "side" -> orbit.withAngles(-90, 0);                        // depth runs left to right
            case "top" -> orbit.withAngles(0, -Orbit.MAX_PITCH);            // looking straight down
            case "behind" -> orbit.withAngles(START_YAW + 180, START_PITCH);
            default -> orbit.withAngles(START_YAW, START_PITCH);
        };
    }

    /** The given releases filling the view, from the current angle. */
    private Orbit framed(final java.util.Collection<? extends Version<?>> releases) {
        final List<Point3D> points = releases.stream().map(cards::get).filter(java.util.Objects::nonNull)
                .map(slate -> centre(slate.placed()).add(offset)).toList();
        final double width = view.getWidth() > 0 ? view.getWidth() : 1060;
        final double height = view.getHeight() > 0 ? view.getHeight() : 880;
        return new Framing().frame(points, CARD_WIDTH * LogScale.LARGEST / 2, FIELD_OF_VIEW, width / height,
                orbit.yaw(), orbit.pitch());
    }

    /** Puts the camera where the orbit says, at once. */
    private void place(final Orbit next) {
        orbit = next;
        aim(next.position(), next.yaw(), next.pitch());
    }

    private void aim(final Point3D at, final double yaw, final double pitch) {
        rigAt.setX(at.getX());
        rigAt.setY(at.getY());
        rigAt.setZ(at.getZ());
        rigYaw.setAngle(yaw);
        rigPitch.setAngle(pitch);
    }

    // ---- flying

    private void presetView(final String name) {
        toOrbit();
        glideTo(preset(name));
    }

    private void toggleMode() {
        if (mode == Mode.FLY) {
            toOrbit();
        } else {
            toFly(FlyCamera.from(orbit));
        }
    }

    /** Starts flying from the given camera, which is normally the current view, so nothing jumps. */
    private void toFly(final FlyCamera from) {
        stopGlide();
        mode = Mode.FLY;
        fly = from.within(roamLow, roamHigh);
        velocity = Velocity.STILL;
        aim(fly.position(), fly.yaw(), fly.pitch());
        if (flight == null) {
            flight = new AnimationTimer() {
                private long last = -1;

                @Override
                public void handle(final long now) {
                    final double seconds = last < 0 ? 0 : Math.min(0.1, (now - last) / 1e9);
                    last = now;
                    fly(seconds);
                }
            };
            flight.start();
        }
        updateHud();
    }

    /** Back to orbiting, around the point straight ahead at about the estate's distance: the same view. */
    private void toOrbit() {
        if (mode != Mode.FLY) {
            return;
        }
        stopFlight();
        mode = Mode.ORBIT;
        final double ahead = Point3D.ZERO.subtract(fly.position()).dotProduct(fly.forward());
        place(fly.toOrbit(ahead > 300 ? ahead : 800));
        updateHud();
    }

    private void stopFlight() {
        if (flight != null) {
            flight.stop();
            flight = null;
        }
        held.clear();
        velocity = Velocity.STILL;
    }

    /** One frame of flying: ease toward the speed the keys ask for, then move. */
    private void fly(final double seconds) {
        final double speed = cruise * (held.contains(KeyCode.SHIFT) ? 4 : 1);
        final Velocity wanted = new Velocity(
                speed * (axis(KeyCode.W) - axis(KeyCode.S)),
                speed * (axis(KeyCode.D) - axis(KeyCode.A)),
                speed * (axis(KeyCode.SPACE) - axis(KeyCode.CONTROL)));
        velocity = velocity.toward(wanted, seconds);
        if (!velocity.isStill()) {
            fly = fly.moved(velocity.ahead() * seconds, velocity.rightward() * seconds, velocity.upward() * seconds)
                    .within(roamLow, roamHigh);
            aim(fly.position(), fly.yaw(), fly.pitch());
        }
    }

    private int axis(final KeyCode key) {
        return held.contains(key) ? 1 : 0;
    }

    /** Standing on the ground in front of the estate, looking in. */
    private FlyCamera entrance() {
        return new FlyCamera(new Point3D(0, groundY - 150, offset.getZ() - 1100), 0, 6);
    }

    private void updateHud() {
        final boolean flying = mode == Mode.FLY;
        modeButton.setText(flying ? "Orbit" : "Fly");
        crosshair.setVisible(flying);
        hud.setText(flying
                ? "FLYING  \u00B7  speed " + Math.round(cruise) + "/s\n"
                  + "W A S D move \u00B7 Space up \u00B7 Ctrl down \u00B7 Shift faster \u00B7 wheel: speed \u00B7 drag to look"
                  + " \u00B7 Tab: orbit"
                : "ORBIT\n"
                  + "drag turn \u00B7 right-drag pan \u00B7 wheel zoom \u00B7 1-5 views \u00B7 F frame \u00B7 R reset"
                  + " \u00B7 Tab or W: fly \u00B7 6: walk in");
    }

    /**
     * A grid on the ground under the estate, a little below the lowest slates, reaching well
     * beyond it; also sets where flying may go.
     */
    private Group ground(final EstateLayout layout) {
        final double minX = layout.placed().stream().mapToDouble(p -> p.lane() * COLUMN).min().orElse(0) - 1600;
        final double maxX = layout.placed().stream().mapToDouble(p -> p.lane() * COLUMN).max().orElse(0) + 1600;
        final double minZ = -1600;
        final double maxZ = layout.maxDepth() * LAYER + 1600;
        final double y = ROW / 2 + 70;
        final double step = 200;
        final PhongMaterial line = material("#262a31");
        final PhongMaterial major = material("#323741");
        final Group grid = new Group();
        int i = 0;
        for (double x = Math.floor(minX / step) * step; x <= maxX; x += step, i++) {
            final Box l = new Box(2, 1, maxZ - minZ);
            l.setMaterial(i % 5 == 0 ? major : line);
            l.getTransforms().add(new Translate(x, y, (minZ + maxZ) / 2));
            grid.getChildren().add(l);
        }
        i = 0;
        for (double z = Math.floor(minZ / step) * step; z <= maxZ; z += step, i++) {
            final Box l = new Box(maxX - minX, 1, 2);
            l.setMaterial(i % 5 == 0 ? major : line);
            l.getTransforms().add(new Translate((minX + maxX) / 2, y, z));
            grid.getChildren().add(l);
        }
        grid.setMouseTransparent(true);

        // in world terms: the content is shifted by the offset
        groundY = y + offset.getY();
        roamLow = new Point3D(minX - 2500, groundY - 6000, minZ + offset.getZ() - 2500);
        roamHigh = new Point3D(maxX + 2500, groundY - 40, maxZ + offset.getZ() + 2500);
        return grid;
    }

    /** Moves the camera to the orbit smoothly, easing in and out. */
    private void glideTo(final Orbit target) {
        stopGlide();
        final Orbit from = orbit;
        glide = new AnimationTimer() {
            private long start = -1;

            @Override
            public void handle(final long now) {
                if (start < 0) {
                    start = now;
                }
                final double t = Math.min(1, (now - start) / (double) GLIDE_NANOS);
                final double eased = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
                place(from.towards(target, eased));
                if (t >= 1) {
                    stop();
                }
            }
        };
        glide.start();
    }

    private void stopGlide() {
        if (glide != null) {
            glide.stop();
            glide = null;
        }
    }

    // ---- building the scene

    private Point3D centre(final Placed placed) {
        return new Point3D(placed.lane() * COLUMN, -placed.row() * ROW,
                (layout.maxDepth() - placed.depth()) * LAYER);
    }

    /**
     * A release's slate: a block coloured by kind, scaled by the log of its artifact's size,
     * with a face on the front and, turned round, on the back.
     */
    private Group slate(final Placed placed) {
        final PhongMaterial material = new PhongMaterial(kindColour(placed));
        material.setSpecularColor(Color.web("#ffffff22"));
        final Box body = new Box(CARD_WIDTH, CARD_HEIGHT, SLATE_DEPTH);
        body.setMaterial(material);

        final Region front = face(placed);
        front.getTransforms().add(new Translate(-CARD_WIDTH / 2, -CARD_HEIGHT / 2, -SLATE_DEPTH / 2 - FACE_GAP));
        final Region back = face(placed);
        back.getTransforms().addAll(new Translate(-CARD_WIDTH / 2, -CARD_HEIGHT / 2, SLATE_DEPTH / 2 + FACE_GAP),
                new Rotate(180, CARD_WIDTH / 2, CARD_HEIGHT / 2, 0, Rotate.Y_AXIS));

        final Group slate = new Group(body, front, back);
        final double factor = scale.factor(sizes.bytes(placed.version()));
        final Point3D at = centre(placed);
        slate.getTransforms().addAll(new Translate(at.getX(), at.getY(), at.getZ()), new Scale(factor, factor, factor));
        slate.setCursor(Cursor.HAND);
        slate.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.isStillSincePress()) {
                select(placed.version());
            }
            e.consume();
        });
        cards.put(placed.version(), new Slate(placed, material, List.of(front, back)));
        return slate;
    }

    /** One face of a slate: unit, version, and its tags. */
    private Region face(final Placed placed) {
        final Label unit = new Label(placed.unit().getSimpleName());
        unit.getStyleClass().add("unit");
        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox top = new HBox(6, unit, spacer);
        if (placed.isVirtual()) {
            final Label chip = new Label("virtual");
            chip.getStyleClass().add("chip");
            top.getChildren().add(chip);
        }
        final Label number = new Label(text.number(placed.version()));
        number.getStyleClass().add("number");
        final Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        final HBox bottom = new HBox(6, number, gap);

        final VBox face = new VBox(3, top, bottom);
        face.getStyleClass().addAll("card", placed.kind().name().toLowerCase());
        if (placed.isVirtual()) {
            face.getStyleClass().add("virtual");
        }
        if (security.flagged(placed)) {
            final boolean bundle = placed.kind() == EstateLayout.Kind.BUNDLE;
            final Label alert = new Label(bundle ? "\u26A0 CVE" : "\u26A0 insecure");
            alert.getStyleClass().addAll("chip", "alert");
            bottom.getChildren().add(alert);
            face.getStyleClass().add(bundle ? "vulnerable" : "insecure");
        }
        face.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        face.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        face.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        // clicks go through to the block behind
        face.setMouseTransparent(true);
        return face;
    }

    /** Puts a slate in a selection state, or back to normal for {@code null}. */
    private void restyle(final Slate slate, final String state) {
        for (final Region face : slate.faces()) {
            face.getStyleClass().removeAll("selected", "member", "passed", "dim");
            if (state != null) {
                face.getStyleClass().add(state);
            }
            // panels inside the 3D sub-scene do not restyle on their own when their classes change
            face.applyCss();
        }
        slate.body().setDiffuseColor("dim".equals(state) ? Color.web("#24272e")
                : "passed".equals(state) ? Color.web("#4a3e22") : kindColour(slate.placed()));
    }

    private Color kindColour(final Placed placed) {
        return Color.web(switch (placed.kind()) {
            case BUNDLE -> "#2f6f4f";
            case LIBRARY -> "#2b5c8a";
            case APPLICATION -> "#8a3b2b";
        });
    }

    /** A thin cylinder from a to b: JavaFX 3D has no line shape. */
    private Cylinder edge(final Point3D a, final Point3D b) {
        final Point3D along = b.subtract(a);
        final Cylinder shape = new Cylinder(1.6, along.magnitude());
        final Point3D middle = a.midpoint(b);
        final Point3D yAxis = new Point3D(0, 1, 0);
        final double angle = Math.toDegrees(Math.acos(along.normalize().dotProduct(yAxis)));
        shape.getTransforms().addAll(new Translate(middle.getX(), middle.getY(), middle.getZ()),
                new Rotate(-angle, along.crossProduct(yAxis)));
        shape.setMouseTransparent(true);
        return shape;
    }

    /** A small plaque under each layer, readable from the front and from behind, like the slates. */
    private List<Node> layerLabels() {
        final PhongMaterial plate = new PhongMaterial(Color.web("#1d2026"));
        return IntStream.rangeClosed(0, layout.maxDepth()).mapToObj(depth -> {
            final double firstLane = layout.placed().stream().filter(p -> p.depth() == depth)
                    .mapToDouble(Placed::lane).min().orElse(0);
            final String text = depth == 0 ? "depth 0 · no internal dependencies" : "depth " + depth;
            final double width = depth == 0 ? 330 : 110;
            final double height = 30;
            final Box body = new Box(width, height, 4);
            body.setMaterial(plate);
            final Label front = plaqueLabel(text, width, height);
            front.getTransforms().add(new Translate(-width / 2, -height / 2, -2 - FACE_GAP));
            final Label back = plaqueLabel(text, width, height);
            back.getTransforms().addAll(new Translate(-width / 2, -height / 2, 2 + FACE_GAP),
                    new Rotate(180, width / 2, height / 2, 0, Rotate.Y_AXIS));
            final Group plaque = new Group(body, front, back);
            plaque.getTransforms().add(new Translate(firstLane * COLUMN - CARD_WIDTH / 2 + width / 2,
                    ROW / 2 + height / 2, (layout.maxDepth() - depth) * LAYER));
            plaque.setMouseTransparent(true);
            return (Node) plaque;
        }).toList();
    }

    private Label plaqueLabel(final String text, final double width, final double height) {
        final Label label = new Label(text);
        label.getStyleClass().add("layer");
        label.setAlignment(javafx.geometry.Pos.CENTER);
        label.setMinSize(width, height);
        label.setPrefSize(width, height);
        label.setMaxSize(width, height);
        return label;
    }

    // ---- camera

    private void orbitAndZoom(final SubScene view) {
        // world distance per screen pixel on the focus plane, for panning
        final java.util.function.DoubleSupplier perPixel = () ->
                orbit.distance() * Math.tan(Math.toRadians(FIELD_OF_VIEW) / 2) / Math.max(1, view.getHeight() / 2);
        view.setOnMousePressed(e -> {
            node.requestFocus();
            lastX = e.getSceneX();
            lastY = e.getSceneY();
        });
        view.setOnMouseDragged(e -> {
            final double dx = e.getSceneX() - lastX;
            final double dy = e.getSceneY() - lastY;
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            if (mode == Mode.FLY) {
                // look around: mouse right looks right, mouse up looks up
                fly = fly.looked(dx * 0.15, -dy * 0.15);
                aim(fly.position(), fly.yaw(), fly.pitch());
                return;
            }
            stopGlide();
            if (e.getButton() == MouseButton.PRIMARY) {
                // turn around the focus point, as if dragging the estate round
                place(orbit.turned(-dx * 0.25, dy * 0.25));
            } else {
                // move the focus point with the mouse
                place(orbit.panned(-dx * perPixel.getAsDouble(), -dy * perPixel.getAsDouble()));
            }
        });
        view.setOnScroll(e -> {
            if (mode == Mode.FLY) {
                cruise = Math.max(40, Math.min(6000, cruise * (e.getDeltaY() > 0 ? 1.25 : 0.8)));
                updateHud();
                return;
            }
            stopGlide();
            final double factor = e.getDeltaY() > 0 ? 0.88 : 1 / 0.88;
            final Point3D under = orbit.onPivotPlane(e.getX(), e.getY(), view.getWidth(), view.getHeight(), FIELD_OF_VIEW);
            place(orbit.zoomedToward(under, factor));
        });
        view.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.isStillSincePress()) {
                onBackground.run();
            }
        });
    }

    private static PhongMaterial material(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        material.setSpecularColor(Color.TRANSPARENT);
        return material;
    }
}
