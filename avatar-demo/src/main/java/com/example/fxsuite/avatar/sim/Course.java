package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The layout of the world, as data: what is where, and how big. {@link World} draws it and
 * {@link Obstacles} makes it solid, so what you see is exactly what blocks you. The same every
 * run. Pure: no scene graph.
 *
 * <ul>
 *   <li>a staircase of crates straight ahead of the start, each step a jump up from the last;</li>
 *   <li>crates scattered about, from knee height to just out of a jump's reach;</li>
 *   <li>tall pillars, to walk between and fly over and land on;</li>
 *   <li>gates up in the air, four bars round an opening to fly through;</li>
 *   <li>targets to shoot at: wooden dummies standing in rows, each facing the start, drones
 *       hovering round it, and, high over everything else and far out, three great armoured
 *       airships going slowly round on their {@link Route}s ({@link Piece#at}).</li>
 * </ul>
 *
 * <p>Crates and targets can be broken ({@link Damage}); pillars and gates are what the world is
 * built on, and stand whatever is thrown at them.
 */
public final class Course {

    public enum Kind {
        PILLAR(false), CRATE(true), GATE(false), DUMMY(true), DRONE(true), AIRSHIP(true);

        private final boolean breakable;

        Kind(final boolean breakable) {
            this.breakable = breakable;
        }

        /** Whether it can be worn down and broken. */
        public boolean breakable() {
            return breakable;
        }

        /** Whether it is something put there to be shot at. */
        public boolean target() {
            return this == DUMMY || this == DRONE || this == AIRSHIP;
        }
    }

    /**
     * One thing in the world, with what it is and which of its kind's colours to draw it in:
     * {@code block}, the box it fills, and {@code solid}, the blocks that are actually in the way
     * (the same box, but for an airship, whose hull, fins, gondola and engines are each one).
     * Broken, all of it goes at once.
     */
    public record Piece(Block block, Kind kind, int tint, List<Block> solid, Route route) {

        public Piece {
            solid = List.copyOf(solid);
        }

        /** A piece that stays where it is, its solid blocks given. */
        public Piece(final Block block, final Kind kind, final int tint, final List<Block> solid) {
            this(block, kind, tint, solid, null);
        }

        /** A piece that is just its box. */
        public Piece(final Block block, final Kind kind, final int tint) {
            this(block, kind, tint, List.of(block));
        }

        /** Whether it moves of itself, along its {@link #route}: an airship. */
        public boolean moves() {
            return route != null;
        }

        /** Where it is {@code seconds} after the start: along its route, if it has one, or where it always is. */
        public Piece at(final double seconds) {
            if (route == null) {
                return this;
            }
            final Place place = route.at(seconds);
            return new Piece(AIRSHIP_BOX.placed(place), kind, tint, airship(place), route);
        }
    }

    /**
     * The way an airship goes: round the start, {@code radius} out, at {@code height}, from
     * {@code heading} at the start, at {@code speed} (units a second; round the other way when
     * negative), nose first along its way.
     */
    public record Route(double heading, double radius, double height, double speed) {

        /** Where along it, {@code seconds} after the start. */
        public Place at(final double seconds) {
            final double angle = heading + Math.toDegrees(speed * seconds / radius);
            final Vec3 at = Num.heading(angle).multiply(radius);
            // going round, its way is square to the line out from the start
            return new Place(at.x(), at.z(), height, Num.wrap(angle + (speed >= 0 ? 90 : -90)));
        }
    }

    /**
     * A part of an airship, a box in the airship's own frame: its middle {@code across} to the
     * right of the hull's line, {@code along} it toward the nose and {@code up} from it, and half
     * as wide, long and tall as these.
     */
    public record Section(double across, double along, double up, double halfAcross, double halfAlong,
                          double halfUp) {

        /** As a block, for an airship at ({@code x}, {@code height}, {@code z}) heading {@code yaw}. */
        public Block placed(final double x, final double height, final double z, final double yaw) {
            final Vec3 at = new Vec3(x, 0, z).add(Num.right(yaw).multiply(across))
                    .add(Num.heading(yaw).multiply(along));
            return new Block(at.x(), at.z(), halfAcross, halfAlong, yaw, height + up - halfUp, height + up + halfUp);
        }

        /** As a block, for an airship at {@code place}. */
        public Block placed(final Place place) {
            return placed(place.x(), place.height(), place.z(), place.yaw());
        }
    }

    /**
     * An airship's hull: stepping out from a blunt nose to its full girth and back in to the
     * tail, 18 m long and 4.4 m across. Then a cross of fins at the tail, a gondola under the
     * middle, and an engine either side on a pylon. Each is solid.
     */
    public static final List<Section> HULL = List.of(
            new Section(0, 840, 0, 100, 60, 100),
            new Section(0, 700, 0, 160, 80, 160),
            new Section(0, 520, 0, 200, 100, 200),
            new Section(0, 40, 0, 220, 380, 220),
            new Section(0, -460, 0, 200, 120, 200),
            new Section(0, -680, 0, 160, 100, 160),
            new Section(0, -840, 0, 100, 60, 100));
    public static final List<Section> FINS = List.of(
            new Section(0, -760, 250, 8, 140, 130),
            new Section(0, -760, -250, 8, 140, 130),
            new Section(-250, -760, 0, 130, 140, 8),
            new Section(250, -760, 0, 130, 140, 8));
    public static final Section GONDOLA = new Section(0, 80, -275, 75, 190, 55);
    public static final List<Section> PYLONS = List.of(
            new Section(-255, -60, -40, 40, 30, 7),
            new Section(255, -60, -40, 40, 30, 7));
    public static final List<Section> ENGINES = List.of(
            new Section(-330, -60, -40, 40, 80, 40),
            new Section(330, -60, -40, 40, 80, 40));
    /** All of an airship's box: out to the fins' tips, nose to tail, gondola to the top fin. */
    public static final Section AIRSHIP_BOX = new Section(0, 0, 25, 380, 900, 355);
    /**
     * The airships' routes: where each starts (heading from the start, and how far), how high it
     * flies, and how fast round, in units a second: slowly, a lap taking the best part of two
     * minutes, the middle one going round the other way.
     */
    private static final double[][] AIRSHIPS = {
            {70, 6500, 3100, 400},
            {190, 7500, 3300, -350},
            {300, 5500, 3500, 300},
    };

    /** The heading the avatar starts on: the staircase lies along it. */
    public static final double START_YAW = 30;
    public static final int STEPS = 5;
    public static final double STEP_RISE = 60;
    public static final double GATE_SPAN = 600;
    public static final double GATE_BAR = 36;
    /** The range of dummies: which way from the start, and how far. */
    public static final double RANGE_HEADING = -40;
    public static final double RANGE_OUT = 1100;
    /** A dummy: a board on legs, as wide, as thick and as tall as this; and how far apart in a row. */
    public static final double DUMMY_WIDTH = 90;
    public static final double DUMMY_DEPTH = 24;
    public static final double DUMMY_HEIGHT = 180;
    public static final double DUMMY_APART = 200;
    /** The drones: how many, how wide across, and how deep top to bottom. */
    public static final int DRONES = 8;
    public static final double DRONE_SIZE = 130;
    public static final double DRONE_HEIGHT = 40;

    private final List<Piece> pieces = new ArrayList<>();
    private final List<Gate> gates = new ArrayList<>();

    public Course() {
        final Random random = new Random(7);

        final double c = Math.cos(Math.toRadians(START_YAW));
        final double s = Math.sin(Math.toRadians(START_YAW));
        for (int i = 0; i < STEPS; i++) {
            final double ahead = 500 + i * 170;
            pieces.add(new Piece(Block.standing(s * ahead, c * ahead, 170, 170, STEP_RISE * (i + 1), START_YAW),
                    Kind.CRATE, i % 3));
        }

        for (int i = 0; i < 90; i++) {
            final double x = spot(random);
            final double z = spot(random);
            if (Math.hypot(x, z) < 1400) {
                continue;  // keep the start, and the staircase, clear
            }
            final double width = 100 + random.nextDouble() * 160;
            final double depth = 100 + random.nextDouble() * 160;
            final double height = 40 + random.nextDouble() * 200;
            pieces.add(new Piece(Block.standing(x, z, width, depth, height, random.nextDouble() * 90),
                    Kind.CRATE, random.nextInt(3)));
        }

        for (int i = 0; i < 170; i++) {
            final double x = spot(random);
            final double z = spot(random);
            if (Math.hypot(x, z) < 1400) {
                continue;
            }
            final double size = 120 + random.nextDouble() * 200;
            final double height = 300 + Math.pow(random.nextDouble(), 2) * 2300;
            pieces.add(new Piece(Block.standing(x, z, size, size, height, 0), Kind.PILLAR, random.nextInt(4)));
        }

        for (int i = 0; i < 14; i++) {
            final double angle = i * 2 * Math.PI / 14 + 0.4;
            final double reach = 1800 + random.nextDouble() * 8000;
            gate(Math.sin(angle) * reach, 500 + random.nextDouble() * 1500, Math.cos(angle) * reach,
                    random.nextDouble() * 180, i % 3);
        }

        // the targets, last, in the open ring round the start: a range of dummies off to the
        // left, another few behind, and drones hovering all round
        range(RANGE_HEADING, RANGE_OUT, 5);
        range(START_YAW + 180, RANGE_OUT + 50, 3);
        for (int i = 0; i < DRONES; i++) {
            final double heading = 10 + 360.0 * i / DRONES;
            final Vec3 at = Num.heading(heading).multiply(i % 2 == 0 ? 800 : 1200);
            final double low = 280 + 110 * (i % 4);
            pieces.add(new Piece(new Block(at.x(), at.z(), DRONE_SIZE / 2, DRONE_SIZE / 2, heading,
                    low, low + DRONE_HEIGHT), Kind.DRONE, i % 3));
        }

        // and the airships, high over everything else, going slowly round far out
        for (int i = 0; i < AIRSHIPS.length; i++) {
            final double[] a = AIRSHIPS[i];
            final Route route = new Route(a[0], a[1], a[2], a[3]);
            final Place start = route.at(0);
            pieces.add(new Piece(AIRSHIP_BOX.placed(start), Kind.AIRSHIP, i % 2, airship(start), route));
        }
    }

    /** An airship's solid parts, at {@code place}: the hull, the fins, the gondola, the pylons and the engines. */
    public static List<Block> airship(final Place place) {
        final List<Block> solid = new ArrayList<>();
        for (final List<Section> parts : List.of(HULL, FINS, List.of(GONDOLA), PYLONS, ENGINES)) {
            for (final Section part : parts) {
                solid.add(part.placed(place));
            }
        }
        return solid;
    }

    /** How high the airships' hulls run, each one's line through the middle of its hull: for the graphics. */
    public static double hullHeight(final Piece airship) {
        return airship.block().bottom() + AIRSHIP_BOX.halfUp() - AIRSHIP_BOX.up();
    }

    /**
     * A row of {@code count} dummies, {@code out} from the start along {@code heading}, each
     * facing back toward it, side by side.
     */
    private void range(final double heading, final double out, final int count) {
        final Vec3 middle = Num.heading(heading).multiply(out);
        final Vec3 across = Num.right(heading);
        for (int k = 0; k < count; k++) {
            final Vec3 at = middle.add(across.multiply((k - (count - 1) / 2.0) * DUMMY_APART));
            pieces.add(new Piece(Block.standing(at.x(), at.z(), DUMMY_WIDTH, DUMMY_DEPTH, DUMMY_HEIGHT, heading),
                    Kind.DUMMY, k % 2));
        }
    }

    public List<Piece> pieces() {
        return List.copyOf(pieces);
    }

    /** The gates' openings, to fly through. */
    public List<Gate> gates() {
        return List.copyOf(gates);
    }

    public Obstacles obstacles() {
        // each piece's solid blocks, numbered as the piece is
        final List<Block> blocks = new ArrayList<>();
        final List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            for (final Block block : pieces.get(i).solid()) {
                blocks.add(block);
                numbers.add(i);
            }
        }
        return new Obstacles(blocks, gates, numbers);
    }

    /**
     * A square frame standing up in the air, centred at the given height: four bars, each a
     * block turned with the gate, round an open middle.
     */
    private void gate(final double x, final double height, final double z, final double yaw, final int tint) {
        final double half = GATE_SPAN / 2;
        final double bar = GATE_BAR / 2;
        final double c = Math.cos(Math.toRadians(yaw));
        final double s = Math.sin(Math.toRadians(yaw));
        // top and bottom run the full width; the sides stand between them
        pieces.add(new Piece(new Block(x, z, half + bar, bar, yaw, height + half - bar, height + half + bar),
                Kind.GATE, tint));
        pieces.add(new Piece(new Block(x, z, half + bar, bar, yaw, height - half - bar, height - half + bar),
                Kind.GATE, tint));
        for (final double side : new double[] {-half, half}) {
            // across the gate's own width, turned with it as Rotate(yaw, Y) turns (side, 0, 0)
            pieces.add(new Piece(new Block(x + side * c, z - side * s, bar, bar, yaw, height - half, height + half),
                    Kind.GATE, tint));
        }
        // the hole the bars stand round: each bar's inner edge is half a bar in from its middle
        gates.add(new Gate(x, z, yaw, height, GATE_SPAN - GATE_BAR));
    }

    private static double spot(final Random random) {
        return (random.nextDouble() * 2 - 1) * (Body.EDGE - 400);
    }
}
