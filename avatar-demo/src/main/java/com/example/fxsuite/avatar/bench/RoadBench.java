package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.Game;
import com.example.fxsuite.avatar.look.RoadworkModel;
import com.example.fxsuite.avatar.roads.Ground;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.roads.Sections;
import com.example.fxsuite.avatar.sim.Vec2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The roads bench: roads designed on their own ({@link Roadway}), laid in order in a made-up place
 * ({@link RoadScenes}, {@link Roadworks}), to work out and refine how roads are built: their strips across, paved and
 * ground, their courses, their formation, the ground prepared for them where it carries them, the structures that
 * carry them where it cannot, and where two roads' prepared grounds meet; nothing else in the world.
 *
 * <p>Seen three ways at once: as built, in 3D; along the road looked at, as its long section; across it at a point,
 * as its cross-section through whatever is there. Each stage shown or not: the ground, as prepared or as it was, as
 * built or grown over; the pavement; the markings; the structures; the walls wanted. Cut away at the point looked
 * at, so the courses show end on. The kinds across written as text ({@link Sections}) beside it, to be changed and
 * applied.
 *
 * <p>{@code -Droads.scene=valley} picks the scene; {@code -Droads.section="town street"} the kind across;
 * {@code -Droads.road=1} which of its roads is looked at; {@code -Droads.at=250} the point looked at, in metres along;
 * {@code -Droads.cut=true} cuts it away there; {@code -Droads.built=true} shows the ground as built;
 * {@code -Droads.yaw}, {@code -Droads.pitch}, {@code -Droads.distance} (m) where it is seen from.
 * {@code -Droads.drive=true} opens the scene to drive in at once.
 * {@code -Dbench.snapshot=out.png} saves the window after {@code -Dbench.settle} ms and exits.
 */
public final class RoadBench extends Application {

    private final Map<String, RoadScenes.Scene> scenes = RoadScenes.all();
    private Sections.Catalogue catalogue = Sections.read(Sections.STANDARD);
    private Map<String, Section> sections = catalogue.sections();
    private final KindPanel kind = new KindPanel(this::changed);

    private final ComboBox<String> sceneBox = new ComboBox<>();
    private final ComboBox<String> sectionBox = new ComboBox<>();
    private final ComboBox<String> roadBox = new ComboBox<>();
    private final CheckBox showGround = new CheckBox("Ground");
    private final CheckBox showEarthworks = new CheckBox("Prepared");
    private final CheckBox asBuilt = new CheckBox("As built");
    private final CheckBox showPavement = new CheckBox("Pavement");
    private final CheckBox showMarkings = new CheckBox("Markings");
    private final CheckBox showStructures = new CheckBox("Structures");
    private final CheckBox showWater = new CheckBox("Water");
    private final CheckBox cutAway = new CheckBox("Cut away");
    private final ComboBox<Integer> taller = new ComboBox<>();
    private final Slider atSlider = new Slider(0, RoadScenes.LENGTH / 100, RoadScenes.LENGTH / 200);
    private final TextArea text = new TextArea(Sections.STANDARD);
    private final Label figures = new Label();
    private final Label problems = new Label();
    private final Button drive = new Button("Drive");

    private final Canvas along = new Canvas(800, 220);
    private final Canvas across = new Canvas(520, 220);

    private final PerspectiveCamera eye = new PerspectiveCamera(true);
    private final Rotate yaw = new Rotate(-35, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(-22, Rotate.X_AXIS);
    private final Translate back = new Translate(0, 0, -9_000);
    private final Translate target = new Translate(0, 0, 0);
    private final Group groundGroup = new Group();
    private final Group roadGroup = new Group();
    private final Group waterGroup = new Group();
    private double lastX;
    private double lastY;

    private RoadScenes.Scene scene;
    private Roadworks works;
    private List<Roadworks.Plan> plans = List.of();
    private int looked;
    private double[] bounds;
    private boolean redesigning;
    /** Whether the roads are being designed again for a change made on the kind's panel, which is left as it is. */
    private boolean fromPanel;
    /** How long the roads took to design, ms. */
    private double designed;

    @Override
    public void start(final Stage stage) {
        eye.setNearClip(10);
        eye.setFarClip(2_000_000);
        eye.setFieldOfView(40);
        eye.getTransforms().addAll(target, yaw, pitch, back);
        final AmbientLight ambient = new AmbientLight(Color.web("#8e959c"));
        final DirectionalLight sun = new DirectionalLight(Color.web("#9a978f"));
        sun.setDirection(new javafx.geometry.Point3D(0.45, 1, 0.35));
        final Group world = new Group(groundGroup, roadGroup, waterGroup, eye, ambient, sun);
        final SubScene view = new SubScene(world, 900, 520, true, SceneAntialiasing.BALANCED);
        view.setFill(Color.web("#c9dbe8"));
        view.setCamera(eye);
        final Pane holder = new Pane(view);
        view.widthProperty().bind(holder.widthProperty());
        view.heightProperty().bind(holder.heightProperty());
        orbit(view);

        sceneBox.getItems().setAll(scenes.keySet());
        sceneBox.setValue(System.getProperty("roads.scene", "valley").toLowerCase(Locale.ROOT));
        if (!scenes.containsKey(sceneBox.getValue())) {
            sceneBox.setValue("valley");
        }
        sectionBox.getItems().setAll(sections.keySet());
        sectionBox.setValue(System.getProperty("roads.section", scenes.get(sceneBox.getValue()).section()));
        for (final CheckBox box : List.of(showGround, showEarthworks, showPavement, showMarkings, showStructures,
                showWater)) {
            box.setSelected(true);
        }
        asBuilt.setSelected(Boolean.getBoolean("roads.built"));
        cutAway.setSelected(Boolean.getBoolean("roads.cut"));
        taller.getItems().setAll(1, 2, 5, 10, 20);
        taller.setValue(Integer.getInteger("roads.taller", 2));
        final String at = System.getProperty("roads.at");
        atSlider.setPrefWidth(400);
        HBox.setHgrow(atSlider, Priority.ALWAYS);

        sceneBox.setOnAction(e -> {
            final String kind = scenes.get(sceneBox.getValue()).section();
            redesigning = true;
            if (sections.containsKey(kind)) {
                sectionBox.setValue(kind);
            }
            redesigning = false;
            design(true);
        });
        sectionBox.setOnAction(e -> {
            if (!redesigning) {
                design(false);
            }
        });
        roadBox.setOnAction(e -> {
            if (!redesigning && roadBox.getSelectionModel().getSelectedIndex() >= 0) {
                looked = roadBox.getSelectionModel().getSelectedIndex();
                atSlider.setMax(road().length() / 100);
                figures();
                lookedAt();
            }
        });
        showEarthworks.setOnAction(e -> drawGround());
        asBuilt.setOnAction(e -> drawGround());
        for (final CheckBox box : List.of(showPavement, showMarkings, showStructures, cutAway)) {
            box.setOnAction(e -> drawRoads());
        }
        showWater.setOnAction(e -> waterGroup.setVisible(showWater.isSelected()));
        showGround.setOnAction(e -> groundGroup.setVisible(showGround.isSelected()));
        taller.setOnAction(e -> drawSections());
        atSlider.valueProperty().addListener((o, was, now) -> lookedAt());

        drive.setOnAction(e -> drive());
        drive.setStyle("-fx-font-weight: bold;");
        final Button apply = new Button("Apply");
        apply.setOnAction(e -> applyText());
        final Button reset = new Button("Standard");
        reset.setOnAction(e -> {
            text.setText(Sections.STANDARD);
            applyText();
        });
        text.setFont(Font.font("Monospaced", 11));
        text.setPrefColumnCount(52);
        figures.setWrapText(true);
        problems.setWrapText(true);
        problems.setTextFill(Color.web("#b03a2e"));
        final VBox written = new VBox(6, text, new HBox(6, apply, reset));
        written.setPadding(new Insets(6, 0, 0, 0));
        VBox.setVgrow(text, Priority.ALWAYS);
        final javafx.scene.control.ScrollPane controls = new javafx.scene.control.ScrollPane(kind);
        controls.setFitToWidth(true);
        final TabPane tabs = new TabPane(new Tab("Kind", controls), new Tab("Text", written));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        final VBox side = new VBox(6, new Label("Kinds across"), tabs, figures, problems);
        side.setPadding(new Insets(8));
        side.setPrefWidth(420);

        final HBox bar = new HBox(8, new Label("Scene"), sceneBox, new Label("Kind"), sectionBox, new Label("Road"),
                roadBox, showGround, showEarthworks, asBuilt, showPavement, showMarkings, showStructures, showWater,
                cutAway, new Label("Heights ×"), taller, drive);
        bar.setPadding(new Insets(6, 8, 6, 8));
        bar.setAlignment(Pos.CENTER_LEFT);
        final HBox atBar = new HBox(8, new Label("Along (m)"), atSlider);
        atBar.setPadding(new Insets(4, 8, 4, 8));
        atBar.setAlignment(Pos.CENTER_LEFT);

        final Pane alongHolder = sized(along);
        final Pane acrossHolder = sized(across);
        along.setOnMousePressed(e -> pick(e.getX()));
        along.setOnMouseDragged(e -> pick(e.getX()));
        final SplitPane drawings = new SplitPane(alongHolder, acrossHolder);
        drawings.setDividerPositions(0.55);
        final SplitPane middle = new SplitPane(holder, drawings);
        middle.setOrientation(Orientation.VERTICAL);
        middle.setDividerPositions(0.62);
        final BorderPane root = new BorderPane(new VBox(middle, atBar));
        VBox.setVgrow(middle, Priority.ALWAYS);
        root.setTop(bar);
        root.setRight(side);
        root.setStyle("-fx-background-color: #f4f5f7;");
        final Scene windowScene = new Scene(root, 1560, 940);

        looked = Integer.getInteger("roads.road", 0);
        design(true);
        if (at != null) {
            atSlider.setValue(Double.parseDouble(at));
        }
        yaw.setAngle(Double.parseDouble(System.getProperty("roads.yaw", "-35")));
        pitch.setAngle(-Double.parseDouble(System.getProperty("roads.pitch", "22")));
        back.setZ(-100 * Double.parseDouble(System.getProperty("roads.distance", "90")));
        stage.setTitle("Roads bench");
        stage.setScene(windowScene);
        stage.show();
        SoundBench.snapshotIfAsked(windowScene);
        if (Boolean.getBoolean("roads.drive")) {
            Platform.runLater(this::drive);
        }
    }

    /**
     * The scene as it is now, its roads of the kind chosen, played in a game window of its own ({@link TestSection}): one
     * of each vehicle parked at the start. Closing the window ends that game; Tab in it closes it.
     */
    private void drive() {
        if (works == null) {
            return;
        }
        final Section section = sections.getOrDefault(sectionBox.getValue(), sections.values().iterator().next());
        final TestSection demo = new TestSection(scene, section, works);
        final Stage window = new Stage();
        final Game game = new Game(window, demo, () -> Platform.runLater(window::close), other -> { });
        window.setOnHidden(e -> game.stop());
    }

    /** The road looked at. */
    private Roadway road() {
        return works.roads().get(looked);
    }

    /** A canvas that fills what holds it, drawn again as that changes size. */
    private Pane sized(final Canvas canvas) {
        final Pane holder = new Pane(canvas);
        holder.setMinSize(100, 100);
        canvas.widthProperty().bind(holder.widthProperty());
        canvas.heightProperty().bind(holder.heightProperty());
        canvas.widthProperty().addListener((o, was, now) -> drawSections());
        canvas.heightProperty().addListener((o, was, now) -> drawSections());
        return holder;
    }

    /** A kind changed on its panel: kept, its text written again from it, the roads designed again with it. */
    private void changed(final Section section) {
        final Map<String, Section> now = new java.util.LinkedHashMap<>(sections);
        now.put(section.name(), section);
        catalogue = new Sections.Catalogue(catalogue.courses(), now);
        sections = now;
        text.setText(Sections.write(catalogue));
        fromPanel = true;
        try {
            design(false);
        } finally {
            fromPanel = false;
        }
    }

    /** The kinds across as the text now says, if it can be read; what is wrong with it, if not. */
    private void applyText() {
        try {
            catalogue = Sections.read(text.getText());
            sections = catalogue.sections();
        } catch (final IllegalArgumentException e) {
            problems.setText(e.getMessage());
            return;
        }
        final String was = sectionBox.getValue();
        redesigning = true;
        sectionBox.getItems().setAll(sections.keySet());
        sectionBox.setValue(sections.containsKey(was) ? was : sections.keySet().iterator().next());
        redesigning = false;
        design(false);
    }

    /** The roads designed again, as the scene and the kind across now are; looked at afresh, if {@code frame}d. */
    private void design(final boolean frame) {
        scene = scenes.get(sceneBox.getValue());
        final Section section = sections.getOrDefault(sectionBox.getValue(), sections.values().iterator().next());
        if (!fromPanel) {
            kind.show(section);
        }
        final long started = System.nanoTime();
        try {
            plans = scene.roads().apply(section);
            works = Roadworks.design(scene.profile(), plans);
        } catch (final IllegalArgumentException e) {
            problems.setText(e.getMessage());
            return;
        }
        designed = (System.nanoTime() - started) / 1e6;
        redesigning = true;
        roadBox.getItems().setAll(works.roads().stream().map(Roadway::name).toList());
        looked = frame && looked >= works.roads().size() ? 0 : Math.min(looked, works.roads().size() - 1);
        roadBox.getSelectionModel().select(looked);
        redesigning = false;
        bounds = RoadworkModel.bounds(works.roads());
        atSlider.setMax(road().length() / 100);
        if (frame) {
            atSlider.setValue(scene.at() / 100);
        }
        drawGround();
        drawRoads();
        waterGroup.getChildren().clear();
        if (scene.watered()) {
            waterGroup.getChildren().add(RoadworkModel.water(scene.water(), bounds));
        }
        waterGroup.setVisible(showWater.isSelected());
        figures();
        lookedAt();
    }

    private void drawGround() {
        final Ground ground = showEarthworks.isSelected() ? works.ground() : Ground.untouched(works.raw());
        groundGroup.getChildren().setAll(RoadworkModel.ground(ground, asBuilt.isSelected(), bounds));
    }

    private void drawRoads() {
        final double upTo = cutAway.isSelected() ? atSlider.getValue() * 100 : Double.POSITIVE_INFINITY;
        roadGroup.getChildren().clear();
        for (int k = 0; k < works.roads().size(); k++) {
            final Roadway r = works.roads().get(k);
            // cut away the road looked at only
            final double to = k == looked ? upTo : Double.POSITIVE_INFINITY;
            if (showPavement.isSelected()) {
                roadGroup.getChildren().add(RoadworkModel.pavement(r, to));
            }
            if (showMarkings.isSelected()) {
                roadGroup.getChildren().add(RoadworkModel.markings(r, to));
            }
            if (showStructures.isSelected()) {
                roadGroup.getChildren().add(RoadworkModel.structures(r, works.ground().profile()));
            }
        }
        if (showStructures.isSelected()) {
            roadGroup.getChildren().add(RoadworkModel.walls(works));
        }
    }

    /** What the road looked at comes to: how long, how steep, how high and deep, how much earth, what carries it where. */
    private void figures() {
        final Roadway road = road();
        final double[] moved = road.earthMoved();
        final List<String> carried = new ArrayList<>();
        for (final Roadway.Stretch s : road.stretches()) {
            carried.add(String.format(Locale.ROOT, "%s %s–%s m", s.carrier().name().toLowerCase(Locale.ROOT),
                    RoadDrawings.metres(road.along(s.from()), 0), RoadDrawings.metres(road.along(s.to()), 0)));
        }
        final long piers = road.structures().stream().mapToLong(s -> s.piers().size()).sum();
        final Section s = road.section();
        final double deepest = s.paved().stream().mapToDouble(l -> l.strip().depth()).max().orElse(0);
        figures.setText(String.format(Locale.ROOT,
                "%s: %s m long; %s m wide, %s m paved, %s m deep; steepest %.1f%% (of %.0f%%)%n"
                        + "highest bank %s m, deepest cutting %s m (takes %s, %s)%n"
                        + "earth cut %,.0f m³, filled %,.0f m³; %d piers%n%s%n"
                        + "%d roads designed in %.1f ms",
                road.name(), RoadDrawings.metres(road.length(), 0), RoadDrawings.metres(s.width(), 2),
                RoadDrawings.metres(s.pavedEdge(-1) + s.pavedEdge(1), 2), RoadDrawings.metres(deepest, 2),
                road.steepest() * 100, s.grade() * 100,
                RoadDrawings.metres(road.highestBank(), 1), RoadDrawings.metres(road.deepestCutting(), 1),
                RoadDrawings.metres(s.earthworks().bank(), 1), RoadDrawings.metres(s.earthworks().cutting(), 1),
                moved[0], moved[1], piers, String.join("; ", carried), works.roads().size(), designed));
        problems.setText(String.join("\n", works.problems()));
    }

    /** The point looked at moved: the camera's target there, the drawings drawn again, the cut away moved. */
    private void lookedAt() {
        if (works == null) {
            return;
        }
        final double at = atSlider.getValue() * 100;
        final Roadway road = road();
        final int i = road.nearestPoint(at);
        final Vec2 p = road.point(i);
        target.setX(p.x());
        target.setY(-road.level(i));
        target.setZ(p.z());
        if (cutAway.isSelected()) {
            drawRoads();
        }
        drawSections();
    }

    private void drawSections() {
        if (works == null || along.getWidth() < 80 || along.getHeight() < 60 || across.getWidth() < 80
                || across.getHeight() < 60) {
            return;
        }
        final double at = atSlider.getValue() * 100;
        RoadDrawings.longSection(along.getGraphicsContext2D(), along.getWidth(), along.getHeight(), road(),
                plans.get(looked).clearances(), scene, at);
        RoadDrawings.crossSection(across.getGraphicsContext2D(), across.getWidth(), across.getHeight(), works, looked,
                scene, road().nearestPoint(at), taller.getValue());
    }

    /** Clicked or dragged on the long section: the point looked at moved there. */
    private void pick(final double x) {
        final double left = 46;
        final double right = 12;
        final double t = (x - left) / (along.getWidth() - left - right);
        atSlider.setValue(Math.clamp(t, 0, 1) * road().length() / 100);
    }

    /** Dragged, it turns round the point looked at; right-dragged, moves across; scrolled, nearer or further. */
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
        view.setOnScroll(e -> back.setZ(Math.clamp(back.getZ() * Math.pow(1.0015, -e.getDeltaY()), -200_000, -300)));
    }
}
