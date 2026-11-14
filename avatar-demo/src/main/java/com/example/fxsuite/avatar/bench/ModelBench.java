package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.look.SidecarModel;
import com.example.fxsuite.avatar.look.World;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
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
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A bench for working on the vehicles' models, apart from the game: any of the game's vehicles on
 * show on their own, drawn exactly as the game draws them ({@link World}, from a world with nothing
 * else in it), to look at from any side; and the sidecar's chassis ({@link Sidecar}), alongside the
 * boxer, to shape.
 *
 * <ul>
 *   <li><b>Look round it:</b> drag to turn round it, right-drag (or Shift-drag) to move across it,
 *       scroll to come nearer or go further off.</li>
 *   <li><b>Shape the sidecar:</b> pick a joint of its frame, or its wheel's hub, and move it across,
 *       along and up by its sliders; the tubes follow. Its tubes' size and its wheel's too. Its
 *       figures are read out as it changes: how much steel, how heavy, how far out its wheel is and
 *       how far ahead of the boxer's back wheel. When it looks right, print it as code, to paste
 *       back into {@link Sidecar#BOXER}.</li>
 * </ul>
 *
 * <p>{@code -Dmodel.pick=sidecar} (or the start of any vehicle's name: {@code boxer}, {@code van},
 * ...) picks what is on show to start with, {@code -Dmodel.side=left} mounts the sidecar on the
 * left; {@code -Dmodel.yaw=40} and {@code -Dmodel.pitch=20} where it is seen from (-140, from ahead
 * on its right), {@code -Dmodel.distance=600} how far off; {@code -Dbench.snapshot=out.png} saves
 * the window after {@code -Dbench.settle} ms and exits.
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

    private final Group shown = new Group();
    private final SidecarModel sidecarModel = new SidecarModel();
    private final PerspectiveCamera eye = new PerspectiveCamera(true);
    private final Rotate yaw = new Rotate(30, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(-18, Rotate.X_AXIS);
    private final Translate back = new Translate(0, 0, -650);
    private final Translate target = new Translate(55, -45, 0);

    private Model model = MODELS.getFirst();
    private World world;
    private Play play;
    private Sidecar sidecar = Sidecar.BOXER;
    private boolean choosing;
    private double lastX;
    private double lastY;

    private final ComboBox<Model> models = new ComboBox<>();
    private final CheckBox showVehicle = new CheckBox("The vehicle (and the floor)");
    private final CheckBox showFrame = new CheckBox("The sidecar's tubes");
    private final CheckBox showJoints = new CheckBox("Its welds and clamps");
    private final CheckBox showWheel = new CheckBox("Its wheel");
    private final javafx.scene.control.RadioButton right = new javafx.scene.control.RadioButton("Right");
    private final javafx.scene.control.RadioButton left = new javafx.scene.control.RadioButton("Left");
    private final ComboBox<String> joints = new ComboBox<>();
    /** The joints the sliders move, by their place in the frame: not those that go wherever the wheel is set. */
    private final List<Integer> movable = new ArrayList<>();
    private final Slider across = EngineBench.slider(-60, 220, 0);
    private final Slider along = EngineBench.slider(-200, 200, 0);
    private final Slider up = EngineBench.slider(0, 140, 0);
    private final Slider tube = EngineBench.slider(1, 4, 1.9);
    private final Slider radius = EngineBench.slider(18, 40, 28);
    private final Slider wheelAlong = EngineBench.slider(-110, 80, -70);
    private final Slider wheelOut = EngineBench.slider(90, 200, 132);
    private final Label place = new Label();
    private final Label figures = new Label();
    private final TextArea code = new TextArea();
    private final VBox shaping = new VBox(8);

    @Override
    public void start(final Stage stage) {
        eye.setNearClip(1);
        eye.setFarClip(200_000);
        eye.setFieldOfView(40);
        eye.getTransforms().addAll(target, yaw, pitch, back);
        // seen from ahead of it on its right, where a sidecar is
        yaw.setAngle(Double.parseDouble(System.getProperty("model.yaw", "-140")));
        pitch.setAngle(-Double.parseDouble(System.getProperty("model.pitch", "18")));
        back.setZ(-Double.parseDouble(System.getProperty("model.distance", "650")));
        final Group scene3d = new Group(shown, sidecarModel.node(), eye);
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
        for (final CheckBox box : List.of(showVehicle, showFrame, showJoints, showWheel)) {
            box.setSelected(true);
            box.setOnAction(e -> seen());
        }
        final List<String> names = new ArrayList<>();
        for (int j = 0; j < sidecar.joints().size(); j++) {
            if (!sidecar.follows(j)) {
                movable.add(j);
                names.add(sidecar.joints().get(j).name());
            }
        }
        joints.getItems().setAll(names);
        final javafx.scene.control.ToggleGroup sides = new javafx.scene.control.ToggleGroup();
        right.setToggleGroup(sides);
        left.setToggleGroup(sides);
        right.setSelected(true);
        sides.selectedToggleProperty().addListener((o, was, now) -> {
            sidecar = sidecar.on(now == left ? Sidecar.Side.LEFT : Sidecar.Side.RIGHT);
            target.setX(55 * sidecar.side().sign());
            yaw.setAngle(-yaw.getAngle());
            redraw();
        });
        for (final Slider slider : List.of(wheelAlong, wheelOut)) {
            slider.valueProperty().addListener((o, was, now) -> moveWheel());
        }
        joints.getSelectionModel().select(0);
        joints.setOnAction(e -> picking());
        if ("left".equalsIgnoreCase(System.getProperty("model.side"))) {
            left.setSelected(true);
        }
        for (final Slider slider : List.of(across, along, up)) {
            slider.valueProperty().addListener((o, was, now) -> moveJoint());
        }
        tube.valueProperty().addListener((o, was, now) -> resize());
        radius.valueProperty().addListener((o, was, now) -> resize());
        place.setFont(Font.font("Monospaced", 12));
        figures.setFont(Font.font("Monospaced", 12));
        figures.setWrapText(true);
        code.setPrefRowCount(8);
        code.setEditable(false);
        code.setFont(Font.font("Monospaced", 11));
        final Button print = new Button("Print it as code");
        print.setOnAction(e -> {
            code.setText(sidecar.code());
            System.out.println(sidecar.code());
        });
        final Button reset = new Button("Back as it was");
        reset.setOnAction(e -> {
            sidecar = Sidecar.BOXER.on(left.isSelected() ? Sidecar.Side.LEFT : Sidecar.Side.RIGHT);
            choosing = true;
            tube.setValue(sidecar.tube());
            radius.setValue(sidecar.radius());
            wheelAlong.setValue(sidecar.along());
            wheelOut.setValue(sidecar.out());
            choosing = false;
            picking();
        });
        shaping.getChildren().addAll(EngineBench.heading("The sidecar"), showFrame, showJoints, showWheel,
                EngineBench.row("Mounted on", new HBox(14, right, left)),
                EngineBench.label("Its wheel, on the outer rail (as far along as it reaches):"),
                EngineBench.row("Along", wheelAlong), EngineBench.row("Out", wheelOut),
                EngineBench.label("Move a joint of its frame (out from the motorcycle, along, up):"), joints,
                EngineBench.row("Out", across), EngineBench.row("Along", along), EngineBench.row("Up", up), place,
                EngineBench.row("Tubes (cm)", tube), EngineBench.row("Wheel (cm)", radius), figures,
                new HBox(8, print, reset), code);

        final VBox controls = new VBox(10, EngineBench.heading("On show"), models, showVehicle,
                EngineBench.label("Drag to turn round it; right-drag (or Shift-drag) to move across it; scroll to"
                        + " come nearer."), shaping);
        controls.setPadding(new Insets(14));
        controls.setPrefWidth(360);
        controls.setMinWidth(360);
        final BorderPane root = new BorderPane();
        root.setLeft(controls);
        root.setCenter(holder);
        root.setStyle("-fx-background-color: #f4f5f7;");
        final Scene scene = new Scene(root, 1180, 820);

        pick(model);
        picking();
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
        world = new World(course);
        final Damage damage = Damage.of(course);
        // whoever looks at it stands well off, out of sight
        play = new Play(new Avatar(Body.standing(new Vec3(0, 0, -100_000), 0), false), damage);
        shown.getChildren().setAll(world.node());
        shaping.setDisable(!picked.sidecar());
        seen();
    }

    private void seen() {
        world.node().setVisible(showVehicle.isSelected());
        sidecarModel.node().setVisible(model.sidecar());
        sidecarModel.showing(showFrame.isSelected(), showJoints.isSelected(), showWheel.isSelected());
    }

    /** Which joint the sliders move: its place set on them, without moving it. */
    private void picking() {
        final Sidecar.Joint joint = sidecar.joints().get(picked());
        choosing = true;
        across.setValue(joint.out());
        along.setValue(joint.along());
        up.setValue(joint.up());
        choosing = false;
        redraw();
    }

    /** The joint picked, moved to where the sliders say. */
    private void moveJoint() {
        if (choosing) {
            return;
        }
        final int index = picked();
        final Sidecar.Joint was = sidecar.joints().get(index);
        sidecar = sidecar.moved(index, was.at(across.getValue(), along.getValue(), up.getValue()));
        redraw();
    }

    /** The joint the sliders move, by its place in the frame. */
    private int picked() {
        return movable.get(Math.max(0, joints.getSelectionModel().getSelectedIndex()));
    }

    /** Its wheel set where the sliders say: along the outer rail, as far as it reaches, and out. */
    private void moveWheel() {
        if (choosing) {
            return;
        }
        sidecar = sidecar.wheel(wheelAlong.getValue(), wheelOut.getValue());
        redraw();
    }

    /** Its tubes, or its wheel, made bigger or smaller. */
    private void resize() {
        if (choosing) {
            return;
        }
        sidecar = sidecar.sized(tube.getValue(), radius.getValue());
        redraw();
    }

    private void redraw() {
        sidecarModel.show(sidecar, picked());
        place.setText(String.format("out %6.1f  along %6.1f  up %6.1f", across.getValue(), along.getValue(),
                up.getValue()));
        final double rear = Course.BOXER.axles().getLast();
        final double lowest = sidecar.joints().stream().mapToDouble(j -> j.up() - sidecar.tube()).min().orElse(0);
        figures.setText(String.format("on the %s; %d tubes, %.2f m of steel, %.1f kg%nwheel %.0f cm out, %.0f cm"
                        + " %s the back wheel%nlowest tube %.0f cm up%s",
                sidecar.side().toString().toLowerCase(Locale.ROOT), sidecar.bars().size(), sidecar.steel() / 100,
                sidecar.mass(), sidecar.track(), Math.abs(sidecar.lead(rear)),
                sidecar.lead(rear) >= 0 ? "ahead of" : "behind", lowest, sidecar.connected() ? "" : "\nNOT ONE FRAME: a joint is loose"));
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
}
