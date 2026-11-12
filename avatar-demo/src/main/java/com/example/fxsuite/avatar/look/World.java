package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
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
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

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
     * Draws what the weapons have done, and moves on what moves of itself: the airships, where
     * they are {@code skyTime} seconds after the start, the rotors and the lights.
     * {@code viewYaw} turns the bars to face the camera.
     */
    public void show(final Damage damage, final double skyTime, final double seconds, final double viewYaw) {
        time += seconds;
        for (int i = 0; i < parts.length; i++) {
            if (pieces.get(i).moves() && !damage.broken(i)) {
                final Block block = pieces.get(i).at(skyTime).block();
                parts[i].setTranslateX(block.x());
                parts[i].setTranslateY(-(block.top() + block.bottom()) / 2);
                parts[i].setTranslateZ(block.z());
                turns[i].setAngle(block.yaw());
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
                bars[k].show(pieces.get(i).at(skyTime).block(), shown[i], viewYaw);
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

    /** Piece {@code piece}'s box {@code seconds} after the start: where it was, if it moves. */
    Block at(final int piece, final double seconds) {
        return pieces.get(piece).at(seconds).block();
    }

    /** What piece {@code piece} is made of, as it now looks: for its broken pieces. */
    public PhongMaterial material(final int piece) {
        return (PhongMaterial) bodies[piece].getMaterial();
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
        if (kind == Course.Kind.DRONE) {
            return;  // a drone's box is mostly air: nothing to crack
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

    /** One of an airship's parts, as a box in its frame. */
    private static Box section(final Group part, final Course.Section section, final PhongMaterial material) {
        final Box box = new Box(section.halfAcross() * 2, section.halfUp() * 2, section.halfAlong() * 2);
        box.setTranslateX(section.across());
        box.setTranslateY(-(section.up() - Course.AIRSHIP_BOX.up()));
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
