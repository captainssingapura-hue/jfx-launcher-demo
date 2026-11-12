package com.example.fxsuite.avatar;

import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.hear.Hearing;
import com.example.fxsuite.avatar.look.BoxRig;
import com.example.fxsuite.avatar.look.Gait;
import com.example.fxsuite.avatar.look.Impacts;
import com.example.fxsuite.avatar.look.Reticle;
import com.example.fxsuite.avatar.look.World;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;
import com.example.fxsuite.avatar.view.Chase;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
 * <p>The keys move the avatar without turning it: W/S ahead and back, A/D sideways. It goes
 * at full speed, a run (or full boost), unless Shift is held, which walks (or flies at cruising
 * speed), as in most shooters. Space jumps (or climbs while flying), Ctrl descends. J puts the
 * {@link JetPack} on or takes it off; wearing it, F takes off and flies as a drone does, or stops flying. It
 * starts without the pack; {@code -Davatar.pack=true} starts with it on.
 * The mouse turns it, with no button held ({@link MouseLook}); clicks are kept for actions.
 * Esc frees the mouse or takes it back. Hold Q or E to swing the camera round the avatar; C
 * puts it back behind. Scroll to zoom.
 *
 * <p>For runs without a keyboard: {@code -Davatar.script="F;W:2;W+D+SHIFT:1.5"} holds each
 * step's keys for its seconds (a step without seconds is a tap, and {@code :1} just waits); an
 * {@code @90} among a step's keys turns 90° over the step, as the mouse would, and
 * {@code ^20} tilts the view up 20° (down, when negative).
 * {@code -Davatar.snapshot=out.png} saves the window just before the script ends and exits;
 * {@code -Davatar.settle=<ms>} picks the moment instead. Either leaves the mouse alone.
 */
public final class AvatarDemo extends Application {

    private static final double FIELD_OF_VIEW = 48;
    private static final double BOOST_VIEW = 14;
    /** How fast Q and E circle the camera round the avatar, degrees per second. */
    private static final double CIRCLE = 100;

    private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
    private final BoxRig rig = new BoxRig();
    private final Course course = new Course();
    private final Obstacles obstacles = course.obstacles();
    private final World world = new World(course);
    private final Impacts impacts = new Impacts();
    private final Reticle reticle = new Reticle();
    private final PerspectiveCamera eye = new PerspectiveCamera(true);
    private final Translate eyeAt = new Translate();
    private final Rotate eyeYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate eyePitch = new Rotate(0, Rotate.X_AXIS);
    private final Label status = new Label();

    private Avatar avatar = new Avatar(Body.standing(Vec3.ZERO, Course.START_YAW), Boolean.getBoolean("avatar.pack"));
    /** The camera: behind, at its usual distance, or {@code -Davatar.zoom} times it (0.5 twice as near). */
    private Chase chase = Chase.behind(eyeOf(avatar.body()), avatar.body().yaw())
            .zoomed(Double.parseDouble(System.getProperty("avatar.zoom", "1")));
    private MouseLook look;
    private Hearing hearing;
    /** Degrees per second the script is turning, as the mouse would. */
    private double scriptedTurn;
    /** Degrees per second the script is tilting the view, as the mouse would. */
    private double scriptedTilt;
    /** The trigger: the left mouse button held while the mouse is captured, or a script's FIRE. */
    private boolean clicking;
    private boolean scriptedTrigger;
    private double fieldOfView = FIELD_OF_VIEW;
    private String shown = "";

    @Override
    public void start(final Stage stage) {
        eye.setNearClip(10);
        eye.setFarClip(60_000);
        eye.setFieldOfView(FIELD_OF_VIEW);
        eye.getTransforms().addAll(eyeAt, eyeYaw, eyePitch);

        final Group scene3d = new Group(world.node(), rig.node(), rig.effects(), impacts.node(), reticle.node(), eye);
        final SubScene view = new SubScene(scene3d, 0, 0, true,
                SceneAntialiasing.BALANCED);
        view.setFill(World.SKY);
        view.setCamera(eye);
        view.setOnScroll(e -> chase = chase.zoomed(Math.pow(1.0015, -e.getDeltaY())));

        status.setStyle("-fx-text-fill: #f4f6f8; -fx-font-size: 15px; -fx-font-weight: bold;");
        final Label help = new Label("Mouse turns · W/S ahead and back · A/D sideways · Shift walk or fly slower · "
                + "Space jump or climb · Ctrl descend\n"
                + "J jet-pack on or off · F fly or land (with the jet-pack) · "
                + "Q/E look round · C camera behind · scroll to zoom · Esc frees the mouse · "
                + "M mute · P sound profile\n"
                + "1 twin rifles · 2 glaive · 3 Gatling gun (again to put away) · click to fire or swing");
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
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && look.captured()) {
                clicking = true;
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                clicking = false;
            }
        });

        stage.setTitle("FxSuite · Avatar");
        stage.setScene(scene);
        stage.show();
        // unattended runs leave the real mouse alone
        final boolean unattended = System.getProperty("avatar.script") != null
                || System.getProperty("avatar.snapshot") != null;
        look = new MouseLook(stage, scene, !unattended);
        hearing = Hearing.open(unattended);

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

    @Override
    public void stop() {
        if (hearing != null) {
            hearing.close();
        }
    }

    /** One frame: move the body, follow with the camera, pose the figure, hear it. */
    private void frame(final double seconds) {
        final Controls controls = new Controls(
                axis(KeyCode.W) - axis(KeyCode.S),
                axis(KeyCode.D) - axis(KeyCode.A),
                look.takeTurn() + scriptedTurn * seconds,
                axis(KeyCode.SPACE) - axis(KeyCode.CONTROL),
                // full speed by default, as in most shooters; Shift held to go carefully
                !held.contains(KeyCode.SHIFT),
                clicking || scriptedTrigger,
                // aimed up or down as far as the view is tilted from rest, so level as it walks
                chase.pitch() - Chase.DEFAULT_PITCH);
        avatar = avatar.step(controls, seconds, obstacles);
        final Body body = avatar.body();
        final double tilt = look.takeTilt() + scriptedTilt * seconds;
        final double circling = axis(KeyCode.E) - axis(KeyCode.Q);
        if (tilt != 0 || circling != 0) {
            chase = chase.turned(circling * CIRCLE * seconds, tilt);
        }
        chase = chase.follow(eyeOf(body), body.yaw(), 1, seconds, circling != 0);

        rig.show(avatar, Gait.of(avatar, chase.yaw(), chase.pitch() - Chase.DEFAULT_PITCH));
        impacts.show(avatar);
        reticle.show(avatar, obstacles, chase.position());
        world.shadowUnder(body, obstacles);
        hearing.hear(avatar, chase);

        // boosting widens the view a little, for a sense of speed
        final double boost = body.flying()
                ? Num.clamp((body.speed() - Body.FLY) / (Body.BOOST - Body.FLY), 0, 1) : 0;
        fieldOfView = Num.ease(fieldOfView, FIELD_OF_VIEW + BOOST_VIEW * boost, 4, seconds);
        eye.setFieldOfView(fieldOfView);
        final Vec3 at = chase.position();
        eyeAt.setX(at.x());
        eyeAt.setY(at.y());
        eyeAt.setZ(at.z());
        eyeYaw.setAngle(chase.yaw());
        eyePitch.setAngle(chase.pitch());

        showStatus();
        // every output has seen this frame's events; what happens next starts a fresh list
        avatar = avatar.delivered();
    }

    private void showStatus() {
        final Body body = avatar.body();
        final double metres = body.groundSpeed() / 100;
        // measured against the top speed each way, so a diagonal is not mistaken for a sprint
        final double pace = Math.max(Math.abs(body.speed()), Math.abs(body.drift()) / Body.SIDESTEP);
        final String what = body.flying()
                ? (pace > Body.FLY * 1.1 ? "Boosting" : "Flying")
                : !body.grounded() ? "In the air"
                : metres < 0.1 ? "Standing"
                : Math.abs(body.drift()) > Math.abs(body.speed()) ? "Side-stepping"
                : pace > Body.WALK * 1.2 ? "Running" : "Walking";
        final String sound = hearing.output() == Speakers.Kind.SILENT ? "no sound"
                : hearing.muted() ? "sound muted" : hearing.profile().title() + " sounds";
        final String weapon = avatar.holding() == Weapon.NONE ? "" : " · " + avatar.holding().title();
        final String text = String.format("%s · %.1f m/s · %.1f m up · %s%s · %s%s", what, metres, body.height() / 100,
                avatar.wearingPack() ? "jet-pack on" : "no jet-pack: J to put it on", weapon, sound,
                look.captured() ? "" : " · mouse free: Esc to turn with it");
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
            case SPACE -> avatar = avatar.jumped();
            case F -> avatar = avatar.toggledFlight();
            case J -> avatar = avatar.toggledPack();
            case DIGIT1 -> avatar = avatar.drawn(Weapon.RIFLES);
            case DIGIT2 -> avatar = avatar.drawn(Weapon.GLAIVE);
            case DIGIT3 -> avatar = avatar.drawn(Weapon.GATLING);
            case C -> chase = Chase.behind(chase.pivot(), avatar.body().yaw()).zoomed(chase.distance() / Chase.DEFAULT_DISTANCE);
            case ESCAPE -> look.toggle();
            case M -> hearing.toggleMute();
            case P -> hearing.nextProfile();
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

    private static Vec3 eyeOf(final Body body) {
        return body.position().add(0, -Chase.EYE_HEIGHT, 0);
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
                    if (name.trim().equalsIgnoreCase("FIRE")) {
                        // the trigger, as the left mouse button held
                        steps.getKeyFrames().add(new KeyFrame(Duration.seconds(t), e -> scriptedTrigger = true));
                        steps.getKeyFrames().add(new KeyFrame(Duration.seconds(t + hold), e -> scriptedTrigger = false));
                        continue;
                    }
                    if (name.trim().startsWith("^")) {
                        final double rate = Double.parseDouble(name.trim().substring(1)) / hold;
                        steps.getKeyFrames().add(new KeyFrame(Duration.seconds(t), e -> scriptedTilt = rate));
                        steps.getKeyFrames().add(new KeyFrame(Duration.seconds(t + hold), e -> scriptedTilt = 0));
                        continue;
                    }
                    if (name.trim().startsWith("@")) {
                        final double rate = Double.parseDouble(name.trim().substring(1)) / hold;
                        steps.getKeyFrames().add(new KeyFrame(Duration.seconds(t), e -> scriptedTurn = rate));
                        steps.getKeyFrames().add(new KeyFrame(Duration.seconds(t + hold), e -> scriptedTurn = 0));
                        continue;
                    }
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
