package com.example.fxsuite.avatar;

import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.hear.Hearing;
import com.example.fxsuite.avatar.look.BoxRig;
import com.example.fxsuite.avatar.look.Gait;
import com.example.fxsuite.avatar.look.Impacts;
import com.example.fxsuite.avatar.look.Reticle;
import com.example.fxsuite.avatar.look.World;
import com.example.fxsuite.avatar.look.Wreckage;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Journal;
import com.example.fxsuite.avatar.sim.Keys;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Recording;
import com.example.fxsuite.avatar.sim.Script;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;
import com.example.fxsuite.avatar.view.Chase;

import javafx.animation.AnimationTimer;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
 * part's keys for its seconds (a part without seconds is a tap, and {@code :1} just waits); an
 * {@code @90} among a part's keys turns 90° over the part, as the mouse would, and
 * {@code ^20} tilts the view up 20° (down, when negative). A {@link Script} is inputs stamped
 * with the step they come in at: while it plays, it alone drives the world, so the same script
 * gives the same run however the frames fall.
 * {@code -Davatar.snapshot=out.png} saves the window just before the script ends and exits;
 * {@code -Davatar.settle=<ms>} picks the moment instead, counted in the world's time. Either
 * leaves the mouse alone.
 */
public final class AvatarDemo extends Application {

    private static final double FIELD_OF_VIEW = 48;
    private static final double BOOST_VIEW = 14;
    /** How fast Q and E circle the camera round the avatar, degrees per second. */
    private static final double CIRCLE = 100;

    private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
    private final BoxRig rig = new BoxRig();
    private final Course course = new Course();
    /** What the weapons have done to the course, and so what still stands in it. */
    private Damage damage = Damage.of(course);
    private final World world = new World(course);
    private final Impacts impacts = new Impacts();
    private final Wreckage wreckage = new Wreckage();
    private final Reticle reticle = new Reticle();
    private final PerspectiveCamera eye = new PerspectiveCamera(true);
    private final Translate eyeAt = new Translate();
    private final Rotate eyeYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate eyePitch = new Rotate(0, Rotate.X_AXIS);
    private final Label status = new Label();
    /** How the frames are going, a second at a time: green when smooth, amber when not. */
    private final Label frames = new Label();
    private final FrameTimes frameTimes = new FrameTimes();
    private static final String FRAMES_STYLE = "-fx-font-size: 12px; -fx-font-family: 'Consolas', monospace; "
            + "-fx-text-fill: ";

    /** A run to play back, {@code -Davatar.replay}: its inputs drive the world from where it started; null for none. */
    private final Recording replay = readReplay();
    /** The first step a replay was found astray at, by its checksums; 0 while it is the same run. */
    private long astray;
    /** Where this run's inputs are being kept, {@code -Davatar.inputs}; null when they are not. */
    private Recording.Recorder recorder;
    /** The avatar after the latest step of the {@link Clock}, the one before it, and as it is shown between them. */
    private Avatar avatar = Play.start(replay != null ? replay.pack() : Boolean.getBoolean("avatar.pack")).avatar();
    private Avatar before = avatar;
    private Avatar seen = avatar;
    private Clock clock = Clock.START;
    /** How many steps the world has taken: what each step's events are stamped with. */
    private long step;
    /**
     * What happened, step by step: the simulation writes each step's events here, and the effects
     * and the sound each read it through a reader of their own, at their own pace.
     */
    private final Journal journal = new Journal();
    private final Journal.Reader forImpacts = journal.reader();
    private final Journal.Reader forWreckage = journal.reader();
    private final Journal.Reader forSound = journal.reader();
    /** Degrees of the mouse's turn not yet taken by a step. */
    private double turning;
    /** The keys pressed since the last step, in order: the next step takes what they do once (a jump, a take-off, the pack, a weapon drawn). */
    private final List<String> pressed = new ArrayList<>();
    /** The script playing, {@code -Davatar.script}, or one that is over at once. */
    private final Script script = Script.parse(System.getProperty("avatar.script", ""));
    /** The keys the script holds at the latest step, for what only the camera takes notice of. */
    private Set<String> scriptHeld = Set.of();
    /** Where to save the window, {@code -Davatar.snapshot}, and at which step; null for no snapshot. */
    private final String snapshot = System.getProperty("avatar.snapshot");
    private long snapshotAt;
    private boolean snapshotTaken;
    private Scene scene;
    /** The camera: behind, at its usual distance, or {@code -Davatar.zoom} times it (0.5 twice as near). */
    private Chase chase = Chase.behind(eyeOf(avatar.body()), avatar.body().yaw())
            .zoomed(Double.parseDouble(System.getProperty("avatar.zoom", "1")));
    private MouseLook look;
    private Hearing hearing;
    /** The trigger: the left mouse button held while the mouse is captured. */
    private boolean clicking;
    private double fieldOfView = FIELD_OF_VIEW;
    private String shown = "";

    @Override
    public void start(final Stage stage) {
        eye.setNearClip(10);
        eye.setFarClip(60_000);
        eye.setFieldOfView(FIELD_OF_VIEW);
        eye.getTransforms().addAll(eyeAt, eyeYaw, eyePitch);

        final Group scene3d = new Group(world.node(), rig.node(), rig.effects(), impacts.node(), wreckage.node(),
                reticle.node(), eye);
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
        frames.setStyle(FRAMES_STYLE + "#b9f6ca;");
        frames.setText("measuring frames…");
        final VBox hud = new VBox(4, status, frames, help);
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
        this.scene = scene;
        // a script naming a key there is no such thing as fails here, not halfway through
        script.keys().forEach(KeyCode::valueOf);
        // by default just before the script's last part ends, while its keys are still held
        final Long settle = Long.getLong("avatar.settle");
        snapshotAt = settle != null ? Math.round(settle / 1000.0 / Clock.STEP)
                : replay != null ? replay.length() - Math.round(0.06 / Clock.STEP)
                : script.length() > 0 ? script.length() - Math.round(0.06 / Clock.STEP)
                : Math.round(0.8 / Clock.STEP);
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
        final boolean unattended = System.getProperty("avatar.script") != null || replay != null
                || System.getProperty("avatar.snapshot") != null;
        look = new MouseLook(stage, scene, !unattended);
        hearing = Hearing.open(unattended);
        final String inputs = System.getProperty("avatar.inputs");
        if (inputs != null) {
            try {
                recorder = new Recording.Recorder(Files.newBufferedWriter(Path.of(inputs)), avatar.wearingPack());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        frame(0);
        // unattended runs also print each second's frame times, for measuring without watching
        final boolean logFrames = unattended;
        new AnimationTimer() {
            private long last = -1;

            @Override
            public void handle(final long now) {
                final double seconds = last < 0 ? 0 : Math.min(0.1, (now - last) / 1e9);
                last = now;
                final long began = System.nanoTime();
                frame(seconds);
                final FrameTimes.Summary summary = frameTimes.frame(now, System.nanoTime() - began);
                if (summary != null) {
                    showFrames(summary);
                    if (logFrames) {
                        System.out.println("frames: " + summary.text().replace('·', '|'));
                    }
                }
            }
        }.start();
    }

    @Override
    public void stop() {
        if (hearing != null) {
            hearing.close();
        }
        if (recorder != null) {
            recorder.close();
            System.out.println("kept the inputs of " + step + " steps");
        }
    }

    /**
     * One frame: the world stepped on by its own {@link Clock}, as many fixed steps as this
     * frame's time holds; then everything else (the camera, the figure, the effects, the sound)
     * shown as the world stands between its last two steps.
     */
    private void frame(final double seconds) {
        // the mouse's turn this frame waits until a step takes it, and is shared among the steps
        turning += look.takeTurn();
        final Clock.Ticked ticked = clock.advance(seconds);
        clock = ticked.clock();
        for (int i = 0; i < ticked.steps(); i++) {
            final double turn = turning / (ticked.steps() - i);
            turning -= turn;
            final long next = step + 1;
            final Controls controls;
            if (replay != null && next <= replay.length()) {
                // while a recording plays back, it alone drives the world, and the view tilts with its aim
                controls = replay.controls(next);
                chase = chase.turned(0, controls.aim() - (chase.pitch() - Chase.DEFAULT_PITCH));
                scriptHeld = Set.of();
                pressed.clear();
            } else if (next <= script.length()) {
                // while a script plays, it alone drives the world: its inputs for this very step
                controls = script.controls(next);
                scriptHeld = script.held(next);
                script.pressed(next).forEach(key -> view(KeyCode.valueOf(key)));
                chase = chase.turned(0, script.tilt(next));
                pressed.clear();
            } else {
                scriptHeld = Set.of();
                controls = Keys.controls(names(held), i == 0 ? pressed : List.of(), turn, clicking,
                        // aimed up or down as far as the view is tilted from rest, so level as it walks
                        chase.pitch() - Chase.DEFAULT_PITCH);
                // what was pressed since the last step, all at the first step that comes
                pressed.clear();
            }
            before = avatar;
            // the avatar moves, its blows wear the world down, and what happened goes to the journal
            final Play.Stepped stepped = new Play(avatar, damage).step(controls, Clock.STEP);
            avatar = stepped.play().avatar();
            damage = stepped.play().damage();
            journal.append(++step, stepped.events());
            if (recorder != null) {
                recorder.step(step, controls, stepped.play());
            }
            if (replay != null && step <= replay.length()) {
                checkReplay(stepped.play());
            }
        }
        // what is seen and heard: the world between its last two steps, so it glides rather than ticks
        seen = avatar.between(before, ticked.blend());
        final Avatar avatar = seen;
        final Body body = avatar.body();
        final double tilt = look.takeTilt();
        final double circling = axis(KeyCode.E) - axis(KeyCode.Q);
        if (tilt != 0 || circling != 0) {
            chase = chase.turned(circling * CIRCLE * seconds, tilt);
        }
        chase = chase.follow(eyeOf(body), body.yaw(), 1, seconds, circling != 0);

        rig.show(avatar, Gait.of(avatar, chase.yaw(), chase.pitch() - Chase.DEFAULT_PITCH));
        // each reads what is new to it in the journal
        impacts.show(avatar, forImpacts.read().events());
        wreckage.show(avatar, forWreckage.read().events(), world, damage.obstacles());
        world.show(damage, seconds, chase.yaw());
        reticle.show(avatar, damage.obstacles(), chase.position());
        world.shadowUnder(body, damage.obstacles());
        // the airships still in the sky drone from the middle of their hulls
        hearing.hear(avatar, forSound.read().events(), chase, damage.standing(Course.Kind.AIRSHIP).stream()
                .map(a -> new Vec3(a.block().x(), -Course.hullHeight(a), a.block().z())).toList());

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
        snapshotIfDue();
    }

    private void showStatus() {
        final Body body = seen.body();
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
        final int targets = damage.count(Course.Kind.DUMMY) + damage.count(Course.Kind.DRONE)
                + damage.count(Course.Kind.AIRSHIP);
        final int down = damage.broken(Course.Kind.DUMMY) + damage.broken(Course.Kind.DRONE)
                + damage.broken(Course.Kind.AIRSHIP);
        final int crates = damage.broken(Course.Kind.CRATE);
        final String score = String.format(" · targets down %d of %d%s", down, targets,
                crates == 0 ? "" : crates == 1 ? " · 1 crate broken" : " · " + crates + " crates broken");
        final String run = replay != null && step <= replay.length() ? (astray == 0 ? " · replaying" : " · replay astray")
                : recorder != null ? " · keeping inputs" : "";
        final String text = String.format("%s · %.1f m/s · %.1f m up · %s%s%s · %s%s%s", what, metres,
                body.height() / 100 + 0.0, avatar.wearingPack() ? "jet-pack on" : "no jet-pack: J to put it on", weapon,
                score, sound, run, look.captured() ? "" : " · mouse free: Esc to turn with it");
        if (!text.equals(shown)) {
            shown = text;
            status.setText(text);
        }
    }

    /** The last second's frames on the HUD, amber when they fell short of a smooth 60. */
    private void showFrames(final FrameTimes.Summary summary) {
        frames.setText(summary.text());
        frames.setStyle(FRAMES_STYLE + (summary.struggling() ? "#ffd180;" : "#b9f6ca;"));
    }

    private void press(final KeyCode code) {
        if (!held.add(code)) {
            return;  // a key held down repeats; act on the first press only
        }
        // what it does to the world waits for the next step, as an input to it (Keys)
        pressed.add(code.name());
        view(code);
    }

    /** What a key does outside the world: the camera, the mouse and the sound. */
    private void view(final KeyCode code) {
        switch (code) {
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

    /** Whether a key is held, at the keyboard or by the script: for what only the camera takes notice of. */
    private double axis(final KeyCode code) {
        return held.contains(code) || scriptHeld.contains(code.name()) ? 1 : 0;
    }

    private static Set<String> names(final Set<KeyCode> codes) {
        final Set<String> names = new HashSet<>();
        codes.forEach(code -> names.add(code.name()));
        return names;
    }

    /** Reads {@code -Davatar.replay}, if set. */
    private static Recording readReplay() {
        final String path = System.getProperty("avatar.replay");
        if (path == null) {
            return null;
        }
        try (Reader in = Files.newBufferedReader(Path.of(path))) {
            final Recording recording = Recording.read(in);
            System.out.println("replaying " + path + ": " + recording.length() + " steps, "
                    + recording.checks() + " checksums" + (recording.rules() == Play.RULES ? ""
                    : ", made under rules " + recording.rules() + " where these are " + Play.RULES));
            return recording;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Whether the world after this step of a replay is as it was when it was kept. */
    private void checkReplay(final Play after) {
        if (astray == 0 && !replay.matches(step, after)) {
            astray = step;
            System.out.println("replay astray: the world differs by step " + step);
        }
        if (step == replay.length()) {
            System.out.println(astray == 0
                    ? "replay the same run: all " + replay.checks() + " checksums matched, to step " + step
                    : "replay astray by step " + astray);
        }
    }

    private static Vec3 eyeOf(final Body body) {
        return body.position().add(0, -Chase.EYE_HEIGHT, 0);
    }

    /** Saves the window to {@code -Davatar.snapshot}, once its step has come, and exits. */
    private void snapshotIfDue() {
        if (snapshot == null || snapshotTaken || step < snapshotAt) {
            return;
        }
        snapshotTaken = true;
        // once this pulse has drawn what the step left
        Platform.runLater(() -> {
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
                ImageIO.write(out, "png", new File(snapshot));
                System.out.println("saved " + snapshot + " at step " + step);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
        });
    }
}
