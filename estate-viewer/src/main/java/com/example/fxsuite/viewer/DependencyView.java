package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.viewer.EstateLayout.Edge;
import com.example.fxsuite.viewer.EstateLayout.Placed;
import com.example.fxsuite.viewer.Highlighter.Highlight;

import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
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
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * The rendering engine both viewers share: draws an {@link EstateLayout} in 3D, lets the
 * user orbit, pan and zoom, and highlights what a selected release resolves to.
 *
 * <p>Each release is a panel facing the viewer. The base layer, releases with no internal
 * dependencies, is at the back; every other release sits one layer in front of its deepest
 * dependency. Declared dependencies are dim lines. Selecting a release lights the graph its
 * resolution uses, dims the rest, and draws in amber the declared dependencies resolution
 * raised past.
 */
public final class DependencyView {

    private static final double COLUMN = 240;
    private static final double ROW = 90;
    private static final double LAYER = 320;
    private static final double CARD_WIDTH = 204;
    private static final double CARD_HEIGHT = 60;

    private static final double START_PITCH = -22;
    private static final double START_YAW = -28;

    private final VersionText text = new VersionText();
    private final Group content = new Group();
    private final Pane node;

    private final Map<Version<?>, Region> cards = new HashMap<>();
    private final Map<Edge, Cylinder> edges = new LinkedHashMap<>();

    private final PhongMaterial edgeIdle = material("#5d6573");
    private final PhongMaterial edgeDim = material("#2b2f37");
    private final PhongMaterial edgeLit = material("#eef1f5");
    private final PhongMaterial edgeRaised = material("#f0b429");

    private final Rotate pitch = new Rotate(START_PITCH, Rotate.X_AXIS);
    private final Rotate yaw = new Rotate(START_YAW, Rotate.Y_AXIS);
    private final Translate camera = new Translate(0, 0, -2300);
    private double fittedDistance = -2300;

    private EstateLayout layout = new EstateLayout(java.util.List.of());
    private Consumer<Highlight> onSelect = highlight -> { };
    private Runnable onClear = () -> { };
    private Runnable onBackground = this::clear;
    private double lastX;
    private double lastY;

    public DependencyView() {
        final Group world = new Group(content);
        world.getTransforms().addAll(pitch, yaw);

        final PerspectiveCamera eye = new PerspectiveCamera(true);
        eye.setFieldOfView(35);
        eye.setNearClip(1);
        eye.setFarClip(40_000);
        eye.getTransforms().add(camera);

        final SubScene view = new SubScene(world, 0, 0, true, SceneAntialiasing.BALANCED);
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
            final Region card = card(placed);
            cards.put(placed.version(), card);
            content.getChildren().add(card);
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
        cards.forEach((version, card) -> {
            card.getStyleClass().removeAll("selected", "member", "passed", "dim");
            if (version.equals(root)) {
                card.getStyleClass().add("selected");
            } else if (highlight.members().contains(version)) {
                card.getStyleClass().add("member");
            } else if (highlight.passedOver().contains(version)) {
                card.getStyleClass().add("passed");
            } else {
                card.getStyleClass().add("dim");
            }
            // panels inside the 3D sub-scene do not restyle on their own when their classes change
            card.applyCss();
        });
        edges.forEach((edge, shape) -> shape.setMaterial(
                highlight.lit().contains(edge) ? edgeLit
                        : highlight.raised().contains(edge) ? edgeRaised : edgeDim));
        onSelect.accept(highlight);
    }

    public void clear() {
        cards.values().forEach(card -> {
            card.getStyleClass().removeAll("selected", "member", "passed", "dim");
            card.applyCss();
        });
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
        final double width = (maxLane - minLane) * COLUMN + CARD_WIDTH;
        final double height = layout.maxRow() * ROW + CARD_HEIGHT;
        final double depth = layout.maxDepth() * LAYER;
        return -1.8 * Math.max(Math.max(width, height), Math.max(depth, 650));
    }

    private Region card(final Placed placed) {
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

        final VBox card = new VBox(3, top, number);
        card.getStyleClass().addAll("card", placed.kind().name().toLowerCase());
        if (placed.isVirtual()) {
            card.getStyleClass().add("virtual");
        }
        card.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        final Point3D at = centre(placed);
        card.setTranslateX(at.getX() - CARD_WIDTH / 2);
        card.setTranslateY(at.getY() - CARD_HEIGHT / 2);
        card.setTranslateZ(at.getZ());
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.isStillSincePress()) {
                select(placed.version());
            }
            e.consume();
        });
        return card;
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

    private java.util.List<Node> layerLabels() {
        return IntStream.rangeClosed(0, layout.maxDepth()).mapToObj(depth -> {
            final double firstLane = layout.placed().stream().filter(p -> p.depth() == depth)
                    .mapToDouble(Placed::lane).min().orElse(0);
            final Label label = new Label(depth == 0 ? "depth 0 · no internal dependencies" : "depth " + depth);
            label.getStyleClass().add("layer");
            label.setTranslateX(firstLane * COLUMN - CARD_WIDTH / 2);
            label.setTranslateY(CARD_HEIGHT / 2 + 14);
            label.setTranslateZ((layout.maxDepth() - depth) * LAYER);
            label.setMouseTransparent(true);
            return (Node) label;
        }).toList();
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
