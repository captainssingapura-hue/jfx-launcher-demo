package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.look.FrameModel;
import com.example.fxsuite.avatar.look.SidecarModel;
import com.example.fxsuite.avatar.look.World;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Frame;
import com.example.fxsuite.avatar.sim.Frames;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Sidecar;
import com.example.fxsuite.avatar.sim.Vec3;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A bench for working on the vehicles' models, apart from the game: any of the game's vehicles on
 * show on their own, drawn exactly as the game draws them ({@link World}, from a world with nothing
 * else in it), to look at from any side; and what is built of rods ({@link Frame}), to shape: the
 * boxer's own frame, and the sidecar's chassis beside it.
 *
 * <ul>
 *   <li><b>Look round it:</b> drag to turn round it, right-drag (or Shift-drag) to move across it,
 *       scroll to come nearer or go further off.</li>
 *   <li><b>Shape a frame:</b> the motorcycle's, or the sidecar's. Pick a node and move it across,
 *       along and up; the rods follow. Pick a rod and size its tube, split it in two at a new node,
 *       or take it away; or join the node picked to another with a new rod. Its figures are read
 *       out as it changes (how much tube, how heavy, where its mass is, whether it is one piece, how
 *       much it leans on its welds) with anything wrong with it.</li>
 *   <li><b>The sidecar:</b> mount it on the right or the left, and set its wheel along its outer
 *       rail and out.</li>
 *   <li><b>Keep it:</b> save what is being shaped as text, a line a node and a line a rod, and open it
 *       again ({@link Frame#text}, {@link Sidecar#text}).</li>
 * </ul>
 *
 * <p>{@code -Dmodel.pick=sidecar} (or the start of any vehicle's name: {@code boxer}, {@code van},
 * ...) picks what is on show to start with, {@code -Dmodel.side=left} mounts the sidecar on the
 * left, {@code -Dmodel.shape=frame} shapes the motorcycle's frame; {@code -Dmodel.yaw=40} and
 * {@code -Dmodel.pitch=20} where it is seen from (-140, from ahead on its right),
 * {@code -Dmodel.distance=600} how far off; {@code -Dmodel.asgame=true} has the game draw even the frames
 * being shaped, as it has them; {@code -Dbench.snapshot=out.png} saves the window after
 * {@code -Dbench.settle} ms and exits.
 */
public final class ModelBench extends Application {

    /** A vehicle on show: its chassis and body; and whether the sidecar is beside it. */
    private record Model(String name, Course.Chassis chassis, Course.Bodywork body, boolean sidecar) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<Model> MODELS = List.of(
            new Model("Boxer with its sidecar", Course.BOXER, null, true),
            new Model("Roadster", Course.CAR_CHASSIS, Course.ROADSTER, false),
            new Model("Van", Course.CAR_CHASSIS, Course.VAN, false),
            new Model("Pickup", Course.PICKUP_CHASSIS, Course.PICKUP_BODY, false),
            new Model("Truck", Course.TRUCK_CHASSIS, Course.TRUCK_CAB, false),
            new Model("Scooter", Course.SCOOTER, null, false),
            new Model("Sport bike", Course.SPORT_BIKE, null, false),
            new Model("Cruiser", Course.CRUISER, null, false),
            new Model("Boxer", Course.BOXER, null, false));

    /** What is being shaped: the sidecar's chassis, or the motorcycle's own frame. */
    private static final String SIDECAR = "The sidecar's chassis";
    private static final String BIKE = "The motorcycle's frame";
    /** Whether the game draws even the frames the bench shapes, as it has them: to see them as the game does. */
    private static final boolean AS_GAME = Boolean.getBoolean("model.asgame");

    private final Group shown = new Group();
    private final Group bikeFrame = new Group();
    private final SidecarModel sidecarModel = new SidecarModel();
    private final PerspectiveCamera eye = new PerspectiveCamera(true);
    private final Rotate yaw = new Rotate(-140, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(-18, Rotate.X_AXIS);
    private final Translate back = new Translate(0, 0, -650);
    private final Translate target = new Translate(55, -45, 0);
    private final PhongMaterial bikeSteel = matte("#1c1e21");
    private final PhongMaterial marked = glow();

    private Model model = MODELS.getFirst();
    private World world;
    private Play play;
    private Sidecar sidecar = Sidecar.BOXER;
    private Frame frame = Frames.BOXER;
    private boolean choosing;
    private double lastX;
    private double lastY;

    private final ComboBox<Model> models = new ComboBox<>();
    private final CheckBox showVehicle = new CheckBox("The vehicle (and the floor)");
    private final CheckBox showRods = new CheckBox("Rods");
    private final CheckBox showNodes = new CheckBox("Welds, clamps and pivots");
    private final CheckBox showWheel = new CheckBox("The sidecar's wheel");
    private final ComboBox<String> shaping = new ComboBox<>();
    private final RadioButton right = new RadioButton("Right");
    private final RadioButton left = new RadioButton("Left");
    private final Slider wheelAlong = EngineBench.slider(-110, 80, -70);
    private final Slider wheelOut = EngineBench.slider(90, 200, 132);
    private final Slider wheelSize = EngineBench.slider(18, 40, 28);
    private final ComboBox<String> nodes = new ComboBox<>();
    /** The nodes the sliders move, by their place in the frame: for the sidecar, not those that go wherever its wheel is set. */
    private final List<Integer> movable = new ArrayList<>();
    private final Slider across = EngineBench.slider(-80, 220, 0);
    private final Slider along = EngineBench.slider(-200, 200, 0);
    private final Slider up = EngineBench.slider(0, 140, 0);
    private final ComboBox<String> rods = new ComboBox<>();
    private final Slider rodTube = EngineBench.slider(0.5, 5, 1.4);
    private final ComboBox<String> joinTo = new ComboBox<>();
    private final Label place = new Label();
    private final Label figures = new Label();
    private final TextArea text = new TextArea();
    private final VBox sidecarControls = new VBox(8);
    private final VBox editing = new VBox(8);
    private Stage stage;

    @Override
    public void start(final Stage stage) {
        this.stage = stage;
        eye.setNearClip(1);
        eye.setFarClip(200_000);
        eye.setFieldOfView(40);
        eye.getTransforms().addAll(target, yaw, pitch, back);
        yaw.setAngle(Double.parseDouble(System.getProperty("model.yaw", "-140")));
        pitch.setAngle(-Double.parseDouble(System.getProperty("model.pitch", "18")));
        back.setZ(-Double.parseDouble(System.getProperty("model.distance", "650")));
        final Group scene3d = new Group(shown, bikeFrame, sidecarModel.node(), eye);
        final SubScene view = new SubScene(scene3d, 760, 640, true, SceneAntialiasing.BALANCED);
        view.setFill(Color.web("#b9d3e6"));
        view.setCamera(eye);
        final Pane holder = new Pane(view);
        view.widthProperty().bind(holder.widthProperty());
        view.heightProperty().bind(holder.heightProperty());
        orbit(view);

        models.getItems().setAll(MODELS);
        final String picked = System.getProperty("model.pick");
        if (picked != null) {
            final String wanted = picked.toLowerCase(Locale.ROOT);
            model = wanted.startsWith("side") ? MODELS.getFirst() : MODELS.stream().skip(1)
                    .filter(m -> m.name().toLowerCase(Locale.ROOT).startsWith(wanted)).findFirst()
                    .orElse(MODELS.getFirst());
        }
        models.setValue(model);
        models.setOnAction(e -> pick(models.getValue()));
        for (final CheckBox box : List.of(showVehicle, showRods, showNodes, showWheel)) {
            box.setSelected(true);
            box.setOnAction(e -> redraw());
        }
        shaping.getItems().setAll(SIDECAR, BIKE);
        shaping.setValue("frame".equalsIgnoreCase(System.getProperty("model.shape")) ? BIKE : SIDECAR);
        shaping.setOnAction(e -> listNodes());
        final ToggleGroup sides = new ToggleGroup();
        right.setToggleGroup(sides);
        left.setToggleGroup(sides);
        right.setSelected(true);
        sides.selectedToggleProperty().addListener((o, was, now) -> {
            sidecar = sidecar.on(now == left ? Sidecar.Side.LEFT : Sidecar.Side.RIGHT);
            target.setX(55 * sidecar.side().sign());
            yaw.setAngle(-yaw.getAngle());
            redraw();
        });
        if ("left".equalsIgnoreCase(System.getProperty("model.side"))) {
            left.setSelected(true);
        }
        for (final Slider slider : List.of(wheelAlong, wheelOut, wheelSize)) {
            slider.valueProperty().addListener((o, was, now) -> setWheel());
        }
        nodes.setOnAction(e -> pickNode());
        for (final Slider slider : List.of(across, along, up)) {
            slider.valueProperty().addListener((o, was, now) -> moveNode());
        }
        rods.setOnAction(e -> pickRod());
        rodTube.valueProperty().addListener((o, was, now) -> sizeRod());
        final Button split = new Button("Split it");
        split.setOnAction(e -> splitRod());
        final Button remove = new Button("Take it away");
        remove.setOnAction(e -> removeRod());
        final Button join = new Button("Join to");
        join.setOnAction(e -> joinNodes());
        place.setFont(Font.font("Monospaced", 12));
        figures.setFont(Font.font("Monospaced", 12));
        figures.setWrapText(true);
        text.setPrefRowCount(7);
        text.setFont(Font.font("Monospaced", 11));
        final Button show = new Button("As text");
        show.setOnAction(e -> text.setText(shapingSidecar() ? sidecar.text() : frame.text()));
        final Button save = new Button("Save…");
        save.setOnAction(e -> save());
        final Button open = new Button("Open…");
        open.setOnAction(e -> open());
        final Button reset = new Button("Back as it was");
        reset.setOnAction(e -> {
            if (shapingSidecar()) {
                sidecar = Sidecar.BOXER.on(left.isSelected() ? Sidecar.Side.LEFT : Sidecar.Side.RIGHT);
            } else {
                frame = Frames.BOXER;
            }
            listNodes();
        });

        sidecarControls.getChildren().addAll(EngineBench.row("Mounted on", new HBox(14, right, left)),
                EngineBench.label("Its wheel, on the outer rail (as far along as it reaches):"),
                EngineBench.row("Along", wheelAlong), EngineBench.row("Out", wheelOut), EngineBench.row("Size", wheelSize));
        editing.getChildren().addAll(EngineBench.heading("Shape"), shaping, sidecarControls,
                EngineBench.label("A node (across, out for the sidecar; along; up):"), nodes,
                EngineBench.row("Across", across), EngineBench.row("Along", along), EngineBench.row("Up", up), place,
                EngineBench.label("A rod:"), rods, EngineBench.row("Tube (cm)", rodTube), new HBox(8, split, remove),
                new HBox(8, join, joinTo), figures, new HBox(8, show, save, open, reset), text);

        final VBox controls = new VBox(10, EngineBench.heading("On show"), models, showVehicle,
                new HBox(12, showRods, showNodes), showWheel,
                EngineBench.label("Drag to turn round it; right-drag (or Shift-drag) to move across it; scroll to"
                        + " come nearer."), editing);
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
        final Scene scene = new Scene(root, 1220, 860);

        pick(model);
        listNodes();
        new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(final long now) {
                final double seconds = last < 0 ? 0 : Math.min(0.1, (now - last) / 1e9);
                last = now;
                world.show(play.damage(), play.moving(), play.lifts(), seconds, yaw.getAngle());
            }
        }.start();

        stage.setTitle("Vehicle bench");
        stage.setScene(scene);
        stage.show();
        SoundBench.snapshotIfAsked(scene);
    }

    /** Another vehicle on show, in a world of its own, standing in the middle of the floor facing +z. */
    private void pick(final Model picked) {
        model = picked;
        final Course course = Course.of(List.of(Course.Piece.vehicle(picked.chassis(), picked.body(),
                new Place(0, 0, picked.chassis().ride(), 0), 0)));
        // a frame of rods the bench shapes is drawn by the bench, as it is now, not as the game has it
        world = new World(course, AS_GAME || !shapesBike());
        final Damage damage = Damage.of(course);
        // whoever looks at it stands well off, out of sight
        play = new Play(new Avatar(Body.standing(new Vec3(0, 0, -100_000), 0), false), damage);
        shown.getChildren().setAll(world.node());
        editing.setDisable(!picked.sidecar() && Frames.of(picked.chassis()) == null);
        if (!picked.sidecar() && Frames.of(picked.chassis()) != null) {
            shaping.setValue(BIKE);
        }
        listNodes();
    }

    /** Whether the vehicle on show is one the bench draws the frame of, as it shapes it. */
    private boolean shapesBike() {
        return Frames.of(model.chassis()) != null;
    }

    private boolean shapingSidecar() {
        return model.sidecar() && SIDECAR.equals(shaping.getValue());
    }

    /** The frame being shaped, as the sliders have it. */
    private Frame shapedFrame() {
        return shapingSidecar() ? sidecar.frame() : frame;
    }

    /** What is being shaped changed: its nodes and rods listed afresh. */
    private void listNodes() {
        sidecarControls.setDisable(!shapingSidecar());
        final Frame shaped = shapedFrame();
        movable.clear();
        final List<String> names = new ArrayList<>();
        for (int n = 0; n < shaped.nodes().size(); n++) {
            if (!shapingSidecar() || !sidecar.follows(n)) {
                movable.add(n);
                names.add(shaped.nodes().get(n).name());
            }
        }
        choosing = true;
        nodes.getItems().setAll(names);
        nodes.getSelectionModel().select(0);
        joinTo.getItems().setAll(shaped.nodes().stream().map(Frame.Node::name).toList());
        joinTo.getSelectionModel().select(0);
        final List<String> rodNames = new ArrayList<>();
        for (final Frame.Rod rod : shaped.rods()) {
            rodNames.add(shaped.nodes().get(rod.from()).name() + " – " + shaped.nodes().get(rod.to()).name());
        }
        rods.getItems().setAll(rodNames);
        rods.getSelectionModel().select(0);
        wheelAlong.setValue(sidecar.along());
        wheelOut.setValue(sidecar.out());
        wheelSize.setValue(sidecar.radius());
        choosing = false;
        pickNode();
        pickRod();
    }

    private int pickedNode() {
        return movable.isEmpty() ? -1 : movable.get(Math.max(0, nodes.getSelectionModel().getSelectedIndex()));
    }

    private int pickedRod() {
        return Math.max(0, rods.getSelectionModel().getSelectedIndex());
    }

    /** Which node the sliders move: its place set on them, without moving it. */
    private void pickNode() {
        if (choosing || pickedNode() < 0) {
            return;
        }
        final Frame.Node node = shapedFrame().nodes().get(pickedNode());
        choosing = true;
        across.setValue(node.across());
        along.setValue(node.along());
        up.setValue(node.up());
        choosing = false;
        redraw();
    }

    private void pickRod() {
        if (choosing || shapedFrame().rods().isEmpty()) {
            return;
        }
        choosing = true;
        rodTube.setValue(shapedFrame().tube(shapedFrame().rods().get(pickedRod())).radius());
        choosing = false;
    }

    /** The frame being shaped, now {@code reshaped}. */
    private void reshape(final Frame reshaped) {
        if (shapingSidecar()) {
            sidecar = sidecar.with(reshaped);
        } else {
            frame = reshaped;
        }
    }

    private void moveNode() {
        if (choosing || pickedNode() < 0) {
            return;
        }
        reshape(shapedFrame().moved(pickedNode(), across.getValue(), along.getValue(), up.getValue()));
        redraw();
    }

    private void sizeRod() {
        if (choosing || shapedFrame().rods().isEmpty()) {
            return;
        }
        final double r = rodTube.getValue();
        final Frame.Tube was = shapedFrame().tube(shapedFrame().rods().get(pickedRod()));
        reshape(shapedFrame().sized(pickedRod(), new Frame.Tube(r, Math.min(was.wall(), r * 0.4))));
        redraw();
    }

    private void splitRod() {
        if (shapedFrame().rods().isEmpty()) {
            return;
        }
        reshape(shapedFrame().split(pickedRod(), "node " + (shapedFrame().nodes().size() + 1)));
        relist();
    }

    private void removeRod() {
        if (shapedFrame().rods().isEmpty()) {
            return;
        }
        reshape(shapedFrame().removed(pickedRod()));
        relist();
    }

    private void joinNodes() {
        final int to = joinTo.getSelectionModel().getSelectedIndex();
        if (pickedNode() < 0 || to < 0 || to == pickedNode()) {
            return;
        }
        reshape(shapedFrame().joined(pickedNode(), to));
        relist();
    }

    /** Nodes or rods added or taken away: listed afresh, the same ones picked where they still are. */
    private void relist() {
        final int node = nodes.getSelectionModel().getSelectedIndex();
        final int rod = rods.getSelectionModel().getSelectedIndex();
        listNodes();
        nodes.getSelectionModel().select(Math.min(node, nodes.getItems().size() - 1));
        rods.getSelectionModel().select(Math.min(rod, rods.getItems().size() - 1));
        pickNode();
        pickRod();
    }

    /** The sidecar's wheel set where the sliders say, and as big. */
    private void setWheel() {
        if (choosing) {
            return;
        }
        sidecar = sidecar.wheel(wheelAlong.getValue(), wheelOut.getValue()).wheeled(wheelSize.getValue());
        redraw();
    }

    private void redraw() {
        world.node().setVisible(showVehicle.isSelected());
        final int node = shapingSidecar() ? -1 : pickedNode();
        bikeFrame.getChildren().setAll(shapesBike()
                ? FrameModel.of(frame, bikeSteel, showNodes.isSelected(), node, marked) : new Group());
        bikeFrame.setVisible(!AS_GAME && showVehicle.isSelected() && showRods.isSelected());
        sidecarModel.node().setVisible(model.sidecar());
        sidecarModel.show(sidecar, shapingSidecar() ? pickedNode() : -1);
        sidecarModel.showing(showRods.isSelected(), showNodes.isSelected(), showWheel.isSelected());
        place.setText(String.format("across %6.1f  along %6.1f  up %6.1f", across.getValue(), along.getValue(),
                up.getValue()));
        final Frame shaped = shapedFrame();
        final Frame.Point centre = shaped.centre();
        final StringBuilder out = new StringBuilder(String.format(
                "%s: %d nodes, %d rods, %.2f m of tube, %.1f kg%nits mass at across %.0f, along %.0f, up %.0f",
                shaped.name(), shaped.nodes().size(), shaped.rods().size(), shaped.steel() / 100, shaped.mass(),
                centre.across(), centre.along(), centre.up()));
        out.append(shaped.slack() == 0 ? "\nbraced: it holds its shape by its rods alone"
                : String.format("%nheld by its welds: pinned, it could move %d ways", shaped.slack()));
        if (shapingSidecar()) {
            final double rear = Course.BOXER.axles().getLast();
            out.append(String.format("%non the %s; wheel %.0f cm out, %.0f cm %s the back wheel",
                    sidecar.side().toString().toLowerCase(Locale.ROOT), sidecar.track(),
                    Math.abs(sidecar.lead(rear)), sidecar.lead(rear) >= 0 ? "ahead of" : "behind"));
        }
        shaped.problems().forEach(p -> out.append("\n! ").append(p));
        figures.setText(out.toString());
    }

    private FileChooser chooser() {
        final FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Frames", "*.frame", "*.sidecar"));
        chooser.setInitialFileName(shapingSidecar() ? "sidecar.sidecar" : "boxer.frame");
        return chooser;
    }

    /** What is being shaped, saved as text. */
    private void save() {
        final File file = chooser().showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), shapingSidecar() ? sidecar.text() : frame.text(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            figures.setText("Could not save: " + e.getMessage());
        }
    }

    /** What is being shaped, opened from text, saved before. */
    private void open() {
        final File file = chooser().showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            final String saved = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (shapingSidecar()) {
                sidecar = Sidecar.read(saved);
                choosing = true;
                (sidecar.side() == Sidecar.Side.LEFT ? left : right).setSelected(true);
                choosing = false;
            } else {
                frame = Frame.read(saved);
            }
            listNodes();
        } catch (IOException | IllegalArgumentException e) {
            figures.setText("Could not open: " + e.getMessage());
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
        view.setOnScroll(e -> back.setZ(Math.clamp(back.getZ() * Math.pow(1.0015, -e.getDeltaY()), -6000, -80)));
    }

    private static PhongMaterial matte(final String colour) {
        final PhongMaterial material = new PhongMaterial(Color.web(colour));
        material.setSpecularColor(Color.web("#3a3f46"));
        return material;
    }

    private static PhongMaterial glow() {
        final PhongMaterial material = new PhongMaterial(Color.web("#ffb347"));
        material.setSelfIlluminationMap(null);
        material.setSpecularColor(Color.web("#ffd9a0"));
        return material;
    }
}
