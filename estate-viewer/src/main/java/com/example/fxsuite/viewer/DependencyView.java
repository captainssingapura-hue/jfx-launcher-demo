package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.viewer.EstateLayout.Edge;
import com.example.fxsuite.viewer.EstateLayout.Placed;
import com.example.fxsuite.viewer.Highlighter.Highlight;

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

    private static final double START_PITCH = -22;
    /** -Destate.yaw=<degrees> starts from another angle, e.g. 152 to look from behind. */
    private static final double START_YAW = Double.parseDouble(System.getProperty("estate.yaw", "-28"));

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

    private final Rotate pitch = new Rotate(START_PITCH, Rotate.X_AXIS);
    private final Rotate yaw = new Rotate(START_YAW, Rotate.Y_AXIS);
    private final Translate camera = new Translate(0, 0, -2300);
    private double fittedDistance = -2300;

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
        world.getTransforms().addAll(pitch, yaw);

        // soft light from above and to the left of the viewer, fixed while the estate turns
        final AmbientLight ambient = new AmbientLight(Color.web("#a9adb4"));
        final PointLight key = new PointLight(Color.web("#6e6e6e"));
        key.getTransforms().add(new Translate(-1400, -1800, -2600));

        final PerspectiveCamera eye = new PerspectiveCamera(true);
        eye.setFieldOfView(35);
        // a near clip well away from zero keeps depth precise enough, even across the whole
        // estate, for a face to stay in front of its own slate
        eye.setNearClip(20);
        eye.setFarClip(40_000);
        eye.getTransforms().add(camera);

        final SubScene view = new SubScene(new Group(world, ambient, key), 0, 0, true, SceneAntialiasing.BALANCED);
        view.setFill(Color.web("#15171c"));
        view.setCamera(eye);
        node = new Pane(view);
        view.widthProperty().bind(node.widthProperty());
        view.heightProperty().bind(node.heightProperty());
        orbitAndZoom(view);
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

    /** Esc does the background action; R resets the view. */
    public void installKeys(final Scene scene) {
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                onBackground.run();
            } else if (e.getCode() == KeyCode.R) {
                resetView();
            }
        });
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

        content.getTransforms().setAll(new Translate(0, layout.maxRow() * ROW / 2, -layout.maxDepth() * LAYER / 2));
        fittedDistance = fit(layout);
        resetView();
        clear();
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
        onSelect.accept(highlight);
    }

    public void clear() {
        cards.values().forEach(slate -> restyle(slate, null));
        edges.values().forEach(shape -> shape.setMaterial(edgeIdle));
        onClear.run();
    }

    /** The shown release written as {@code "Ledger 1.20261101.0"}, if there is one. */
    public Optional<Version<?>> find(final String named) {
        return layout.placed().stream().<Version<?>>map(Placed::version)
                .filter(v -> text.named(v).equals(named)).findFirst();
    }

    public void resetView() {
        pitch.setAngle(START_PITCH);
        yaw.setAngle(START_YAW);
        camera.setX(0);
        camera.setY(0);
        camera.setZ(fittedDistance);
    }

    // ---- building the scene

    private Point3D centre(final Placed placed) {
        return new Point3D(placed.lane() * COLUMN, -placed.row() * ROW,
                (layout.maxDepth() - placed.depth()) * LAYER);
    }

    /** A camera distance that keeps the whole set in view from the starting angle. */
    private double fit(final EstateLayout layout) {
        final double minLane = layout.placed().stream().mapToDouble(Placed::lane).min().orElse(0);
        final double maxLane = layout.placed().stream().mapToDouble(Placed::lane).max().orElse(0);
        final double width = (maxLane - minLane) * COLUMN + CARD_WIDTH * LogScale.LARGEST;
        final double height = layout.maxRow() * ROW + CARD_HEIGHT * LogScale.LARGEST;
        final double depth = layout.maxDepth() * LAYER;
        return -1.8 * Math.max(Math.max(width, height), Math.max(depth, 650));
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
        view.setOnMousePressed(e -> {
            lastX = e.getSceneX();
            lastY = e.getSceneY();
        });
        view.setOnMouseDragged(e -> {
            final double dx = e.getSceneX() - lastX;
            final double dy = e.getSceneY() - lastY;
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            if (e.getButton() == MouseButton.PRIMARY) {
                yaw.setAngle(yaw.getAngle() + dx * 0.25);
                pitch.setAngle(Math.max(-85, Math.min(85, pitch.getAngle() - dy * 0.25)));
            } else {
                final double scale = -camera.getZ() / 900;
                camera.setX(camera.getX() - dx * scale);
                camera.setY(camera.getY() - dy * scale);
            }
        });
        view.setOnScroll(e -> {
            final double factor = e.getDeltaY() > 0 ? 0.9 : 1.1;
            camera.setZ(Math.max(-30_000, Math.min(-300, camera.getZ() * factor)));
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
