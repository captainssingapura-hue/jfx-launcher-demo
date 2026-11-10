package com.example.fxsuite.avatar;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * A box-rig avatar in a small world, followed by a chase camera.
 *
 * <p>W/S move, A/D turn, Shift runs (or boosts), Space jumps (or climbs while flying), Ctrl
 * descends, F takes off or stops flying. Drag, or hold Q or E, to circle the camera round the
 * avatar; C puts it back behind. Scroll to zoom.
 *
 * <p>For runs without a keyboard: {@code -Davatar.script="F;W+SHIFT:2;W+D:1.5"} holds each
 * step's keys for its seconds (a step without seconds is a tap, and {@code :1} just waits), and
 * {@code -Davatar.snapshot=out.png} saves the window just before the script ends and exits.
 * {@code -Davatar.settle=<ms>} picks the moment instead.
 */
public final class AvatarDemo extends Application {

    private static final double FIELD_OF_VIEW = 48;
    private static final double BOOST_VIEW = 14;
    /** How fast Q and E circle the camera round the avatar, degrees per second. */
    private static final double CIRCLE = 100;

    private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
    private final BoxRig rig = new BoxRig();
    private final World world = new World();
    private final PerspectiveCamera eye = new PerspectiveCamera(true);
    private final Translate eyeAt = new Translate();
    private final Rotate eyeYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate eyePitch = new Rotate(0, Rotate.X_AXIS);
    private final Label status = new Label();

    private Body body = Body.standing(Point3D.ZERO, 30);
    private Chase chase = Chase.behind(eyeOf(body), body.yaw());
    private boolean dragging;
    private double dragX;
    private double dragY;
    private double fieldOfView = FIELD_OF_VIEW;
    private String shown = "";

    @Override
    public void start(final Stage stage) {
        eye.setNearClip(10);
        eye.setFarClip(60_000);
        eye.setFieldOfView(FIELD_OF_VIEW);
        eye.getTransforms().addAll(eyeAt, eyeYaw, eyePitch);

        final SubScene view = new SubScene(new Group(world.node(), rig.node(), eye), 0, 0, true,
                SceneAntialiasing.BALANCED);
        view.setFill(World.SKY);
        view.setCamera(eye);
        lookAround(view);

        status.setStyle("-fx-text-fill: #f4f6f8; -fx-font-size: 15px; -fx-font-weight: bold;");
        final Label help = new Label("W/S move · A/D turn · Shift run or boost · Space jump or climb · "
                + "Ctrl descend · F fly or land · drag or Q/E circle the camera · C camera behind · scroll to zoom");
        help.setStyle("-fx-text-fill: #e6eaee; -fx-font-size: 12px;");
        final VBox hud = new VBox(4, status, help);
        hud.setPadding(new Insets(10, 14, 10, 14));
        hud.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        hud.setStyle("-fx-background-color: rgba(20, 24, 30, 0.55); -fx-background-radius: 6;");
        hud.setMouseTransparent(true);

        final StackPane root = new StackPane(view, hud);
        StackPane.setAlignment(hud, Pos.TOP_LEFT);
        StackPane.setMargin(hud, new Insets(12));
        view.widthProperty().bind(root.widthProperty());
        view.heightProperty().bind(root.heightProperty());

        final Scene scene = new Scene(root, 1280, 800);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> press(e.getCode()));
        scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> release(e.getCode()));

        stage.setTitle("FxSuite · Avatar");
        stage.setScene(scene);
        stage.show();

        frame(0);
        new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(final long now) {
                final double seconds = last < 0 ? 0 : Math.min(0.1, (now - last) / 1e9);
                last = now;
                frame(seconds);
            }
        }.start();

        final double scripted = playScript();
        snapshotIfAsked(scene, scripted);
    }

    /** One frame: move the body, follow with the camera, pose the figure. */
    private void frame(final double seconds) {
        final Controls controls = new Controls(
                axis(KeyCode.W) - axis(KeyCode.S),
                axis(KeyCode.D) - axis(KeyCode.A),
                axis(KeyCode.SPACE) - axis(KeyCode.CONTROL),
                held.contains(KeyCode.SHIFT));
        body = body.step(controls, seconds);
        final double moving = Body.clamp(Math.abs(body.speed()) / Body.WALK, 0, 1);
        final double circling = axis(KeyCode.E) - axis(KeyCode.Q);
        if (circling != 0) {
            chase = chase.turned(circling * CIRCLE * seconds, 0);
        }
        chase = chase.follow(eyeOf(body), body.yaw(), moving, seconds, dragging || circling != 0);

        rig.show(body, Gait.of(body, chase.yaw(), chase.pitch() - Chase.DEFAULT_PITCH));
        world.shadowUnder(body);

        // boosting widens the view a little, for a sense of speed
        final double boost = body.flying()
                ? Body.clamp((body.speed() - Body.FLY) / (Body.BOOST - Body.FLY), 0, 1) : 0;
        fieldOfView = Body.ease(fieldOfView, FIELD_OF_VIEW + BOOST_VIEW * boost, 4, seconds);
        eye.setFieldOfView(fieldOfView);
        final Point3D at = chase.position();
        eyeAt.setX(at.getX());
        eyeAt.setY(at.getY());
        eyeAt.setZ(at.getZ());
        eyeYaw.setAngle(chase.yaw());
        eyePitch.setAngle(chase.pitch());

        showStatus();
    }

    private void showStatus() {
        final double metres = Math.abs(body.speed()) / 100;
        final String what = body.flying()
                ? (body.speed() > Body.FLY * 1.1 ? "Boosting" : "Flying")
                : !body.grounded() ? "In the air"
                : metres < 0.1 ? "Standing"
                : Math.abs(body.speed()) > Body.WALK * 1.2 ? "Running" : "Walking";
        final String text = String.format("%s · %.1f m/s · %.1f m up", what, metres, body.height() / 100);
        if (!text.equals(shown)) {
            shown = text;
            status.setText(text);
        }
    }

    private void press(final KeyCode code) {
        if (!held.add(code)) {
            return;  // a key held down repeats; act on the first press only
        }
        switch (code) {
            case SPACE -> body = body.jumped();
            case F -> body = body.toggledFlight();
            case C -> chase = Chase.behind(chase.pivot(), body.yaw()).zoomed(chase.distance() / Chase.DEFAULT_DISTANCE);
            default -> {
            }
        }
    }

    private void release(final KeyCode code) {
        held.remove(code);
    }

    private double axis(final KeyCode code) {
        return held.contains(code) ? 1 : 0;
    }

    /** Drag to turn the camera round the avatar, scroll to zoom. */
    private void lookAround(final SubScene view) {
        view.setOnMousePressed(e -> {
            dragging = true;
            dragX = e.getSceneX();
            dragY = e.getSceneY();
        });
        view.setOnMouseDragged(e -> {
            chase = chase.turned((e.getSceneX() - dragX) * 0.3, -(e.getSceneY() - dragY) * 0.25);
            dragX = e.getSceneX();
            dragY = e.getSceneY();
        });
        view.setOnMouseReleased(e -> dragging = false);
        view.setOnScroll(e -> chase = chase.zoomed(Math.pow(1.0015, -e.getDeltaY())));
    }

    private static Point3D eyeOf(final Body body) {
        return body.position().add(0, -BoxRig.EYE_HEIGHT, 0);
    }

    /**
     * Plays {@code -Davatar.script}, if set.
     *
     * @return when it ends, in seconds from now, or 0 without one
     */
    private double playScript() {
        final String script = System.getProperty("avatar.script");
        if (script == null || script.isBlank()) {
            return 0;
        }
        final Timeline steps = new Timeline();
        double t = 0.5;
        for (final String step : script.split(";")) {
            final String[] parts = step.trim().split(":");
            final double hold = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : 0.15;
            if (!parts[0].isBlank()) {
                for (final String name : parts[0].split("\\+")) {
                    final KeyCode code = KeyCode.valueOf(name.trim().toUpperCase());
                    steps.getKeyFrames().add(new KeyFrame(Duration.seconds(t), e -> press(code)));
                    steps.getKeyFrames().add(new KeyFrame(Duration.seconds(t + hold), e -> release(code)));
                }
            }
            t += hold;
        }
        steps.play();
        return t;
    }

    /** Saves the window to {@code -Davatar.snapshot}, if set, and exits. */
    private static void snapshotIfAsked(final Scene scene, final double scripted) {
        final String path = System.getProperty("avatar.snapshot");
        if (path == null) {
            return;
        }
        // by default just before the script's last step ends, while its keys are still held
        final long settle = Long.getLong("avatar.settle", scripted > 0 ? Math.round(scripted * 1000) - 60 : 800);
        final PauseTransition wait = new PauseTransition(Duration.millis(settle));
        wait.setOnFinished(e -> {
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
        wait.play();
    }
}
