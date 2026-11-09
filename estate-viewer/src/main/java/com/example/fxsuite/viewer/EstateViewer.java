package com.example.fxsuite.viewer;

import com.example.fxsuite.deployment.Version;
import com.example.fxsuite.deployment.VersionText;
import com.example.fxsuite.deployment.Violation;
import com.example.fxsuite.deployment.Warning;
import com.example.fxsuite.estate.DemoEstate;
import com.example.fxsuite.viewer.EstateLayout.Edge;
import com.example.fxsuite.viewer.EstateLayout.Placed;
import com.example.fxsuite.viewer.Highlighter.Highlight;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The demo estate in 3D. Each release is a panel facing the viewer. The base layer, releases
 * with no internal dependencies, is at the back; every other release sits one layer in front
 * of its deepest dependency. Each unit keeps one column through every layer.
 *
 * <p>Declared dependencies are drawn dimmed. Clicking a release highlights the graph it
 * resolves to; a declared dependency that resolution raised past is drawn in amber.
 *
 * <p>For an unattended picture: {@code -Destate.snapshot=out.png} saves the window and exits,
 * and {@code -Destate.select="Ledger 1.20261101.0"} selects a release first.
 */
public final class EstateViewer extends Application {

    private static final double COLUMN = 240;
    private static final double ROW = 90;
    private static final double LAYER = 320;
    private static final double CARD_WIDTH = 204;
    private static final double CARD_HEIGHT = 60;

    private static final double START_PITCH = -22;
    private static final double START_YAW = -28;
    private static final double START_DISTANCE = -2300;

    private final EstateLayout layout = new EstateLayout(new DemoEstate().units());
    private final VersionText text = new VersionText();

    private final Map<Version<?>, Region> cards = new HashMap<>();
    private final Map<Edge, Cylinder> edges = new LinkedHashMap<>();

    private final PhongMaterial edgeIdle = material("#5d6573");
    private final PhongMaterial edgeDim = material("#2b2f37");
    private final PhongMaterial edgeLit = material("#eef1f5");
    private final PhongMaterial edgeRaised = material("#f0b429");

    private final Rotate pitch = new Rotate(START_PITCH, Rotate.X_AXIS);
    private final Rotate yaw = new Rotate(START_YAW, Rotate.Y_AXIS);
    private final Translate camera = new Translate(0, 0, START_DISTANCE);

    private final VBox details = new VBox(10);
    private double lastX;
    private double lastY;

    @Override
    public void start(final Stage stage) {
        final Group content = new Group();
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

        final Point3D middle = new Point3D(
                0,
                -layout.maxRow() * ROW / 2,
                layout.maxDepth() * LAYER / 2);
        content.getTransforms().add(new Translate(-middle.getX(), -middle.getY(), -middle.getZ()));

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
        final Pane holder = new Pane(view);
        view.widthProperty().bind(holder.widthProperty());
        view.heightProperty().bind(holder.heightProperty());
        orbitAndZoom(view);

        final BorderPane root = new BorderPane(holder);
        root.setRight(sidePanel());
        final Scene scene = new Scene(root, 1440, 880);
        scene.getStylesheets().add(EstateViewer.class.getResource("/viewer.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                clear();
            } else if (e.getCode() == KeyCode.R) {
                resetView();
            }
        });

        stage.setTitle("FxSuite · Estate viewer");
        stage.setScene(scene);
        stage.show();
        clear();

        Optional.ofNullable(System.getProperty("estate.select")).flatMap(this::find).ifPresent(this::select);
        Optional.ofNullable(System.getProperty("estate.snapshot")).ifPresent(path -> saveAndExit(scene, path));
    }

    // ---- building the scene

    private Point3D centre(final Placed placed) {
        return new Point3D(placed.lane() * COLUMN, -placed.row() * ROW,
                (layout.maxDepth() - placed.depth()) * LAYER);
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

    private List<Node> layerLabels() {
        return java.util.stream.IntStream.rangeClosed(0, layout.maxDepth()).mapToObj(depth -> {
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

    private ScrollPane sidePanel() {
        final Label title = new Label("Demo estate");
        title.getStyleClass().add("title");
        final Label counts = new Label(layout.placed().size() + " releases · " + layout.edges().size()
                + " dependencies · " + (layout.maxDepth() + 1) + " layers");
        counts.getStyleClass().add("muted");

        final VBox legend = new VBox(6,
                swatch("bundle", "Bundle of external libraries"),
                swatch("library", "Internal library"),
                swatch("application", "Application"),
                swatch("virtual-sample", "Virtual release: same binary, newer dependencies"));

        final Label help = new Label("Drag to orbit · right-drag to pan · scroll to zoom\n"
                + "Click a release to see what it resolves to · Esc clears · R resets the view");
        help.getStyleClass().add("muted");
        help.setWrapText(true);

        final VBox panel = new VBox(14, title, counts, legend, help, new Region(), details);
        panel.getStyleClass().add("side");
        final ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(380);
        scroll.getStyleClass().add("side-scroll");
        return scroll;
    }

    private HBox swatch(final String kind, final String meaning) {
        final Region box = new Region();
        box.getStyleClass().addAll("swatch", kind);
        final Label label = new Label(meaning);
        label.setWrapText(true);
        return new HBox(10, box, label);
    }

    // ---- selection

    private void select(final Version<?> root) {
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
            card.applyCss();
        });
        edges.forEach((edge, shape) -> shape.setMaterial(
                highlight.lit().contains(edge) ? edgeLit
                        : highlight.raised().contains(edge) ? edgeRaised : edgeDim));
        showDetails(highlight);
    }

    private void clear() {
        cards.values().forEach(card -> {
            card.getStyleClass().removeAll("selected", "member", "passed", "dim");
            card.applyCss();
        });
        edges.values().forEach(shape -> shape.setMaterial(edgeIdle));
        details.getChildren().setAll(muted("Nothing selected."));
    }

    private void showDetails(final Highlight highlight) {
        final Placed placed = layout.of(highlight.root()).orElseThrow();
        final Label name = new Label(text.named(highlight.root()));
        name.getStyleClass().add("selection");
        final String kind = switch (placed.kind()) {
            case BUNDLE -> "Bundle";
            case LIBRARY -> "Library";
            case APPLICATION -> "Application";
        };
        details.getChildren().setAll(name,
                muted(kind + " · depth " + placed.depth() + (placed.isVirtual() ? " · virtual release" : "")));

        details.getChildren().add(section("Declares"));
        final List<Version<?>> declared = highlight.root().dependencies().versions().stream()
                .<Version<?>>map(v -> v).sorted(Comparator.comparing(text::named)).toList();
        if (declared.isEmpty()) {
            details.getChildren().add(muted("No internal dependencies."));
        }
        declared.forEach(v -> details.getChildren().add(new Label(text.named(v))));

        final List<Version<?>> resolved = highlight.resolution().resolved().versions().stream()
                .<Version<?>>map(v -> v).sorted(Comparator.comparing(text::named)).toList();
        details.getChildren().add(section("Runs with " + resolved.size() + " releases"));
        resolved.forEach(v -> details.getChildren().add(new Label(text.named(v))));

        for (final Warning warning : highlight.resolution().warnings().items()) {
            details.getChildren().add(section("Warning"));
            details.getChildren().add(styled(warning.message(), "warning"));
        }
        for (final Violation conflict : highlight.resolution().conflicts().items()) {
            details.getChildren().add(section("Conflict"));
            details.getChildren().add(styled(conflict.message(), "conflict"));
        }
    }

    private Optional<Version<?>> find(final String named) {
        return layout.placed().stream().map(Placed::version).filter(v -> text.named(v).equals(named)).findFirst();
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
                clear();
            }
        });
    }

    private void resetView() {
        pitch.setAngle(START_PITCH);
        yaw.setAngle(START_YAW);
        camera.setX(0);
        camera.setY(0);
        camera.setZ(START_DISTANCE);
    }

    // ---- small helpers

    private static PhongMaterial material(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        material.setSpecularColor(Color.TRANSPARENT);
        return material;
    }

    private Label section(final String title) {
        return styled(title.toUpperCase(), "section");
    }

    private Label muted(final String message) {
        return styled(message, "muted");
    }

    private Label styled(final String message, final String styleClass) {
        final Label label = new Label(message);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        return label;
    }

    private void saveAndExit(final Scene scene, final String path) {
        final PauseTransition settle = new PauseTransition(Duration.millis(600));
        settle.setOnFinished(e -> {
            try {
                final WritableImage image = scene.snapshot(null);
                final PixelReader pixels = image.getPixelReader();
                final BufferedImage out = new BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < out.getHeight(); y++) {
                    for (int x = 0; x < out.getWidth(); x++) {
                        out.setRGB(x, y, pixels.getArgb(x, y));
                    }
                }
                ImageIO.write(out, "png", new File(path));
                System.out.println("saved " + path);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
        });
        settle.play();
    }
}
