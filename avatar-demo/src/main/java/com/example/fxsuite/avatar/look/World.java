package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Fruit;
import com.example.fxsuite.avatar.sim.Lift;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;

import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Translate;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The world drawn: a ground with a metre grid, and each piece of the {@link Course}, coloured by
 * what it is. Pillars, crates and the gates' bars are plain boxes. A dummy is a wooden target
 * board, a head on its shoulders and a bullseye painted on both faces. A drone is a dark body
 * with four rotors spinning at the corners and a light blinking under it. Each fills the box
 * that blocks the avatar, so what you see is what stops you.
 *
 * <p>And what the weapons have done to it ({@link Damage}), each frame: what is worn down darkens
 * and cracks, a little more as it nears breaking, with a bar over it for a few seconds after
 * each blow showing how much it has left. What is broken is gone; {@link Wreckage} throws its
 * pieces about.
 *
 * <p>JavaFX 3D has no shadows, so the avatar gets a blob: a dark disc on whatever is under it,
 * the ground or a block's top, that shrinks as it climbs. It is enough to tell how high
 * something is, and where it will land.
 */
public final class World {

    public static final Color SKY = Color.web("#9fb8c9");
    private static final double GROUND = 18000;
    private static final double GRID = 500;
    /** The disc lies this far above whatever it is on. */
    private static final double SHADOW_LIFT = 1.8;
    /** Worn right down, a piece is this much of the way to scorched. */
    private static final double CHAR = 0.55;
    private static final Color SCORCHED = Color.web("#2a2118");
    /** Cracks open as a piece wears down past each of these. */
    private static final double[] CRACKS = {0.7, 0.4, 0.15};
    /** How long a bar stays up after a blow, in seconds; and how many can be up at once. */
    private static final double BAR_TIME = 2.5;
    private static final int BARS = 8;
    private static final double BAR_WIDTH = 80;
    /** How fast the drones' rotors turn, degrees a second. */
    private static final double ROTORS = 2200;

    private final Group root = new Group();
    private final Cylinder shadow = new Cylinder(46, 1, 24);
    private final List<Course.Piece> pieces;
    /** Each piece: all of it, the part that darkens, its material as built, and how much it had left when last drawn. */
    private final Group[] parts;
    /** Each piece's turn about the vertical, for what moves. */
    private final Rotate[] turns;
    private final Shape3D[] bodies;
    private final Color[] colours;
    private final double[] shown;
    private final int[] cracked;
    private final double[] struckAt;
    private final List<Rotate> rotors = new ArrayList<>();
    private final List<Node> lights = new ArrayList<>();
    private final List<Node> fires = new ArrayList<>();
    private final PhongMaterial flame = BoxRig.glow("#ff7a1a");
    private final Bar[] bars = new Bar[BARS];
    private final PhongMaterial crack = BoxRig.matte("#1d1915");
    private double time;
    /** Each platform's lamp, lever and thrusters, by piece: to show its controls. */
    private final Map<Integer, Rig> rigs = new HashMap<>();
    /**
     * Where each piece that moves was, step by step, the last {@link #HISTORY} of them: so a mark
     * made in a step goes where the piece was in that step ({@link #at}).
     */
    private final TreeMap<Long, Map<Integer, Block>> history = new TreeMap<>();
    private static final int HISTORY = 240;

    public World(final Course course) {
        final Box ground = new Box(GROUND * 2, 2, GROUND * 2);
        ground.setTranslateY(1);
        ground.setMaterial(BoxRig.matte("#66775c"));
        root.getChildren().add(ground);

        final PhongMaterial line = BoxRig.matte("#56664d");
        for (double at = -GROUND; at <= GROUND; at += GRID) {
            root.getChildren().add(flat(new Box(4, 1, GROUND * 2), at, 0, line));
            root.getChildren().add(flat(new Box(GROUND * 2, 1, 4), 0, at, line));
        }

        pieces = course.pieces();
        parts = new Group[pieces.size()];
        turns = new Rotate[pieces.size()];
        bodies = new Shape3D[pieces.size()];
        colours = new Color[pieces.size()];
        shown = new double[pieces.size()];
        cracked = new int[pieces.size()];
        struckAt = new double[pieces.size()];
        final String[] stone = {"#8d8577", "#a29a8b", "#7b8590", "#9a8f9e"};
        final String[] crate = {"#b38b5d", "#a07a4e", "#c79f6b"};
        final String[] lit = {"#ffd166", "#7df9ff", "#ff8fab"};
        final String[] board = {"#d9c39a", "#c2a878"};
        final String[] drone = {"#3a3f47", "#4b4f3a", "#4a3c3c"};
        final String[] hull = {"#6c757e", "#6e6c58"};
        final String[] asphalt = {"#4f555b", "#62676c"};
        for (int i = 0; i < pieces.size(); i++) {
            final Course.Piece piece = pieces.get(i);
            final Block block = piece.block();
            final Group part = new Group();
            part.setTranslateX(block.x());
            part.setTranslateY(-(block.top() + block.bottom()) / 2);
            part.setTranslateZ(block.z());
            turns[i] = new Rotate(block.yaw(), Rotate.Y_AXIS);
            part.getTransforms().add(turns[i]);
            bodies[i] = switch (piece.kind()) {
                case PILLAR -> plain(part, block, BoxRig.matte(stone[piece.tint() % stone.length]));
                case CRATE -> plain(part, block, BoxRig.matte(crate[piece.tint() % crate.length]));
                case GATE -> plain(part, block, BoxRig.glow(lit[piece.tint() % lit.length]));
                case DUMMY -> dummy(part, block, BoxRig.matte(board[piece.tint() % board.length]));
                case DRONE -> drone(part, block, BoxRig.matte(drone[piece.tint() % drone.length]));
                case AIRSHIP -> airship(part, BoxRig.matte(hull[piece.tint() % hull.length]));
                case PLATFORM -> platform(i, part);
                case FURNITURE -> switch (piece.furniture()) {
                    case TABLE -> table(part, block);
                    case STOOL -> stool(part, block);
                    case SCOOTER -> scooter(part, block);
                };
                case FRUIT -> fruit(part, piece.fruit());
                case ROAD -> road(part, block, BoxRig.matte(asphalt[piece.tint() % asphalt.length]));
                case BUNKER -> bunker(part);
            };
            colours[i] = ((PhongMaterial) bodies[i].getMaterial()).getDiffuseColor();
            parts[i] = part;
            shown[i] = 1;
            struckAt[i] = Double.NEGATIVE_INFINITY;
            root.getChildren().add(part);
        }

        for (int i = 0; i < BARS; i++) {
            bars[i] = new Bar();
            root.getChildren().add(bars[i].node);
        }

        shadow.setMaterial(BoxRig.matte("#45533e"));
        root.getChildren().add(shadow);

        final AmbientLight ambient = new AmbientLight(Color.web("#8b939c"));
        final DirectionalLight sun = new DirectionalLight(Color.web("#a8a49a"));
        sun.setDirection(new Point3D(0.35, 1, 0.45));
        root.getChildren().addAll(ambient, sun);
    }

    public Node node() {
        return root;
    }

    /**
     * Draws what the weapons have done, and moves what moves to where it is seen: each of
     * {@code placed}, by piece, where it is drawn this frame ({@link com.example.fxsuite.avatar.sim.Play#moving}, between its
     * last two steps). Then the platforms' controls ({@code lifts}), the rotors and the lights.
     * {@code viewYaw} turns the bars to face the camera.
     */
    public void show(final Damage damage, final Map<Integer, Block> placed, final List<Lift> lifts,
                     final double seconds, final double viewYaw) {
        time += seconds;
        for (final Map.Entry<Integer, Block> moved : placed.entrySet()) {
            final int i = moved.getKey();
            final Block block = moved.getValue();
            parts[i].setTranslateX(block.x());
            parts[i].setTranslateY(-(block.top() + block.bottom()) / 2);
            parts[i].setTranslateZ(block.z());
            turns[i].setAngle(block.yaw());
        }
        for (final Lift lift : lifts) {
            final Rig rig = rigs.get(lift.piece());
            if (rig != null) {
                rig.show(lift, time);
            }
        }
        for (final Rotate rotor : rotors) {
            rotor.setAngle((rotor.getAngle() + ROTORS * seconds) % 360);
        }
        final boolean on = time % 1.2 < 0.15;
        for (final Node light : lights) {
            light.setVisible(on);
        }
        for (int k = 0; k < fires.size(); k++) {
            // flickering, each out of step with the rest
            final double flicker = 0.8 + 0.25 * Math.sin(time * 17 + k * 2.1) + 0.15 * Math.sin(time * 41 + k);
            fires.get(k).setScaleX(flicker);
            fires.get(k).setScaleY(0.7 + 0.6 * flicker * flicker);
            fires.get(k).setScaleZ(flicker);
        }
        for (int i = 0; i < parts.length; i++) {
            final double left = damage.left(i);
            if (left == shown[i]) {
                continue;
            }
            shown[i] = left;
            if (damage.broken(i)) {
                parts[i].setVisible(false);
                struckAt[i] = Double.NEGATIVE_INFINITY;
                continue;
            }
            struckAt[i] = time;
            wear(i, left);
        }
        // the bars over the most lately struck, while their time lasts
        final List<Integer> showing = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            if (time - struckAt[i] < BAR_TIME) {
                showing.add(i);
            }
        }
        showing.sort((a, b) -> Double.compare(struckAt[b], struckAt[a]));
        for (int k = 0; k < BARS; k++) {
            if (k < showing.size()) {
                final int i = showing.get(k);
                bars[k].show(placed.getOrDefault(i, pieces.get(i).block()), shown[i], viewYaw);
            } else {
                bars[k].node.setVisible(false);
            }
        }
    }

    /**
     * Piece {@code piece}'s own group: placed at its middle and turned with it, and moved with it
     * if it moves. What is put in it is part of the piece: it goes where the piece goes, and is
     * gone when the piece is broken.
     */
    Group group(final int piece) {
        return parts[piece];
    }

    /** What piece {@code piece} is. */
    Course.Kind kind(final int piece) {
        return pieces.get(piece).kind();
    }

    /** Which fruit piece {@code piece} is, if it is fruit; null if not. */
    Fruit fruit(final int piece) {
        return pieces.get(piece).fruit();
    }

    /** Where each piece that moves was after step {@code step}, {@code placed}: kept a while, for {@link #at}. */
    public void moved(final long step, final Map<Integer, Block> placed) {
        history.put(step, placed);
        while (history.size() > HISTORY) {
            history.pollFirstEntry();
        }
    }

    /** Piece {@code piece}'s box after step {@code step}: where it was then, if it moves and that is still known; where it was built, otherwise. */
    Block at(final int piece, final long step) {
        final Map.Entry<Long, Map<Integer, Block>> then = history.floorEntry(step);
        if (then != null && then.getValue().containsKey(piece)) {
            return then.getValue().get(piece);
        }
        return pieces.get(piece).block();
    }

    /** What piece {@code piece} is made of, as it now looks: for its broken pieces. */
    public PhongMaterial material(final int piece) {
        return (PhongMaterial) bodies[piece].getMaterial();
    }

    /** Shows the blob shadow, or hides it: when the avatar is out of sight, inside a vehicle. */
    public void shadowShown(final boolean shown) {
        shadow.setVisible(shown);
    }

    /** Moves the blob shadow onto whatever is under the body, smaller the higher it is above it. */
    public void shadowUnder(final Body body, final Obstacles obstacles) {
        final double x = body.position().x();
        final double z = body.position().z();
        final double floor = obstacles.floor(x, z, Body.FOOTING, body.height() + 1e-6);
        shadow.setTranslateX(x);
        shadow.setTranslateY(-floor - SHADOW_LIFT);
        shadow.setTranslateZ(z);
        final double scale = Math.max(0.25, 1 / (1 + (body.height() - floor) / 500));
        shadow.setScaleX(scale);
        shadow.setScaleZ(scale);
    }

    /** Darkened as far as it is worn (each piece has a material of its own), and cracked at each step down. */
    private void wear(final int piece, final double left) {
        material(piece).setDiffuseColor(colours[piece].interpolate(SCORCHED, (1 - left) * CHAR));
        final Course.Kind kind = pieces.get(piece).kind();
        if (kind == Course.Kind.DRONE || kind == Course.Kind.FRUIT) {
            return;  // a drone's box is mostly air, and fruit is round: nothing to crack
        }
        while (cracked[piece] < CRACKS.length && left < CRACKS[cracked[piece]]) {
            cracked[piece]++;
            for (int k = 0; k < 2; k++) {
                if (kind == Course.Kind.AIRSHIP) {
                    fire(piece, cracked[piece] * 2 + k);
                } else {
                    crack(piece, cracked[piece] * 2 + k);
                }
            }
        }
    }

    /** A crack across one of the faces of piece {@code piece}, the {@code n}th: where, and which way, by number. */
    private void crack(final int piece, final int n) {
        final Block block = pieces.get(piece).block();
        final double w = block.halfWidth();
        final double d = block.halfDepth();
        final double h = (block.top() - block.bottom()) / 2;
        final double u = unit(piece, n, 1) - 0.5;
        final double v = unit(piece, n, 2) - 0.5;
        final double turn = unit(piece, n, 3) * 180;
        final int face = (int) (unit(piece, n, 4) * 5);
        final double across;
        final Box line;
        switch (face) {
            case 0, 1 -> {
                // the front or the back
                across = Math.min(w, h) * (0.6 + 0.6 * unit(piece, n, 5));
                line = new Box(across, 2.5, 0.6);
                line.setTranslateX(u * w);
                line.setTranslateY(v * h);
                line.setTranslateZ(face == 0 ? d + 0.3 : -d - 0.3);
                line.getTransforms().add(new Rotate(turn, Rotate.Z_AXIS));
            }
            case 2, 3 -> {
                // the left or right side
                across = Math.min(d, h) * (0.6 + 0.6 * unit(piece, n, 5));
                line = new Box(0.6, 2.5, across);
                line.setTranslateX(face == 2 ? w + 0.3 : -w - 0.3);
                line.setTranslateY(v * h);
                line.setTranslateZ(u * d);
                line.getTransforms().add(new Rotate(turn, Rotate.X_AXIS));
            }
            default -> {
                // the top
                across = Math.min(w, d) * (0.6 + 0.6 * unit(piece, n, 5));
                line = new Box(across, 0.6, 2.5);
                line.setTranslateX(u * w);
                line.setTranslateY(-h - 0.3);
                line.setTranslateZ(v * d);
                line.getTransforms().add(new Rotate(turn, Rotate.Y_AXIS));
            }
        }
        line.setMaterial(crack);
        parts[piece].getChildren().add(line);
    }

    /**
     * A fire broken out on an airship's hull, the {@code n}th: on top or down a side of its
     * middle, where by number. The airship's frame has the hull's line at its middle, and its
     * box's middle {@link Course#AIRSHIP_BOX} above that.
     */
    private void fire(final int piece, final int n) {
        final Course.Section middle = Course.HULL.get(3);
        final double along = (unit(piece, n, 1) - 0.5) * 1.6 * middle.halfAlong();
        final double round = (unit(piece, n, 2) - 0.5) * Math.PI * 1.2;
        // round the hull's square middle, from one side over the top to the other
        final double across = Math.sin(round) * middle.halfAcross();
        final double up = Math.cos(round) * middle.halfUp();
        final Box burning = new Box(46, 54, 70);
        burning.setMaterial(flame);
        burning.setTranslateX(Math.max(-middle.halfAcross(), Math.min(middle.halfAcross(), across * 1.3)));
        burning.setTranslateY(-(Math.min(middle.halfUp(), up * 1.3) - Course.AIRSHIP_BOX.up()) - 20);
        burning.setTranslateZ(along);
        fires.add(burning);
        parts[piece].getChildren().add(burning);
    }

    /** 0 to 1, the same for the same piece, crack and salt. */
    private static double unit(final int piece, final int n, final int salt) {
        return Num.hash(piece, n, salt);
    }

    /** A bunker: its concrete walls and roof, the slit along its front and the door at its back left open. */
    private static Shape3D bunker(final Group part) {
        final PhongMaterial concrete = BoxRig.matte("#8d8a80");
        final PhongMaterial roof = BoxRig.matte("#77746b");
        Shape3D front = null;
        for (final Course.Section wall : Course.BUNKER_WALLS) {
            final Box box = section(part, Course.BUNKER_BOX, wall, wall == Course.BUNKER_WALLS.getLast() ? roof : concrete);
            if (front == null) {
                front = box;
            }
        }
        return front;
    }

    /**
     * A stretch of road: a ramp as a wedge, its top sloping as the block's does, or a deck as a
     * box; a yellow line down the middle of it and a white one along each edge, lying on its top.
     */
    private static Shape3D road(final Group part, final Block block, final PhongMaterial material) {
        final Shape3D body = block.sloped() ? wedge(block) : new Box(block.halfWidth() * 2,
                block.top() - block.bottom(), block.halfDepth() * 2);
        body.setMaterial(material);
        part.getChildren().add(body);
        // the road runs along the ramp, or along the deck's longer side
        final boolean along = block.sloped() || block.halfDepth() >= block.halfWidth();
        final double length = along ? Math.hypot(block.halfDepth() * 2, block.top() - block.toe())
                : block.halfWidth() * 2;
        final double half = along ? block.halfWidth() : block.halfDepth();
        final double mid = (block.top() + block.bottom()) / 2;
        final double slope = Math.toDegrees(Math.atan2(block.top() - block.toe(), block.halfDepth() * 2));
        final PhongMaterial yellow = BoxRig.matte("#f2c230");
        final PhongMaterial white = BoxRig.matte("#e8e8e2");
        for (final double across : new double[] {0, -(half - 25), half - 25}) {
            final Box line = along ? new Box(across == 0 ? 14 : 10, 2, length) : new Box(length, 2, across == 0 ? 14 : 10);
            line.setMaterial(across == 0 ? yellow : white);
            if (along) {
                line.setTranslateX(across);
                line.getTransforms().add(new Rotate(slope, Rotate.X_AXIS));
            } else {
                line.setTranslateZ(across);
            }
            line.setTranslateY(mid - (block.top() + block.toe()) / 2 - 1.5);
            part.getChildren().add(line);
        }
        return body;
    }

    /** A ramp's solid: its footprint at the bottom, its top sloping from its toe at the back up to its top at the front. */
    private static Shape3D wedge(final Block block) {
        final float w = (float) block.halfWidth();
        final float d = (float) block.halfDepth();
        final double mid = (block.top() + block.bottom()) / 2;
        // heights up, as the part's own y (down), from its middle
        final float under = (float) (mid - block.bottom());
        final float back = (float) (mid - block.toe());
        final float front = (float) (mid - block.top());
        final TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
                -w, under, -d, w, under, -d, w, under, d, -w, under, d,
                -w, back, -d, w, back, -d, w, front, d, -w, front, d);
        mesh.getTexCoords().addAll(0, 0);
        final int[][] quads = {{0, 1, 2, 3}, {4, 7, 6, 5}, {0, 4, 5, 1}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 5, 6, 2}};
        for (int f = 0; f < quads.length; f++) {
            final int[] q = quads[f];
            // wound so that each face looks outward, and is lit from outside
            mesh.getFaces().addAll(q[0], 0, q[2], 0, q[1], 0, q[0], 0, q[3], 0, q[2], 0);
            mesh.getFaceSmoothingGroups().addAll(1 << f, 1 << f);
        }
        final MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        return view;
    }

    /** A block as a box filling the piece's box. */
    private static Shape3D plain(final Group part, final Block block, final PhongMaterial material) {
        final Box box = new Box(block.halfWidth() * 2, block.top() - block.bottom(), block.halfDepth() * 2);
        box.setMaterial(material);
        part.getChildren().add(box);
        return box;
    }

    /**
     * A target board: a body as wide as the box up to its shoulders, a head on top, and a
     * bullseye painted on both faces at the height the guns shoot from the hip.
     */
    private static Shape3D dummy(final Group part, final Block block, final PhongMaterial wood) {
        final double width = block.halfWidth() * 2;
        final double depth = block.halfDepth() * 2;
        final double tall = block.top() - block.bottom();
        final double shoulders = tall * 0.75;
        final Box body = new Box(width, shoulders, depth);
        body.setTranslateY(tall / 2 - shoulders / 2);
        body.setMaterial(wood);
        final Box head = new Box(width * 0.5, tall - shoulders, depth);
        head.setTranslateY(-tall / 2 + (tall - shoulders) / 2);
        head.setMaterial(wood);
        part.getChildren().addAll(body, head);
        // the rings, outermost first, each a hair proud of the last
        final PhongMaterial red = BoxRig.matte("#c0392b");
        final PhongMaterial white = BoxRig.matte("#f4efe4");
        final double[] radii = {width * 0.38, width * 0.28, width * 0.18, width * 0.08};
        final double centre = tall / 2 - 120;
        for (final double side : new double[] {1, -1}) {
            for (int k = 0; k < radii.length; k++) {
                final Cylinder ring = new Cylinder(radii[k], 0.1, 28);
                ring.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
                ring.setTranslateY(centre);
                ring.setTranslateZ(side * (depth / 2 + 0.06 * (k + 1)));
                ring.setMaterial(k % 2 == 0 ? white : red);
                part.getChildren().add(ring);
            }
        }
        return body;
    }

    /** A drone: a body in the middle, an arm out to each corner, a rotor spinning on each, a light under it. */
    private Shape3D drone(final Group part, final Block block, final PhongMaterial shell) {
        final double half = block.halfWidth();
        final double tall = block.top() - block.bottom();
        final Box body = new Box(half * 0.8, tall * 0.55, half * 0.8);
        body.setMaterial(shell);
        part.getChildren().add(body);
        final PhongMaterial metal = BoxRig.matte("#2b2e33");
        final PhongMaterial blade = BoxRig.matte("#9aa3ad");
        final double corner = half * 0.66;
        for (final double x : new double[] {-corner, corner}) {
            for (final double z : new double[] {-corner, corner}) {
                final Box arm = new Box(6, 5, Math.hypot(x, z));
                arm.setMaterial(metal);
                arm.setTranslateX(x / 2);
                arm.setTranslateZ(z / 2);
                arm.getTransforms().add(new Rotate(Math.toDegrees(Math.atan2(x, z)), Rotate.Y_AXIS));
                final Cylinder hub = new Cylinder(5, tall * 0.5, 12);
                hub.setMaterial(metal);
                hub.setTranslateX(x);
                hub.setTranslateZ(z);
                final Rotate spin = new Rotate(45 * rotors.size(), Rotate.Y_AXIS);
                final Box rotor = new Box(half * 0.62, 1.5, 6);
                rotor.setMaterial(blade);
                rotor.setTranslateX(x);
                rotor.setTranslateY(-tall / 2 + 1);
                rotor.setTranslateZ(z);
                rotor.getTransforms().add(spin);
                rotors.add(spin);
                part.getChildren().addAll(arm, hub, rotor);
            }
        }
        final Box light = new Box(8, 4, 8);
        light.setMaterial(BoxRig.glow("#ff3b30"));
        light.setTranslateY(tall * 0.3);
        lights.add(light);
        part.getChildren().add(light);
        return body;
    }

    /**
     * An airship, from {@link Course}'s own parts, so each box drawn is a box that is solid:
     * the stepped hull, banded in yellow, the fins, the gondola with its windows lit, the
     * engines on their pylons with a propeller turning at the back of each, and lights
     * blinking at the nose and on the top fin.
     */
    private Shape3D airship(final Group part, final PhongMaterial shell) {
        final PhongMaterial dark = BoxRig.matte("#3b4148");
        final PhongMaterial band = BoxRig.matte("#c9a227");
        final PhongMaterial windows = BoxRig.glow("#ffe9a8");
        final PhongMaterial blade = BoxRig.matte("#22262b");
        Shape3D middle = null;
        for (final Course.Section section : Course.HULL) {
            final Box hull = section(part, section, shell);
            if (section == Course.HULL.get(3)) {
                middle = hull;
            }
        }
        // two bands round the middle, a hair proud of it
        for (final double along : new double[] {-260, 300}) {
            final Course.Section m = Course.HULL.get(3);
            section(part, new Course.Section(0, along, 0, m.halfAcross() + 1, 22, m.halfUp() + 1), band);
        }
        for (final Course.Section fin : Course.FINS) {
            section(part, fin, dark);
        }
        final Course.Section gondola = Course.GONDOLA;
        section(part, gondola, dark);
        for (double along = gondola.along() - 150; along <= gondola.along() + 150; along += 50) {
            for (final double side : new double[] {-1, 1}) {
                section(part, new Course.Section(side * (gondola.halfAcross() + 0.5), along, gondola.up() + 12,
                        1, 14, 12), windows);
            }
        }
        for (final Course.Section pylon : Course.PYLONS) {
            section(part, pylon, dark);
        }
        for (final Course.Section engine : Course.ENGINES) {
            section(part, engine, shell);
            // the propeller: two blades crossed, turning about the engine's length
            final Group propeller = new Group();
            final Rotate spin = new Rotate(30 * rotors.size(), Rotate.Z_AXIS);
            propeller.getTransforms().add(spin);
            rotors.add(spin);
            for (final double turn : new double[] {0, 90}) {
                final Box b = new Box(170, 12, 4);
                b.setMaterial(blade);
                b.getTransforms().add(new Rotate(turn, Rotate.Z_AXIS));
                propeller.getChildren().add(b);
            }
            propeller.setTranslateX(engine.across());
            propeller.setTranslateY(-(engine.up() - Course.AIRSHIP_BOX.up()));
            propeller.setTranslateZ(engine.along() - engine.halfAlong() - 6);
            part.getChildren().add(propeller);
        }
        final PhongMaterial red = BoxRig.glow("#ff3b30");
        final Course.Section nose = Course.HULL.getFirst();
        final Course.Section fin = Course.FINS.getFirst();
        for (final Course.Section light : new Course.Section[] {
                new Course.Section(0, nose.along() + nose.halfAlong() + 4, 0, 10, 4, 10),
                new Course.Section(0, fin.along() - fin.halfAlong() + 10, fin.up() + fin.halfUp() + 6, 8, 8, 6)}) {
            lights.add(section(part, light, red));
        }
        return middle;
    }

    /**
     * A platform, from its own parts, as big as its deck was made ({@link Course.Deck}): a steel
     * deck edged in yellow and black, a glowing thruster under each corner, and at the front the
     * console, its panel tilted to whoever stands behind it, with a lever that leans the way the
     * platform goes and a lamp that shows green while someone has the controls.
     */
    private Shape3D platform(final int piece, final Group part) {
        final Course.Shape shape = pieces.get(piece).shape();
        final Course.Deck size = Course.Deck.of(shape);
        final Course.Section frame = shape.box();
        final PhongMaterial steel = BoxRig.matte("#5d656e");
        final PhongMaterial dark = BoxRig.matte("#2b3036");
        final PhongMaterial yellow = BoxRig.matte("#e0b400");
        final Course.Section d = size.slab();
        final Box deck = section(part, frame, d, steel);
        // hazard stripes round the deck's top edge
        final double top = d.up() + d.halfUp() + 0.6;
        for (int side = 0; side < 4; side++) {
            final boolean across = side < 2;
            final double length = across ? d.halfAcross() * 2 : d.halfAlong() * 2;
            final double edge = side % 2 == 0 ? 1 : -1;
            // about 40 cm each, as many as fit the edge exactly
            final int stripes = Math.max(1, (int) Math.round(length / 40));
            final double stripe = length / stripes;
            for (int k = 0; k < stripes; k++) {
                final double along = -length / 2 + (k + 0.5) * stripe;
                final Course.Section strip = across
                        ? new Course.Section(along, edge * (d.halfAlong() - 6), top, stripe / 2, 6, 0.6)
                        : new Course.Section(edge * (d.halfAcross() - 6), along, top, 6, stripe / 2, 0.6);
                section(part, frame, strip, k % 2 == 0 ? yellow : dark);
            }
        }
        // a thruster under each corner, glowing
        final List<Node> thrusters = new ArrayList<>();
        for (final double x : new double[] {-1, 1}) {
            for (final double z : new double[] {-1, 1}) {
                final Course.Section pod = new Course.Section(x * (d.halfAcross() - 50), z * (d.halfAlong() - 50),
                        d.up() - d.halfUp() - 12, 34, 34, 12);
                section(part, frame, pod, dark);
                final Box glow = section(part, frame, new Course.Section(pod.across(), pod.along(),
                        pod.up() - pod.halfUp() - 1, 26, 26, 1), BoxRig.glow("#7df9ff"));
                thrusters.add(glow);
            }
        }
        // the console, its panel tilted toward whoever stands behind it, with buttons, a lever and a lamp
        final Course.Section c = size.console();
        section(part, frame, c, dark);
        final Group panel = new Group();
        panel.setTranslateX(c.across());
        panel.setTranslateY(-(c.up() + c.halfUp() - frame.up()));
        panel.setTranslateZ(c.along());
        panel.getTransforms().addAll(new Rotate(180, Rotate.Y_AXIS), new Rotate(-35, Rotate.X_AXIS));
        final Box face = new Box(c.halfAcross() * 2 + 8, 4, c.halfAlong() * 2 + 16);
        face.setMaterial(steel);
        panel.getChildren().add(face);
        final String[] lit = {"#ff3b30", "#ffd166", "#34c759"};
        for (int k = 0; k < lit.length; k++) {
            final Box button = new Box(8, 3, 8);
            button.setMaterial(BoxRig.glow(lit[k]));
            button.setTranslateX(-22 + k * 14);
            button.setTranslateY(-3);
            button.setTranslateZ(8);
            panel.getChildren().add(button);
        }
        final Rotate lean = new Rotate(0, Rotate.X_AXIS);
        final Group lever = new Group();
        lever.setTranslateX(24);
        lever.setTranslateY(-2);
        lever.setTranslateZ(-4);
        lever.getTransforms().add(lean);
        final Box stick = new Box(3, 22, 3);
        stick.setTranslateY(-11);
        stick.setMaterial(dark);
        final Box knob = new Box(7, 7, 7);
        knob.setTranslateY(-24);
        knob.setMaterial(BoxRig.matte("#c62828"));
        lever.getChildren().addAll(stick, knob);
        panel.getChildren().add(lever);
        part.getChildren().add(panel);
        final Box lamp = section(part, frame, new Course.Section(c.across() - 28, c.along() + 10,
                c.up() + c.halfUp() + 8, 5, 5, 8), BoxRig.glow("#ffb300"));
        rigs.put(piece, new Rig(lamp, lean, thrusters));
        return deck;
    }

    /** A piece of {@code fruit}, as that fruit looks; its material its own, so it can darken as it is worn. */
    private static Shape3D fruit(final Group part, final Fruit fruit) {
        return switch (fruit) {
            case WATERMELON -> watermelon(part, fruit);
            case PINEAPPLE -> pineapple(part, fruit);
        };
    }

    /** A watermelon, lying on its side: a round, dark green body striped lighter from end to end. */
    private static Shape3D watermelon(final Group part, final Fruit fruit) {
        final Sphere body = new Sphere(1, 24);
        // the sphere's poles along its y; scaled long that way, then laid on its side, across
        body.setScaleX(fruit.height() / 2);
        body.setScaleY(fruit.length() / 2);
        body.setScaleZ(fruit.width() / 2);
        final PhongMaterial rind = new PhongMaterial(Color.WHITE);
        rind.setDiffuseMap(STRIPES);
        rind.setSpecularColor(Color.web("#3a4a3a"));
        body.setMaterial(rind);
        final Group lying = new Group(body);
        lying.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
        part.getChildren().add(lying);
        return body;
    }

    /** A pineapple, standing on end: a golden body scaled in a diamond pattern, and a crown of spiky leaves on top. */
    private static Shape3D pineapple(final Group part, final Fruit fruit) {
        final Sphere body = new Sphere(1, 16);
        body.setScaleX(fruit.length() / 2);
        body.setScaleY(fruit.height() / 2);
        body.setScaleZ(fruit.width() / 2);
        final PhongMaterial skin = new PhongMaterial(Color.WHITE);
        skin.setDiffuseMap(DIAMONDS);
        body.setMaterial(skin);
        part.getChildren().add(body);
        final PhongMaterial leaves = BoxRig.matte("#3f7d2e");
        for (int k = 0; k < 5; k++) {
            final Box leaf = new Box(3, 16, 7);
            leaf.setMaterial(leaves);
            leaf.setTranslateY(-fruit.height() / 2 - 6);
            leaf.getTransforms().addAll(new Rotate(k * 72, Rotate.Y_AXIS), new Rotate(k == 0 ? 0 : 22, Rotate.X_AXIS));
            part.getChildren().add(leaf);
        }
        return body;
    }

    /** A pineapple's skin, flat: gold, scaled in diamonds by darker lines running both ways round it. */
    private static final WritableImage DIAMONDS = diamonds();

    private static WritableImage diamonds() {
        final int size = 64;
        final WritableImage image = new WritableImage(size, size);
        final Color gold = Color.web("#d39a32");
        final Color line = Color.web("#7a5212");
        for (int v = 0; v < size; v++) {
            for (int u = 0; u < size; u++) {
                // eight diamonds round, eight high
                final boolean onLine = Math.floorMod(u + v, 8) == 0 || Math.floorMod(u - v, 8) == 0;
                image.getPixelWriter().setColor(u, v, onLine ? line : gold);
            }
        }
        return image;
    }

    /** A watermelon's skin, flat: dark green, with lighter stripes that wander a little, end to end. */
    private static final WritableImage STRIPES = stripes();

    private static WritableImage stripes() {
        final int wide = 128;
        final int high = 32;
        final WritableImage image = new WritableImage(wide, high);
        final Color dark = Color.web("#1e4a1c");
        final Color light = Color.web("#5b9a3c");
        for (int v = 0; v < high; v++) {
            final double wander = 2.5 * Math.sin(v * Math.PI * 3 / high);
            for (int u = 0; u < wide; u++) {
                // nine stripes round it, each a third light
                final double round = ((u + wander) / wide * 9) % 1;
                image.getPixelWriter().setColor(u, v, round < 0.33 ? light : dark);
            }
        }
        return image;
    }

    /**
     * The fruit stall's table ({@link Furniture#TABLE}): planks on a pair of pallets, a cloth hung over the front (its -z,
     * toward the back of the barge, where the customers come), and
     * about it what a stall has: a striped awning on two poles, a scale on the table, a sign, and
     * the table itself solid; what is about it comes with it.
     */
    private static Shape3D table(final Group part, final Block block) {
        final PhongMaterial planks = BoxRig.matte("#c9a273");
        final PhongMaterial pallet = BoxRig.matte("#9c7a4e");
        final double halfWide = block.halfWidth();
        final double halfDeep = block.halfDepth();
        final double half = (block.top() - block.bottom()) / 2;
        // the table's frame: its middle; the deck is half its height below (y is down)
        final Box top = new Box(halfWide * 2, 6, halfDeep * 2);
        top.setTranslateY(-half + 3);
        top.setMaterial(planks);
        part.getChildren().add(top);
        for (final double side : new double[] {-1, 1}) {
            final Box under = new Box(halfWide * 0.9, half * 2 - 6, halfDeep * 2 - 10);
            under.setTranslateX(side * halfWide * 0.5);
            under.setTranslateY(3);
            under.setMaterial(pallet);
            part.getChildren().add(under);
        }
        final Box cloth = new Box(halfWide * 2 - 4, half * 1.2, 1.5);
        cloth.setTranslateY(-half + 6 + half * 0.6);
        cloth.setTranslateZ(-halfDeep - 1);
        cloth.setMaterial(BoxRig.matte("#2f5d8a"));
        part.getChildren().add(cloth);
        // the awning: two poles at the back, a canopy striped red and white, sloping to the front
        final PhongMaterial pole = BoxRig.matte("#55595e");
        for (final double side : new double[] {-1, 1}) {
            final Box stick = new Box(5, 230, 5);
            stick.setTranslateX(side * (halfWide + 10));
            stick.setTranslateY(half - 115);
            stick.setTranslateZ(halfDeep + 30);
            stick.setMaterial(pole);
            part.getChildren().add(stick);
        }
        final Group canopy = new Group();
        canopy.setTranslateY(half - 230);
        canopy.setTranslateZ(halfDeep + 30 - 85);
        canopy.getTransforms().add(new Rotate(12, Rotate.X_AXIS));
        final int strips = 8;
        final double across = (halfWide + 30) * 2;
        for (int k = 0; k < strips; k++) {
            final Box strip = new Box(across / strips, 3, 190);
            strip.setTranslateX(-across / 2 + (k + 0.5) * across / strips);
            strip.setMaterial(BoxRig.matte(k % 2 == 0 ? "#d8323a" : "#f2efe6"));
            canopy.getChildren().add(strip);
        }
        part.getChildren().add(canopy);
        // a scale at one end of the table, and a sign at the front corner
        final Box scaleBase = new Box(22, 8, 18);
        scaleBase.setTranslateX(halfWide - 20);
        scaleBase.setTranslateY(-half - 4);
        scaleBase.setTranslateZ(-halfDeep + 16);
        scaleBase.setMaterial(BoxRig.matte("#b9bec4"));
        final Box scalePan = new Box(26, 2, 22);
        scalePan.setTranslateX(halfWide - 20);
        scalePan.setTranslateY(-half - 9);
        scalePan.setTranslateZ(-halfDeep + 16);
        scalePan.setMaterial(BoxRig.matte("#d6dadf"));
        part.getChildren().addAll(scaleBase, scalePan);
        final Box board = new Box(56, 34, 2);
        board.setTranslateX(-halfWide + 34);
        board.setTranslateY(-half - 44);
        board.setTranslateZ(-halfDeep - 3);
        board.setMaterial(BoxRig.matte("#f4ead2"));
        final Box post = new Box(3, 30, 3);
        post.setTranslateX(-halfWide + 34);
        post.setTranslateY(-half - 14);
        post.setTranslateZ(-halfDeep - 3);
        post.setMaterial(pallet);
        final Text sign = new Text("西瓜");
        sign.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));
        sign.setFill(Color.web("#b3261e"));
        // a Text faces -z, the front, to whoever stands there: centred on the board
        sign.getTransforms().add(new Translate(-halfWide + 34 - 22, -half - 36, -halfDeep - 4.2));
        part.getChildren().addAll(board, post, sign);
        return top;
    }

    /** A wooden stool: a round seat on a stout leg, as tall as it is ({@link Furniture#STOOL}). */
    private static Shape3D stool(final Group part, final Block block) {
        final double tall = block.top() - block.bottom();
        final Cylinder seat = new Cylinder(block.halfWidth(), 6);
        seat.setTranslateY(-tall / 2 + 3);
        seat.setMaterial(BoxRig.matte("#8a6440"));
        final Cylinder leg = new Cylinder(block.halfWidth() * 0.55, tall - 6);
        leg.setTranslateY(3);
        leg.setMaterial(BoxRig.matte("#6e4f32"));
        part.getChildren().addAll(seat, leg);
        return seat;
    }

    /**
     * A little scooter ({@link Furniture#SCOOTER}), its front along its frame: body, seat, front,
     * handlebar, wheels and a yellow plate. Solid as far as its seat; the front and the handlebar
     * stand above that.
     */
    private static Shape3D scooter(final Group part, final Block block) {
        final PhongMaterial shell = BoxRig.matte("#2c3238");
        final PhongMaterial black = BoxRig.matte("#141414");
        // built from its wheels' feet up (y is down); the frame's middle is half its height up
        final Group scooter = new Group();
        scooter.setTranslateY((block.top() - block.bottom()) / 2);
        final Box body = scooterBox(34, 22, 100, 0, -26, 0, shell);
        scooter.getChildren().add(body);
        scooter.getChildren().add(scooterBox(30, 10, 48, 0, -52, -18, black));
        scooter.getChildren().add(scooterBox(34, 70, 12, 0, -60, 46, shell));
        scooter.getChildren().add(scooterBox(62, 4, 4, 0, -98, 44, black));
        scooter.getChildren().add(scooterBox(16, 8, 1, 0, -40, -51, BoxRig.matte("#e8c21a")));
        for (final double z : new double[] {-38, 40}) {
            final Cylinder wheel = new Cylinder(17, 8);
            wheel.setMaterial(black);
            wheel.setTranslateY(-17);
            wheel.setTranslateZ(z);
            wheel.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
            scooter.getChildren().add(wheel);
        }
        part.getChildren().add(scooter);
        return body;
    }

    private static Box scooterBox(final double w, final double h, final double d, final double x, final double y,
                                  final double z, final PhongMaterial material) {
        final Box box = new Box(w, h, d);
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        box.setMaterial(material);
        return box;
    }

    /** A platform's controls as shown: the lamp, the lever, the thrusters. */
    private static final class Rig {
        private final Box lamp;
        private final Rotate lean;
        private final List<Node> thrusters;
        private final PhongMaterial idle = BoxRig.glow("#ffb300");
        private final PhongMaterial held = BoxRig.glow("#34c759");

        Rig(final Box lamp, final Rotate lean, final List<Node> thrusters) {
            this.lamp = lamp;
            this.lean = lean;
            this.thrusters = thrusters;
        }

        /** As {@code lift} is: green while held, the lever leaning the way it goes, the thrusters brighter the harder it goes. */
        void show(final Lift lift, final double time) {
            lamp.setMaterial(lift.held() ? held : idle);
            lean.setAngle(30 * Num.clamp(lift.ahead() / Lift.SPEED + lift.climb() / Lift.CLIMB, -1, 1));
            final double going = Math.min(1, lift.speed() / Lift.SPEED);
            final double throb = 0.75 + 0.25 * Math.sin(time * 9) + 0.5 * Math.abs(going);
            for (final Node thruster : thrusters) {
                thruster.setVisible(lift.engine());
                thruster.setScaleX(throb);
                thruster.setScaleZ(throb);
            }
        }
    }

    /** One of an airship's parts, as a box in its frame. */
    private static Box section(final Group part, final Course.Section section, final PhongMaterial material) {
        return section(part, Course.AIRSHIP_BOX, section, material);
    }

    /** One of the parts of something that moves, as a box in its frame, the middle of whose box is {@code frame}'s. */
    private static Box section(final Group part, final Course.Section frame, final Course.Section section,
                               final PhongMaterial material) {
        final Box box = new Box(section.halfAcross() * 2, section.halfUp() * 2, section.halfAlong() * 2);
        box.setTranslateX(section.across());
        box.setTranslateY(-(section.up() - frame.up()));
        box.setTranslateZ(section.along());
        box.setMaterial(material);
        part.getChildren().add(box);
        return box;
    }

    private static Box flat(final Box box, final double x, final double z, final PhongMaterial material) {
        box.setTranslateX(x);
        box.setTranslateY(-0.6);
        box.setTranslateZ(z);
        box.setMaterial(material);
        return box;
    }

    /** A bar over something struck: dark behind, and in front, coloured green to red, as much as it has left. */
    private static final class Bar {
        private final Group node = new Group();
        private final Rotate facing = new Rotate(0, Rotate.Y_AXIS);
        private final Box fill = new Box(BAR_WIDTH, 6, 1.2);
        private double drawn = -1;

        Bar() {
            final Box back = new Box(BAR_WIDTH + 4, 10, 1);
            back.setMaterial(BoxRig.matte("#15181c"));
            back.setTranslateZ(0.8);
            node.getTransforms().add(facing);
            node.getChildren().addAll(back, fill);
            node.setVisible(false);
        }

        void show(final Block block, final double left, final double viewYaw) {
            // as wide as a dummy is, or bigger over something much bigger: an airship
            final double scale = Math.max(1, Math.max(block.halfWidth(), block.halfDepth()) / 120);
            node.setScaleX(scale);
            node.setScaleY(scale);
            node.setScaleZ(scale);
            node.setTranslateX(block.x());
            node.setTranslateY(-block.top() - 30 * scale);
            node.setTranslateZ(block.z());
            facing.setAngle(viewYaw);
            node.setVisible(true);
            if (left == drawn) {
                return;
            }
            drawn = left;
            // anchored at its left end, so it shrinks toward it
            fill.setScaleX(Math.max(0.001, left));
            fill.setTranslateX(-BAR_WIDTH / 2 * (1 - left));
            fill.setMaterial(SHADES[(int) Math.round(left * (SHADES.length - 1))]);
        }
    }

    /** The bar's colours, red when all but gone through yellow to green when whole. */
    private static final PhongMaterial[] SHADES = shades(20);

    private static PhongMaterial[] shades(final int steps) {
        final Color green = Color.web("#4cd964");
        final Color yellow = Color.web("#f5c518");
        final Color red = Color.web("#e74c3c");
        final PhongMaterial[] shades = new PhongMaterial[steps + 1];
        for (int i = 0; i <= steps; i++) {
            final double left = (double) i / steps;
            final Color c = left > 0.5 ? yellow.interpolate(green, (left - 0.5) * 2) : red.interpolate(yellow, left * 2);
            shades[i] = BoxRig.glow(String.format("#%02x%02x%02x",
                    Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255)));
        }
        return shades;
    }
}
