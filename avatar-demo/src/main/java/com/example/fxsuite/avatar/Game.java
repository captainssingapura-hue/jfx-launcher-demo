package com.example.fxsuite.avatar;

import com.example.fxsuite.avatar.audio.Speakers;
import com.example.fxsuite.avatar.hear.Hearing;
import com.example.fxsuite.avatar.look.Bombs;
import com.example.fxsuite.avatar.look.BoxRig;
import com.example.fxsuite.avatar.look.Gait;
import com.example.fxsuite.avatar.look.Guns;
import com.example.fxsuite.avatar.look.Pose;
import com.example.fxsuite.avatar.look.Impacts;
import com.example.fxsuite.avatar.look.Reticle;
import com.example.fxsuite.avatar.look.Stall;
import com.example.fxsuite.avatar.look.World;
import com.example.fxsuite.avatar.look.Wreckage;
import com.example.fxsuite.avatar.sim.Avatar;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Journal;
import com.example.fxsuite.avatar.sim.Keys;
import com.example.fxsuite.avatar.sim.Gun;
import com.example.fxsuite.avatar.sim.Hit;
import com.example.fxsuite.avatar.sim.Lift;
import com.example.fxsuite.avatar.sim.Turret;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Person;
import com.example.fxsuite.avatar.sim.Play;
import com.example.fxsuite.avatar.sim.Prop;
import com.example.fxsuite.avatar.sim.Recording;
import com.example.fxsuite.avatar.sim.Script;
import com.example.fxsuite.avatar.sim.Sky;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Weapon;
import com.example.fxsuite.avatar.sound.Cues;
import com.example.fxsuite.avatar.sound.Engine;
import com.example.fxsuite.avatar.sound.Gearbox;
import com.example.fxsuite.avatar.sound.Vehicle;
import com.example.fxsuite.avatar.view.Chase;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public final class Game {

    private static final double FIELD_OF_VIEW = 48;
    private static final double BOOST_VIEW = 14;
    /** How fast Q and E circle the camera round the avatar, degrees per second. */
    private static final double CIRCLE = 100;

    private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);
    private final BoxRig rig = new BoxRig();
    /** Which demo this is, and its world. */
    private final Demo demo;
    private final Course course;
    /** Back to the choice of demos. */
    private final Runnable back;
    /** Over to another demo, or this one again from the start. */
    private final java.util.function.Consumer<Demo> switchTo;
    /** The menu bar along the top: the demos, the view, the sound. Right clicks are left for the game. */
    private final MenuBar menuBar = new MenuBar();
    private AnimationTimer timer;
    /** What the weapons have done to the course, and so what still stands in it. */
    private Damage damage;
    /** What moves of itself: the airships along their routes, and their bombs. */
    private Sky sky = Sky.START;
    /** The platforms moved from their consoles. */
    private List<Lift> lifts;
    /** The people, as they are after the latest step and the one before: drawn between them. */
    private List<Person> people;
    private List<Person> peopleBefore;
    /** The loose things: the furniture and the fruit. */
    private List<Prop> props;
    /** The guns on their mounts, and how they are drawn. */
    private List<Turret> turrets;
    private final Guns guns;
    /**
     * How far back the camera is from a gun being manned, how far over it, and how far it looks down
     * at rest: inside a bunker, close behind it and next to level, to look out under its roof; at an
     * open gun, further back and over the gunner's head, looking down a little.
     */
    private static final double GUN_VIEW = 260;
    private static final double GUN_EYE = 30;
    /** How much higher the camera looks from over a vehicle wearing a body. */
    private static final double BODY_OVER = 140;
    /** Whether the view at a vehicle's controls is first person (V), and how far the head is turned from ahead, in a land vehicle. */
    private boolean firstPerson;
    private double headYaw;
    /** How far round a driver's head turns, either way. */
    private static final double HEAD_TURN = 120;
    /** Behind a gun, in first person: how far back of where it turns, and how far up. */
    private static final double SIGHT_BACK = 70;
    private static final double SIGHT_UP = 34;
    /** How hard the land vehicle driven was worked, and how fast it was going, at the last frame; and
     * which gear it is in ({@link #gearbox}), and of which vehicle. */
    private Gearbox.Geared geared;
    private int gearedPiece = -1;
    /** A land vehicle's instruments. */
    private final Dashboard dashboard = new Dashboard();
    private final VehiclePanel panel = new VehiclePanel(this::panelPressed);
    private double engineLoad;
    private double lastAhead;
    /** The controls the last step was taken with: the throttle a driven engine is heard to have. */
    private Controls lastControls = Controls.NONE;
    private static final double GUN_REST = -4;
    private static final double OPEN_GUN_VIEW = 560;
    private static final double OPEN_GUN_EYE = 170;
    private static final double OPEN_GUN_REST = -12;
    /** What the camera follows, while manning a gun: this and up, by what carries the gun; apart from any vehicle. */
    private static final int GUNNING = 1_000_000;
    /** How much deeper the heavy Gatling gun sounds than the hand-held one. */
    private static final double GUN_PITCH = 0.72;
    /** And the giant one under the airship, deeper still. */
    private static final double GIANT_PITCH = 0.55;
    /** How far out a gun's sight is marked, at most. */
    private static final double GUN_SIGHT = 6000;
    /** Where each piece that moves was after the step before the latest, and after the latest: drawn between them. */
    private Map<Integer, Block> movedBefore = Map.of();
    private Map<Integer, Block> movedNow = Map.of();
    private final World world;
    private final Impacts impacts;
    private final Wreckage wreckage = new Wreckage();
    /** The people as drawn, and what they sit on at the stall on the barge. */
    private final Stall stall;
    private final Bombs bombs = new Bombs();
    private final Reticle reticle = new Reticle();
    private final PerspectiveCamera eye = new PerspectiveCamera(true);
    private final Translate eyeAt = new Translate();
    private final Rotate eyeYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate eyePitch = new Rotate(0, Rotate.X_AXIS);
    private final Label status = new Label();
    /** The keys, for whatever the avatar is doing: on foot, or at the controls of each kind of vehicle. */
    private final Label help = new Label();
    private static final String VIEW_KEYS = "Q/E look round · C camera behind · V first person, at the controls ·"
            + " scroll to zoom · Esc frees the mouse · "
            + "M mute · P sound profile · Tab: the demos (or the menu bar)";
    private static final String ON_FOOT_HELP = "Mouse turns · W/S ahead and back · A/D sideways · Shift walk or fly slower · "
            + "Space jump or climb · Ctrl descend\n"
            + "J jet-pack on or off · F fly or land (with the jet-pack) · " + VIEW_KEYS + "\n"
            + "1 twin rifles · 2 glaive · 3 Gatling gun (again to put away) · click to fire or swing · "
            + "T fly a platform (at its console), drive a vehicle (at its door), board an airship (under its hatch)"
            + " or man a gun"; 
    private static final String PLATFORM_HELP = "The platform: W/S ahead and back · A/D sideways · mouse turns · "
            + "Space climb · Ctrl descend · Shift carefully · F engine on or off · T let go\n"
            + "1 twin rifles · 2 glaive · 3 Gatling gun · click to fire or swing\n" + VIEW_KEYS;
    private static final String DRIVE_HELP = "The chassis: W accelerate · S brake, then reverse · A/D steer ·"
            + " Space handbrake · Shift gently · T let go of the wheel\n"
            + "Only the front wheels steer: it turns about its back wheels, the tighter the slower it goes\n" + VIEW_KEYS;
    private static final String RIDE_HELP = "Astride: W throttle · S brake, then back · A/D steer ·"
            + " Space brake hard · Shift gently · T get off\n"
            + "It leans into its turns, the harder the faster it goes round; the gears change by themselves\n"
            + VIEW_KEYS;
    private static final String ARMED_DRIVE_HELP ="At the wheel: W accelerate · S brake, then reverse · A/D steer ·"
            + " Space handbrake · Shift gently · T let go of the wheel\n"
            + "Click fire its gun, fixed beside the seat: held, it spins up, then fires the way the car faces\n"
            + VIEW_KEYS;
    private static final String PICKUP_HELP = "The pickup: W accelerate · S brake, then reverse · A/D steer ·"
            + " Space handbrake · Shift gently · T let go\n"
            + "The mouse aims the gun at the back, all round, 10° down and 45° up; it holds its aim as the pickup"
            + " turns · click fire · G give up the gun\n" + VIEW_KEYS;
    private static final String GUNSHIP_HELP = "The gunship: W/S ahead and back · A/D turn · Space climb ·"
            + " Ctrl descend · Shift carefully · T let go\n"
            + "The mouse aims the gun, all round, 30° down and 40° up; it holds its aim as the gunship turns"
            + " · click fire: held, it spins up, then fires · G give up the gun\n" + VIEW_KEYS;
    private static final String ARMED_AIRSHIP_HELP = "The airship: W/S ahead and back · A/D turn (slowly) · Space climb ·"
            + " Ctrl descend · Shift slower · T step out (through the hatch) · G give up the gun\n"
            + "The mouse aims the giant gun under the gondola, all round, 80° down and 10° up; it holds its aim as the"
            + " airship turns · click fire: held, it spins up, then fires · right-click drop a bomb, hold for a stick"
            + "\n" + VIEW_KEYS;
    /** At the controls of a vehicle whose gun is not under control: how to take it back. */
    private static final String GUN_OFF_HELP = "\nG take control of the gun: the keys drive, A/D turning,"
            + " and the mouse aims it";
    private static final String GUN_HELP = "The heavy Gatling gun: the mouse aims it, 55° either way, 8° down and 25° up;"
            + " it swings round at its own pace · T step out\n"
            + "Click fire: held, it spins up, then fires\n" + VIEW_KEYS;
    private static final String AIRSHIP_HELP = "The airship: W/S ahead and back · mouse turns (slowly) · "
            + "Space climb · Ctrl descend · Shift slower · T step out (through the hatch)\n"
            + "Click drop a bomb, hold for a stick of them · moored, the bay loads a bomb back each second\n"
            + VIEW_KEYS;
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
    private Avatar avatar;
    private Avatar before;
    private Avatar seen;
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
    private final Journal.Reader forStall = journal.reader();
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
    /** The camera: behind the avatar, or, at a vehicle's controls, behind the vehicle. */
    private Chase chase;
    private MouseLook look;
    private Hearing hearing;
    /** The trigger: the left mouse button held while the mouse is captured. */
    private boolean clicking;
    /** The other button: the right mouse button held while the mouse is captured; aboard an airship, its bombs. */
    private boolean bombing;
    /** A bomb let go once, from the control panel, at the next step. */
    private boolean bombOnce;
    private double fieldOfView = FIELD_OF_VIEW;
    private String shown = "";
    /** The vehicle whose controls the avatar has, by piece, as last seen; -1 for none. */
    private int piloting = -1;
    /** How far back the camera is going to, easing there as the view changes hands; NaN when it stays put. */
    private double zoomTo = Double.NaN;
    /** How far back it was behind the avatar, to go back to on leaving a vehicle. */
    private double zoomWas = Chase.DEFAULT_DISTANCE;

    /**
     * Demo {@code demo}, played in {@code stage} from now; {@code back} goes back to the choice of
     * demos, and {@code switchTo} straight to another (or this one again, from the start).
     */
    public Game(final Stage stage, final Demo demo, final Runnable back,
                final java.util.function.Consumer<Demo> switchTo) {
        this.demo = demo;
        this.back = back;
        this.switchTo = switchTo;
        course = new Course(demo);
        damage = Damage.of(course);
        lifts = Lift.of(damage);
        people = Person.of(damage, lifts);
        peopleBefore = people;
        props = Prop.of(damage);
        turrets = Turret.of(damage);
        guns = new Guns(turrets);
        world = new World(course);
        impacts = new Impacts(world);
        stall = new Stall(course, wreckage);
        avatar = Play.start(demo, replay != null ? replay.pack() : Boolean.getBoolean("avatar.pack")).avatar();
        before = avatar;
        seen = avatar;
        // the camera: behind, at its usual distance, or -Davatar.zoom times it (0.5 twice as near)
        chase = Chase.behind(eyeOf(avatar.body()), avatar.body().yaw())
                .zoomed(Double.parseDouble(System.getProperty("avatar.zoom", "1")));
        start(stage);
    }

    /** Plays it in {@code stage}. */
    private void start(final Stage stage) {
        eye.setNearClip(10);
        eye.setFarClip(150_000);
        eye.setFieldOfView(FIELD_OF_VIEW);
        eye.getTransforms().addAll(eyeAt, eyeYaw, eyePitch);

        final Group scene3d = new Group(world.node(), rig.node(), rig.effects(), impacts.node(), wreckage.node(),
                bombs.node(), stall.node(), guns.node(),
                reticle.node(), eye);
        final SubScene view = new SubScene(scene3d, 0, 0, true,
                SceneAntialiasing.BALANCED);
        view.setFill(World.SKY);
        view.setCamera(eye);
        view.setOnScroll(e -> {
            zoomTo = Double.NaN;
            chase = chase.zoomed(Math.pow(1.0015, -e.getDeltaY()));
        });

        status.setStyle("-fx-text-fill: #f4f6f8; -fx-font-size: 15px; -fx-font-weight: bold;");
        help.setText(ON_FOOT_HELP);
        help.setStyle("-fx-text-fill: #e6eaee; -fx-font-size: 12px;");
        frames.setStyle(FRAMES_STYLE + "#b9f6ca;");
        frames.setText("measuring frames…");
        final VBox hud = new VBox(4, status, frames, help);
        hud.setPadding(new Insets(10, 14, 10, 14));
        hud.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        hud.setStyle("-fx-background-color: rgba(20, 24, 30, 0.55); -fx-background-radius: 6;");
        hud.setMouseTransparent(true);

        // the menu bar along the top, and under it the world, the HUD over it
        menus();
        final StackPane shown = new StackPane(view, hud, dashboard.node(), panel.node());
        // a vehicle's controls, low on the left, to click with the mouse freed
        StackPane.setAlignment(panel.node(), Pos.BOTTOM_LEFT);
        StackPane.setMargin(panel.node(), new Insets(0, 0, 20, 24));
        StackPane.setAlignment(hud, Pos.TOP_LEFT);
        StackPane.setMargin(hud, new Insets(12));
        final BorderPane root = new BorderPane(shown);
        root.setTop(menuBar);
        // a land vehicle's instruments, low on the right, as racing games have them
        StackPane.setAlignment(dashboard.node(), Pos.BOTTOM_RIGHT);
        StackPane.setMargin(dashboard.node(), new Insets(0, 24, 20, 0));
        view.widthProperty().bind(shown.widthProperty());
        view.heightProperty().bind(shown.heightProperty());

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
            } else if (e.getButton() == MouseButton.SECONDARY && look.captured()) {
                bombing = true;
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                clicking = false;
            } else if (e.getButton() == MouseButton.SECONDARY) {
                bombing = false;
            }
        });

        stage.setTitle("FxSuite · Avatar · " + demo.title());
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
                recorder = new Recording.Recorder(Files.newBufferedWriter(Path.of(inputs)), avatar.wearingPack(), demo);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        movedNow = new Play(avatar, damage, sky, lifts, people, props, turrets).moving();
        movedBefore = movedNow;
        frame(0);
        // unattended runs also print each second's frame times, for measuring without watching
        final boolean logFrames = unattended;
        timer = new AnimationTimer() {
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
        };
        timer.start();
    }

    /** Stops it: the frames, the sound, the mouse; and keeps the inputs, if they are being kept. */
    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (look != null) {
            look.close();
            look = null;
        }
        if (hearing != null) {
            hearing.close();
            hearing = null;
        }
        if (recorder != null) {
            recorder.close();
            recorder = null;
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
        final double mouse = look.takeTurn();
        turning += mouse;
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
                chase = chase.turned(0, controls.aim() - (chase.pitch() - restPitch()));
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
                        chase.pitch() - restPitch(), bombing || bombOnce);
                bombOnce = false;
                // what was pressed since the last step, all at the first step that comes
                pressed.clear();
            }
            before = avatar;
            lastControls = controls;
            // the avatar moves, its blows wear the world down, and what happened goes to the journal
            final Play.Stepped stepped = new Play(avatar, damage, sky, lifts, people, props, turrets).step(controls, Clock.STEP);
            avatar = stepped.play().avatar();
            damage = stepped.play().damage();
            sky = stepped.play().sky();
            lifts = stepped.play().lifts();
            peopleBefore = people;
            people = stepped.play().people();
            props = stepped.play().props();
            turrets = stepped.play().turrets();
            journal.append(++step, stepped.events());
            // where what moves now is, to draw it between its last two places, and to put marks on it
            movedBefore = movedNow;
            movedNow = stepped.play().moving();
            world.moved(step, movedNow);
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
        // at a vehicle's controls, the view is the vehicle's: behind it, as far back as it is big
        final Map<Integer, Block> shownAt = between(movedBefore, movedNow, ticked.blend());
        final Optional<Lift> flying = lifts.stream().filter(Lift::held).findFirst();
        // and manning a gun of its own (a bunker's), the gun's: behind it and over it, looking along
        // it; with a vehicle's gun under control, the view stays the vehicle's, the gun aimed from it
        final Optional<Turret> gunned = turrets.stream().filter(Turret::held).findFirst();
        final Optional<Turret> alone = gunned.filter(gun -> lifts.stream().noneMatch(l -> l.piece() == gun.host()));
        final boolean inside = flying.filter(lift -> lift.craft().closed()).isPresent()
                || alone.filter(gun -> gun.mount().closed()).isPresent();
        final int nowPiloting = flying.map(Lift::piece).orElse(alone.map(gun -> GUNNING + gun.host()).orElse(-1));
        if (nowPiloting != piloting) {
            if (piloting < 0) {
                zoomWas = chase.distance();
            }
            zoomTo = flying.map(Game::viewFrom)
                    .orElse(alone.map(gun -> gun.mount().closed() ? GUN_VIEW : OPEN_GUN_VIEW).orElse(zoomWas));
            // the view tipped to where the aim is level, as it rests for what is now looked along
            chase = chase.turned(0, restPitch() - chase.pitch());
            piloting = nowPiloting;
        }
        if (!Double.isNaN(zoomTo)) {
            final double d = Num.ease(chase.distance(), zoomTo, 3, seconds);
            chase = chase.zoomed(d / chase.distance());
            if (Math.abs(d - zoomTo) < 1) {
                zoomTo = Double.NaN;
            }
        }
        final Block craft = flying.isPresent() ? shownAt.get(piloting) : null;
        chase = alone.isPresent()
                ? chase.follow(alone.get().pivot().add(0, alone.get().mount().closed() ? -GUN_EYE : -OPEN_GUN_EYE, 0),
                alone.get().heading(), 1, seconds,
                circling != 0)
                : craft != null
                ? chase.follow(new Vec3(craft.x(), -(craft.bottom() + craft.top()) / 2 - overBody(flying.get()), craft.z()),
                craft.yaw(), 1,
                seconds, circling != 0)
                : chase.follow(eyeOf(body), body.yaw(), 1, seconds, circling != 0);

        // in first person at a vehicle's controls, the view is from where its driver's eyes are: the
        // seat, the console, the front of the gondola, or behind a gun; in a land vehicle the mouse,
        // which does not steer it, turns the head
        final boolean seated = flying.filter(lift -> lift.craft() instanceof Course.Chassis).isPresent()
                && gunned.isEmpty();
        headYaw = firstPerson && seated ? Num.clamp(headYaw + mouse, -HEAD_TURN, HEAD_TURN) : 0;
        final View cockpit = firstPerson ? cockpit(flying.orElse(null), alone.orElse(null), shownAt) : null;
        world.glassFrom(cockpit != null && flying.isPresent() ? flying.get().piece() : -1);
        final Vec3 viewer = cockpit != null ? cockpit.at() : chase.position();
        // inside a closed vehicle the avatar is out of sight: no figure, no shadow, no sights; looking
        // out from where its eyes are, its own figure is not seen either
        rig.node().setVisible(!inside && cockpit == null);
        rig.effects().setVisible(!inside && cockpit == null);
        reticle.node().setVisible(!inside);
        world.shadowShown(!inside);
        // at the wheel of a land vehicle, sat in its seat, facing the way it heads, as it is drawn now
        final Pose looking = Gait.of(avatar, chase.yaw(), chase.pitch() - Chase.DEFAULT_PITCH);
        final Lift driven = flying.filter(lift -> lift.craft() instanceof Course.Chassis).orElse(null);
        if (driven != null && craft != null) {
            // in a car, sat; on a two-wheeler, astride, leaning with it into its turns
            final Course.Chassis chassis = (Course.Chassis) driven.craft();
            // and on a banked turn, rolled with it
            final double tipped = driven.lean() + driven.roll();
            rig.show(seated(avatar, chassis, craft, tipped),
                    chassis.twoWheels() ? Gait.astride(looking, chassis, tipped) : Gait.seated(looking, tipped));
        } else {
            rig.show(avatar, looking);
        }
        // each reads what is new to it in the journal
        // what moves of itself is shown where it was as far behind its latest step as the avatar is
        final double behind = (1 - ticked.blend()) * Clock.STEP;
        final Play now = new Play(this.avatar, damage, sky, lifts, people, props, turrets);
        final Obstacles standing = now.obstacles();
        impacts.show(avatar, forImpacts.read().entries());
        wreckage.show(avatar, forWreckage.read().events(), world, standing);
        world.show(damage, shownAt, lifts, seconds, chase.yaw());
        bombs.show(sky, behind);
        stall.show(people, peopleBefore, ticked.blend(), forStall.read().events(), seconds);
        // the gun the player fires: one manned, or one fixed to what it drives
        final Optional<Turret> firing = gunned.or(() -> fixedGun(flying.orElse(null)));
        if (firing.isPresent()) {
            reticle.showAt(gunSight(firing.get(), standing), viewer);
        } else {
            reticle.show(avatar, standing, viewer);
        }
        // each gun drawn where what carries it is drawn, between its last two steps, as that is
        final Map<Integer, Vec3> lag = new HashMap<>();
        for (final Turret turret : turrets) {
            final Block was = movedNow.get(turret.host());
            final Block drawn = shownAt.get(turret.host());
            if (was != null && drawn != null) {
                lag.put(turret.host(), new Vec3(drawn.x() - was.x(), -(drawn.bottom() - was.bottom()), drawn.z() - was.z()));
            }
        }
        guns.show(turrets, damage, seconds, lag);
        world.shadowUnder(body, standing);
        // the airships still in the sky drone from the middle of their hulls
        // and so does one to be flown, while it stands
        final List<Course.Piece> droning = new ArrayList<>(now.airships());
        for (final Lift lift : lifts) {
            if (lift.craft().closed() && !damage.broken(lift.piece())) {
                droning.add(damage.piece(lift.piece()).placed(lift.place()));
            }
        }
        // a gun being manned is heard spinning and roaring from its muzzle, deeper than the hand-held one
        hearing.hear(avatar, forSound.read().events(), chase, droning.stream()
                .map(a -> new Vec3(a.block().x(), -Course.hullHeight(a), a.block().z())).toList(),
                firing.map(gun -> new Cues.Gunner(gun.gun().spin(), gun.firing(), gun.muzzle(),
                        gun.mount().gun() == Gun.GIANT_GATLING ? GIANT_PITCH : GUN_PITCH)).orElse(null),
                flying.filter(lift -> lift.craft() instanceof Course.Chassis).map(lift -> motor(lift, seconds)).orElse(null));
        // and its instruments, while one is driven
        instruments(flying.filter(lift -> lift.craft() instanceof Course.Chassis).orElse(null), seconds);

        // boosting widens the view a little, for a sense of speed
        final double boost = body.flying()
                ? Num.clamp((body.speed() - Body.FLY) / (Body.BOOST - Body.FLY), 0, 1) : 0;
        fieldOfView = Num.ease(fieldOfView, FIELD_OF_VIEW + BOOST_VIEW * boost, 4, seconds);
        eye.setFieldOfView(fieldOfView);
        final Vec3 at = cockpit != null ? cockpit.at() : chase.position();
        eyeAt.setX(at.x());
        eyeAt.setY(at.y());
        eyeAt.setZ(at.z());
        eyeYaw.setAngle(cockpit != null ? cockpit.yaw() : chase.yaw());
        eyePitch.setAngle(cockpit != null ? cockpit.pitch() : chase.pitch());

        showStatus();
        snapshotIfDue();
    }

    /**
     * Where each piece that moves is drawn: {@code share} of the way from where it was after the
     * step before the latest to where it is now, as the avatar is.
     */
    private static Map<Integer, Block> between(final Map<Integer, Block> before, final Map<Integer, Block> now,
                                               final double share) {
        final Map<Integer, Block> seen = new HashMap<>();
        for (final Map.Entry<Integer, Block> moved : now.entrySet()) {
            final Block was = before.get(moved.getKey());
            seen.put(moved.getKey(), was == null ? moved.getValue() : moved.getValue().between(was, share));
        }
        return seen;
    }

    /**
     * The gun being manned, in a few words: its barrels, its heat if it overheats, and how much is
     * left of what it is on; on a gunship, how fast and high that is going too.
     */
    private String gunStatus(final Turret gun) {
        final double spin = gun.gun().spin();
        final String barrels = gun.firing() ? "firing" : spin >= 1 ? "up to speed"
                : spin > 0 ? String.format("spinning, %.0f%%", 100 * spin) : "still";
        final String heat = !gun.mount().overheats() ? ""
                : String.format(" · heat %.0f%%%s", 100 * gun.heat(), gun.cooling() ? ", too hot: cooling" : "");
        final double left = 100 * damage.left(gun.host());
        final Lift ship = lifts.stream().filter(l -> l.piece() == gun.host()).findFirst().orElse(null);
        if (ship != null && ship.craft() instanceof Course.Chassis chassis) {
            final double speed = ship.ahead() / 100;
            return String.format("Driving the %s · %.1f m/s%s · wheels %+.0f° · gun %s%s · %.0f%% left", chassis.name(),
                    Math.abs(speed), speed < -0.05 ? " in reverse" : "", ship.steer(), barrels, heat, left);
        }
        if (ship != null && ship.craft().closed()) {
            return String.format("Flying the airship · %.1f m/s · %.1f m up%s · giant gun %s%s · hull %.0f%% · %s",
                    ship.speed() / 100, ship.place().height() / 100, ship.moored() ? ", moored" : "", barrels, heat,
                    left, bay(ship));
        }
        if (ship != null) {
            return String.format("Flying the gunship · %.1f m/s · %.1f m up · gun %s%s · %.0f%% left",
                    ship.speed() / 100, ship.place().height() / 100, barrels, heat, left);
        }
        return String.format("Manning the heavy Gatling gun · %s%s · bunker %.0f%% left", barrels, heat, left);
    }

    /** The vehicle being flown, in a few words: which it is, and how much of it is left. */
    private String vehicleStatus(final Play here, final Lift lift) {
        final double left = 100 * damage.left(lift.piece());
        if (lift.craft() instanceof Course.Chassis chassis) {
            final double speed = lift.ahead() / 100;
            final String gun = fixedGun(lift).map(g -> " · gun " + (g.firing() ? "firing" : g.gun().spin() >= 1
                    ? "up to speed" : g.gun().spin() > 0 ? String.format("spinning, %.0f%%", 100 * g.gun().spin()) : "still"))
                    .orElse("");
            if (chassis.twoWheels()) {
                // ridden, leaning into its turns
                return String.format("Riding the %s · %.1f m/s%s · bars %+.0f° · leaning %+.0f°%s · %.0f%% left",
                        chassis.name(), Math.abs(speed), speed < -0.05 ? " in reverse" : "", lift.steer(), lift.lean(),
                        lift.climb() > 0 || Math.abs(lift.tilt()) > 1
                                ? String.format(" · sloping %+.0f°", lift.tilt()) : "",
                        left);
            }
            // by its body, if it wears one: a van, a roadster; else its chassis, bare
            final Course.Bodywork body = damage.piece(lift.piece()).body();
            return String.format("Driving the %s · %.1f m/s%s · wheels %+.0f°%s%s · %.0f%% left",
                    body != null ? body.name() : chassis.name() + " chassis",
                    Math.abs(speed), speed < -0.05 ? " in reverse" : "", lift.steer(),
                    lift.climb() > 0 || Math.abs(lift.tilt()) > 1
                            ? String.format(" · sloping %+.0f°", lift.tilt()) : "",
                    gun, left);
        }
        if (lift.craft().closed()) {
            return String.format("Aboard the airship · %.1f m/s · %.1f m up%s · hull %.0f%% · %s", lift.speed() / 100,
                    lift.place().height() / 100, lift.moored() ? ", moored" : "", left, bay(lift));
        }
        return lift.engine()
                ? String.format("Flying the platform · %.1f m/s · %.1f m up · %.0f%% left", lift.speed() / 100,
                lift.place().height() / 100, left)
                : String.format("At the platform, engine off, %s · %.0f%% left", docked(here, lift), left);
    }

    /** An airship's bomb bay, in a few words: how many bombs are in it, and whether more are loading. */
    private static String bay(final Lift lift) {
        final int full = lift.craft().bombs();
        return lift.moored()
                ? String.format("bombs %d/%d, %s", lift.bombs(), full, lift.bombs() < full ? "loading" : "loaded")
                : lift.bombs() == 0 ? String.format("bombs 0/%d: moor to load more", full)
                : String.format("bombs %d/%d", lift.bombs(), full);
    }

    /** What platform {@code lift}, its engine cut, rests on, in a word or two; or that it is still coming down. */
    private String docked(final Play here, final Lift lift) {
        final Lift.Support under = here.under(lift);
        if (!lift.resting(under)) {
            return "coming down";
        }
        if (under.piece() < 0) {
            return "resting";
        }
        return switch (damage.piece(under.piece()).kind()) {
            case AIRSHIP -> "docked on an airship";
            case PLATFORM -> "docked on a platform";
            case FURNITURE, FRUIT -> "resting on something loose";
            default -> "resting on a block";
        };
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
        // an airship to be flown is a vehicle, not a target: its own state is shown at its controls
        final List<Lift> ships = lifts.stream().filter(l -> damage.piece(l.piece()).kind() == Course.Kind.AIRSHIP)
                .toList();
        final int targets = damage.count(Course.Kind.DUMMY) + damage.count(Course.Kind.DRONE)
                + damage.count(Course.Kind.AIRSHIP) - ships.size();
        final int down = damage.broken(Course.Kind.DUMMY) + damage.broken(Course.Kind.DRONE)
                + damage.broken(Course.Kind.AIRSHIP) - (int) ships.stream().filter(l -> damage.broken(l.piece())).count();
        final int crates = damage.broken(Course.Kind.CRATE);
        final String score = (targets == 0 ? "" : String.format(" · targets down %d of %d", down, targets))
                + String.format("%s",
                crates == 0 ? "" : crates == 1 ? " · 1 crate broken" : " · " + crates + " crates broken")
                + (damage.broken(Course.Kind.FRUIT) == 0 ? ""
                        : String.format(" · fruit smashed %d of %d", damage.broken(Course.Kind.FRUIT),
                                damage.count(Course.Kind.FRUIT)))
                + (people.stream().noneMatch(Person::down) ? ""
                        : String.format(" · people down %d of %d", people.stream().filter(Person::down).count(),
                                people.size()));
        final String run = replay != null && step <= replay.length() ? (astray == 0 ? " · replaying" : " · replay astray")
                : recorder != null ? " · keeping inputs" : "";
        // a platform's controls: taken, or within reach
        final Play here = new Play(avatar, damage, sky, lifts, people, props, turrets);
        final String reach = here.withinReach().map(lift -> lift.craft().closed() ? " · T: board the airship"
                        : lift.craft() instanceof Course.Chassis chassis
                                ? (chassis.twoWheels() ? " · T: get on" : " · T: get in") : " · T: take the controls")
                .orElse(here.gunWithinReach().isPresent() ? (here.gunWithinReach().get().mount().seat().equals(here.gunWithinReach().get().mount().station()) ? " · T: man the gun" : " · T: get in")
                        : "");
        final String free = look.captured() ? "" : " · mouse free: Esc to turn with it";
        // at a vehicle's controls, what there is to say is the vehicle's, and its keys are the help
        final Optional<Lift> flown = here.atControls();
        final Optional<Turret> manned = here.manning();
        final String text = manned.map(gun -> gunStatus(gun) + score + " · " + sound + run + free)
                .or(() -> flown.map(lift -> vehicleStatus(here, lift) + (lift.craft().closed() ? "" : weapon)
                        + score + " · " + sound + run + free))
                .orElse(String.format("%s · %.1f m/s · %.1f m up · %s%s%s%s · %s%s%s", what, metres,
                        body.height() / 100 + 0.0, avatar.wearingPack() ? "jet-pack on" : "no jet-pack: J to put it on",
                        weapon, reach, score, sound, run, free));
        if (!text.equals(shown)) {
            shown = text;
            status.setText(text);
        }
        // the gun on the vehicle at whose controls the avatar is, if it has one, under control or not
        final Optional<Turret> vehicleGun = flown.flatMap(lift -> turrets.stream()
                .filter(t -> t.host() == lift.piece() && !t.mount().fixed()).findFirst());
        final boolean gunControl = vehicleGun.filter(Turret::held).isPresent();
        final String keys = flown.isEmpty() ? (manned.isPresent() ? GUN_HELP : ON_FOOT_HELP)
                : gunControl ? flown.map(lift -> lift.craft() instanceof Course.Chassis ? PICKUP_HELP
                        : lift.craft().closed() ? ARMED_AIRSHIP_HELP : GUNSHIP_HELP).orElseThrow()
                : flown.map(lift -> lift.craft() instanceof Course.Chassis chassis
                        ? (chassis.twoWheels() ? RIDE_HELP : fixedGun(lift).isPresent() ? ARMED_DRIVE_HELP : DRIVE_HELP)
                        : lift.craft().closed() ? AIRSHIP_HELP : PLATFORM_HELP).orElseThrow()
                        + (vehicleGun.isPresent() ? GUN_OFF_HELP : "");
        if (!keys.equals(help.getText())) {
            help.setText(keys);
        }
        showPanel(manned, flown, vehicleGun.isPresent(), gunControl);
    }

    /**
     * The control panel, at a vehicle's controls or a gun's: a button for each thing that can be
     * done there, with its key. A vehicle with a gun on it ({@code armed}) has its gun to take
     * control of or give up ({@code gunControl} whether it is under control); an airship, its bombs.
     */
    private void showPanel(final Optional<Turret> manned, final Optional<Lift> flown, final boolean armed,
                           final boolean gunControl) {
        if (manned.isEmpty() && flown.isEmpty()) {
            panel.hide();
            return;
        }
        final List<VehiclePanel.Control> controls = new ArrayList<>();
        if (armed) {
            controls.add(VehiclePanel.Control.GUN);
        }
        if (flown.filter(lift -> lift.craft().bombs() > 0).isPresent()) {
            controls.add(VehiclePanel.Control.BOMB);
        }
        controls.addAll(List.of(VehiclePanel.Control.FIRST_PERSON, VehiclePanel.Control.BEHIND,
                VehiclePanel.Control.LEAVE));
        final String title = flown.map(this::vehicleName).orElse("The heavy Gatling gun");
        panel.show(title, controls, gunControl, firstPerson, !look.captured());
    }

    /** What vehicle {@code lift} is, in a word or two, for the control panel's heading. */
    private String vehicleName(final Lift lift) {
        if (lift.craft() instanceof Course.Chassis chassis) {
            final Course.Bodywork body = damage.piece(lift.piece()).body();
            final String name = body != null ? body.name() : chassis.name();
            return "The " + name;
        }
        final boolean armed = turrets.stream().anyMatch(t -> t.host() == lift.piece() && !t.mount().fixed());
        return lift.craft().closed() ? (armed ? "The airship and its giant gun" : "The airship")
                : armed ? "The gunship" : "The platform";
    }

    /** A control panel button pressed: just as its key would do. */
    private void panelPressed(final VehiclePanel.Control control) {
        switch (control) {
            // as G and T pressed, at the next step
            case GUN -> pressed.add(KeyCode.G.name());
            case BOMB -> bombOnce = true;
            case FIRST_PERSON -> view(KeyCode.V);
            case BEHIND -> view(KeyCode.C);
            case LEAVE -> pressed.add(KeyCode.T.name());
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

    /**
     * The menu bar: the demos, each by name, this one ticked, to switch to (this one again starts it
     * over), and the choice of all of them (as Tab); the view, first person and the camera put back
     * behind (as V and C); and the sound, muted and its profile (as M and P). The mouse is free
     * while a menu is open, to point with, and goes back to as it was after.
     */
    private void menus() {
        final Menu demos = new Menu("Demo");
        final ToggleGroup which = new ToggleGroup();
        final Demo[] all = Demo.values();
        for (int i = 0; i < all.length; i++) {
            final Demo other = all[i];
            final RadioMenuItem item = new RadioMenuItem((i + 1) + "   " + other.title());
            item.setToggleGroup(which);
            item.setSelected(other == demo);
            item.setOnAction(e -> switchTo.accept(other));
            demos.getItems().add(item);
        }
        final MenuItem again = new MenuItem("Start " + demo.title() + " again");
        again.setOnAction(e -> switchTo.accept(demo));
        final MenuItem choice = new MenuItem("All demos…   (Tab)");
        choice.setOnAction(e -> back.run());
        demos.getItems().addAll(new SeparatorMenuItem(), again, choice);
        final Menu views = new Menu("View");
        views.getItems().addAll(
                item("First person, at the controls   (V)", KeyCode.V),
                item("Camera behind   (C)", KeyCode.C));
        final Menu sounds = new Menu("Sound");
        sounds.getItems().addAll(
                item("Mute or unmute   (M)", KeyCode.M),
                item("Next sound profile   (P)", KeyCode.P));
        for (final Menu menu : List.of(demos, views, sounds)) {
            menu.setOnShowing(e -> opening());
            menu.setOnHidden(e -> look.holdFree(false));
        }
        menuBar.getMenus().addAll(demos, views, sounds);
        menuBar.setFocusTraversable(false);
    }

    /** A menu item that does what key {@code key} does. */
    private MenuItem item(final String text, final KeyCode key) {
        final MenuItem item = new MenuItem(text);
        item.setOnAction(e -> view(key));
        return item;
    }

    /** A menu opening: the mouse freed to use it, and no trigger or key left held down for the avatar meanwhile. */
    private void opening() {
        clicking = false;
        bombing = false;
        held.clear();
        look.holdFree(true);
    }

    /** What a key does outside the world: the camera, the mouse and the sound. */
    private void view(final KeyCode code) {
        switch (code) {
            case C -> {
                chase = Chase.behind(chase.pivot(), avatar.body().yaw()).zoomed(chase.distance() / Chase.DEFAULT_DISTANCE);
                headYaw = 0;
            }
            case V -> firstPerson = !firstPerson;
            case ESCAPE -> look.toggle();
            case TAB -> back.run();
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

    /**
     * {@code avatar} as drawn in the seat of {@code chassis}, which is drawn at {@code block}: there,
     * facing the way it heads, still; on a two-wheeler leaning {@code lean} degrees, moved with it
     * as it tips about its tyres ({@link Gait#leaning}). Only how it is drawn: where it is stays the
     * world's own.
     */
    private static Avatar seated(final Avatar avatar, final Course.Chassis chassis, final Block block,
                                 final double lean) {
        final double[] tipped = Gait.leaning(lean, chassis.ride());
        // off the middle, as a car's driver is, it rises or drops as well, rolled to one side
        final double off = chassis.driverAcross() * Math.sin(Math.toRadians(lean));
        final Vec3 seat = new Vec3(block.x(), -(block.bottom() + chassis.ride() - tipped[1] - off), block.z())
                .add(Num.right(block.yaw()).multiply(chassis.driverAcross() + tipped[0]))
                .add(Num.heading(block.yaw()).multiply(chassis.driverAlong()));
        final Body body = avatar.body();
        return new Avatar(new Body(seat, block.yaw(), block.yaw(), 0, 0, 0, 0, false, true, body.phase(), body.time()),
                avatar.wearingPack(), avatar.sweep(), avatar.arms());
    }

    /**
     * The engine of land vehicle {@code lift}, as it is heard {@code seconds} after the last frame:
     * a V8 in the pickup and the truck, a V6 in the smaller ones; through its gearbox's gears as it
     * gathers speed, the revs dropping at each change and the throttle cut as it changes, down
     * them as it slows; worked as hard as it is speeding up.
     */
    private Cues.Motor motor(final Lift lift, final double seconds) {
        final Course.Chassis chassis = (Course.Chassis) lift.craft();
        final Engine engine = engine(chassis);
        final double speeding = seconds > 0 ? (Math.abs(lift.ahead()) - Math.abs(lastAhead)) / seconds : 0;
        lastAhead = lift.ahead();
        engineLoad = Num.ease(engineLoad, Num.clamp(speeding / chassis.drive().accel(), 0, 1), 6, seconds);
        // the throttle: W going ahead, or S once it is going back; off it, it is held back, and pops
        final double forward = lastControls.forward();
        final boolean back = lift.ahead() < -1;
        final double throttle = Math.min(1, forward > 0 ? forward : lift.ahead() < 1 && forward < 0 ? -forward : 0);
        // through the gears: a new vehicle taken, it starts in first, idling
        final Gearbox gearbox = gearbox(chassis);
        if (geared == null || gearedPiece != lift.piece()) {
            geared = gearbox.stopped();
            gearedPiece = lift.piece();
        }
        // its speed, in metres a second: the world is in centimetres
        geared = gearbox.step(geared, lift.ahead() / 100, throttle, back, seconds);
        final Vec3 bonnet = lift.at(new Course.Section(0, chassis.length() / 2 - 70, 20, 0, 0, 0));
        return new Cues.Motor(engine, geared.rpm(), geared.firing(), engineLoad, bonnet);
    }

    /**
     * The engine in {@code chassis}: its vehicle's, as the engine bench has it ({@link Vehicle#of}):
     * a V8 in the pickup and the truck, a V6 in the car; a 125 cc single in the scooter, a 90°
     * L-twin in the sport bike, a 45° V-twin in the cruiser.
     */
    private static Engine engine(final Course.Chassis chassis) {
        return Vehicle.of(chassis).engine();
    }

    /**
     * The gearbox in {@code chassis}: its vehicle's, as the engine bench has it, its gears, final
     * drive and wheels the same, so at any speed the same gear and revs: six gears in the car and
     * the pickup, eight in the truck; the scooter's CVT; the sport bike's six, the cruiser's five.
     */
    private static Gearbox gearbox(final Course.Chassis chassis) {
        return new Gearbox(Vehicle.of(chassis));
    }

    /**
     * The instruments of land vehicle {@code lift} (null for none driven): its engine's turns a
     * minute, as its gearbox has them ({@link #motor}), from its idle up to its red line, a V8's
     * lower than a V6's; its gear, R going back; and its speed in kilometres an hour.
     */
    private void instruments(final Lift lift, final double seconds) {
        if (lift == null || geared == null) {
            dashboard.hide();
            geared = null;
            return;
        }
        final Engine engine = engine((Course.Chassis) lift.craft());
        dashboard.show(geared.rpm(), engine.redline(), geared.shown(), Math.abs(lift.ahead()) / 100 * 3.6, seconds);
    }

    /** A view: from {@code at}, turned {@code yaw} round and {@code pitch} up. */
    private record View(Vec3 at, double yaw, double pitch) {
    }

    /**
     * The first-person view at the controls of {@code lift}, or behind {@code gun}, as they are drawn
     * ({@code shownAt}): from where its driver's eyes are, looking the way it heads (and the head
     * turned, in a land vehicle), up or down as the view is tilted, and tipped as it is; behind a
     * gun, looking where it is aimed. Null when there are none.
     */
    private View cockpit(final Lift lift, final Turret gun, final Map<Integer, Block> shownAt) {
        final double look = chase.pitch() - restPitch();
        if (gun != null) {
            final Block was = movedNow.get(gun.host());
            final Block drawn = shownAt.get(gun.host());
            final Vec3 lag = was == null || drawn == null ? Vec3.ZERO
                    : new Vec3(drawn.x() - was.x(), -(drawn.bottom() - was.bottom()), drawn.z() - was.z());
            final double yaw = Num.wrap(gun.base().yaw() + gun.wantYaw());
            final Vec3 eye = gun.pivot().add(lag).add(0, -SIGHT_UP, 0).add(Num.heading(yaw).multiply(-SIGHT_BACK));
            return new View(eye, yaw, look);
        }
        if (lift == null) {
            return null;
        }
        final Block block = shownAt.get(lift.piece());
        if (block == null) {
            return null;
        }
        final Course.Shape shape = damage.piece(lift.piece()).shape();
        final Place drawn = new Place(block.x(), block.z(), (block.bottom() + block.top()) / 2 - shape.box().up(),
                block.yaw());
        return switch (lift.craft()) {
            case Course.Chassis chassis -> new View(spot(drawn, chassis.driverAcross(), chassis.driverAlong(),
                    Chase.EYE_HEIGHT - Gait.SEATED_LOWER
                            + Math.tan(Math.toRadians(lift.tilt())) * chassis.driverAlong()),
                    Num.wrap(block.yaw() + headYaw), look + lift.tilt());
            case Course.Deck deck -> new View(spot(drawn, 0, deck.pilot(), Chase.EYE_HEIGHT), block.yaw(), look);
            case Course.Ship ship -> new View(spot(drawn, 0, Course.GONDOLA.along() + Course.GONDOLA.halfAlong() + 12,
                    Course.GONDOLA.up()), block.yaw(), look);
        };
    }

    /** The point {@code across}, {@code along} and {@code up} in the frame of {@code place}. */
    private static Vec3 spot(final Place place, final double across, final double along, final double up) {
        return new Vec3(place.x(), -(place.height() + up), place.z()).add(Num.right(place.yaw()).multiply(across))
                .add(Num.heading(place.yaw()).multiply(along));
    }

    /** How much higher the camera looks from over a vehicle wearing a body, to see over its roof: a van's is tall. */
    private double overBody(final Lift lift) {
        return lift.craft() instanceof Course.Chassis && damage.piece(lift.piece()).body() != null ? BODY_OVER : 0;
    }

    /** The gun fixed to {@code lift}, if it has one: fired by whoever drives it. */
    private Optional<Turret> fixedGun(final Lift lift) {
        return lift == null ? Optional.empty()
                : turrets.stream().filter(t -> t.host() == lift.piece() && t.mount().fixed()).findFirst();
    }

    /** Where gun {@code gun}'s line first meets {@code world} (not what carries it), just short of it; or as far as it is marked. */
    private static Vec3 gunSight(final Turret gun, final Obstacles world) {
        final Vec3 line = gun.line();
        final Hit hit = world.without(gun.host()).ray(gun.muzzle(), line, GUN_SIGHT);
        return hit == null ? gun.muzzle().add(line.multiply(GUN_SIGHT)) : hit.at().subtract(line.multiply(2));
    }

    /**
     * How far the camera looks down when the aim is level: as it always does, behind the avatar or
     * a vehicle (whose gun is aimed from there too), or, manning a gun of its own, next to level.
     * The aim is how far the view is tilted up from this.
     */
    private double restPitch() {
        return turrets.stream().filter(Turret::held).filter(gun -> !onVehicle(gun)).findFirst()
                .map(gun -> gun.mount().closed() ? GUN_REST : OPEN_GUN_REST).orElse(Chase.DEFAULT_PITCH);
    }

    /** Whether {@code gun} is on a vehicle: aimed from its controls, the view the vehicle's. */
    private boolean onVehicle(final Turret gun) {
        return lifts.stream().anyMatch(lift -> lift.piece() == gun.host());
    }

    /** How far back the camera is from vehicle {@code lift}: enough to see all of it, and some way round it. */
    private static double viewFrom(final Lift lift) {
        final Course.Section box = lift.craft().room();
        return Num.clamp(2.2 * Math.max(box.halfAlong(), box.halfAcross()) + 200, Chase.DEFAULT_DISTANCE,
                Chase.MAX_DISTANCE);
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
