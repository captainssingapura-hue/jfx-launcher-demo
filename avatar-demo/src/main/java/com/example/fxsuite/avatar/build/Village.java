package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Prism;
import com.example.fxsuite.avatar.sim.River;
import com.example.fxsuite.avatar.sim.Survey;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A meadow, a river winding across it, and a small village by its bank: the open country.
 *
 * <ul>
 *   <li><b>The river</b> ({@link River}): across the whole world, east to west, its middle two long
 *       gentle waves laid over each other; so wide, its banks mud; shallow, waded through.</li>
 *   <li><b>A footbridge</b> over it where the village is, square across it: a timber deck on the level,
 *       ramped up to from each bank, railed along both sides.</li>
 *   <li><b>The village</b>, north of the river: earth houses ({@link EarthHouse}) round a stone well,
 *       each facing it, each its own size and finish, as its seed has them: its walls, its roof, its
 *       window, its woodwork.</li>
 *   <li><b>Trees</b> along both banks, and here and there across the meadow.</li>
 * </ul>
 *
 * <p>The avatar starts south of the river, before the bridge, the village beyond it. Its houses are
 * drawn by their keys from {@link #KEYS}.
 */
public final class Village {

    private Village() {
    }

    /** Its houses' keys, to draw them by, start here: past the pavilions'. */
    public static final int KEYS = 500;
    /** What is of timber standing about (a railing, a post) is drawn as a wall of this colour: timber. */
    private static final int TIMBER = 2;

    /** The river: so wide, its banks so wide each side; its water and its banks so high over the ground. */
    public static final double RIVER = 1_200;
    public static final double BANK = 250;
    public static final double WATER_TOP = 10;
    public static final double BANK_TOP = 3;
    /** Its line, a point so far apart, from one edge of the world to the other. */
    private static final double STEP = 500;
    private static final double REACH = com.example.fxsuite.avatar.sim.Body.EDGE;

    /** Where the river's middle is, north or south, at {@code x} east: two gentle waves laid over each other, south of the village. */
    public static double riverAt(final double x) {
        return 4_000 * Math.sin(x / 18_000 + 0.3) + 1_500 * Math.sin(x / 7_000 + 1.3) - 6_600;
    }

    /** Which way the river runs at {@code x}: a heading, as {@link Num#heading} has them. */
    static double riverHeading(final double x) {
        final double slope = 4_000 / 18_000.0 * Math.cos(x / 18_000 + 0.3) + 1_500 / 7_000.0 * Math.cos(x / 7_000 + 1.3);
        return Math.toDegrees(Math.atan2(1, slope));
    }

    /** The river across the world. */
    public static List<River> rivers() {
        final List<Vec2> line = new ArrayList<>();
        for (double x = -REACH; x <= REACH; x += STEP) {
            line.add(new Vec2(x, riverAt(x)));
        }
        return List.of(new River(line, RIVER, WATER_TOP));
    }

    /** The well, the village's middle: so far north of the world's middle; its stone ring so far round, so high; its water within. */
    public static final Vec3 WELL = new Vec3(0, 0, 4_000);
    private static final double WELL_RING = 90;
    private static final double WELL_INSIDE = 65;
    private static final double WELL_HIGH = 75;

    /** How many houses; how near and how far from the well; how far apart, at least, beyond their walls; how far clear of the river. */
    public static final int HOUSES = 12;
    private static final double NEAREST = 1_800;
    private static final double FURTHEST = 6_000;
    private static final double APART = 700;
    private static final double DRY = RIVER / 2 + BANK + 1_500;
    /** The way down to the bridge, kept clear: so wide each side of its line. */
    private static final double WAY = 600;
    /** The seed the village is drawn from: the same seed, the same village. */
    private static final long SEED = 11;

    /** A house in the village: as big as it is built, of what, standing where, facing the well. */
    public record House(EarthHouse.Size size, Material walls, Material roof, Material window, Material woodwork,
                        Place place) {
    }

    /** Its houses, drawn from its seed, each where it fits. */
    public static final List<House> VILLAGE = houses();

    private static List<House> houses() {
        final SplittableRandom random = new SplittableRandom(SEED);
        final List<House> houses = new ArrayList<>();
        int tries = 0;
        while (houses.size() < HOUSES && tries++ < 10_000) {
            final double width = 450 + 300 * random.nextDouble();
            final double doorHigh = 205 + 15 * random.nextDouble();
            final double walls = Math.max(doorHigh + 20, 235 + 50 * random.nextDouble());
            final EarthHouse.Size size = new EarthHouse.Size(width, walls, 40 + 25 * random.nextDouble(),
                    120 + 110 * random.nextDouble(), 25 + 25 * random.nextDouble(), 90 + 20 * random.nextDouble(), doorHigh,
                    random.nextDouble() < 0.6 ? 50 + 40 * random.nextDouble() : 0, random.nextDouble() < 0.7);
            final double heading = 360 * random.nextDouble();
            final double out = NEAREST + (FURTHEST - NEAREST) * Math.sqrt(random.nextDouble());
            final Vec3 way = Num.heading(heading);
            final double x = WELL.x() + way.x() * out;
            final double z = WELL.z() + way.z() * out;
            final double reach = EarthHouse.of(size).eave();
            final boolean dry = Math.abs(z - riverAt(x)) > DRY + reach;
            final boolean offTheWay = Math.abs(x - WELL.x()) > WAY + reach || z > WELL.z();
            final boolean apart = houses.stream().allMatch(h -> Math.hypot(h.place().x() - x, h.place().z() - z)
                    > reach + EarthHouse.of(h.size()).eave() + APART);
            final Material walled = EarthHouse.WALLS.get(random.nextInt(EarthHouse.WALLS.size()));
            final Material roof = EarthHouse.ROOFS.get(random.nextInt(EarthHouse.ROOFS.size()));
            final Material window = random.nextDouble() < 0.3 ? Material.GLASS : Material.TIMBER;
            final Material wood = random.nextDouble() < 0.2 ? Material.PAINTED : Material.TIMBER;
            if (!dry || !offTheWay || !apart) {
                continue;
            }
            // its door facing the well
            final double facing = Math.toDegrees(Math.atan2(WELL.x() - x, WELL.z() - z));
            houses.add(new House(size, walled, roof, window, wood, new Place(x, z, 0, facing)));
        }
        return List.copyOf(houses);
    }

    private static final Map<Integer, List<Solid>> DRESSED = new ConcurrentHashMap<>();

    /** House {@code k}, dressed: of what it is built. Worked out once, when first wanted. */
    public static List<Solid> solids(final int k) {
        return DRESSED.computeIfAbsent(k, i -> {
            final House house = VILLAGE.get(i);
            return EarthHouse.of(house.size()).solids(EarthHouse.palette().with(Map.of(Role.WALL, house.walls(),
                    Role.SPIRE, house.walls(), Role.ROOF, house.roof(), Role.GLAZING, house.window(),
                    Role.FRAME, house.woodwork())));
        });
    }

    /** The footbridge: so wide, its deck so high, so far past the banks each side; its ramps, one in four; its railings so high. */
    public static final double BRIDGE_WIDE = 300;
    public static final double DECK = 60;
    private static final double BEYOND = 200;
    private static final double RAMP = 4 * DECK;
    private static final double RAILING = 100;
    private static final double RAILING_THICK = 8;

    /** Where the avatar starts: south of the river, before the bridge, the village beyond it. */
    public static Vec3 start() {
        return new Vec3(0, 0, riverAt(0) - 3_000);
    }

    /** The meadow's land ({@link Terrains#meadow}), surveyed: worked out once, when first wanted. */
    public static Survey land() {
        return Land.SURVEY;
    }

    private static final class Land {
        static final Survey SURVEY = Terrains.meadow().survey();
    }

    /** Tells the simulation all of the meadow village: what stands in it, its river, where the avatar starts, its land. */
    public static void tell() {
        Course.village(Village::pieces, Village::rivers, Village::start, Village::land);
    }

    /** What stands in the meadow: its houses, each one piece; the well; the footbridge; the trees. */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < VILLAGE.size(); k++) {
            final House house = VILLAGE.get(k);
            pieces.add(Town.building(solids(k), house.place(), KEYS + k, EarthHouse.of(house.size()).shape(), 1));
        }
        pieces.addAll(well());
        pieces.addAll(bridge());
        pieces.addAll(trees());
        return pieces;
    }

    /** The well: a ring of stone in the way, its water within; two posts and a beam over it. */
    private static List<Course.Piece> well() {
        final List<Course.Piece> well = new ArrayList<>();
        well.add(Course.Piece.shaped(new Block(WELL.x(), WELL.z(), WELL_RING, WELL_RING, 0, 0, WELL_HIGH), Course.Kind.GARDEN,
                Course.GARDEN_STONE, List.of(), List.of(new Prism(circle(WELL_RING), List.of(circle(WELL_INSIDE)), 0, WELL_HIGH)),
                new Place(WELL.x(), WELL.z(), 0, 0)));
        well.add(new Course.Piece(new Block(WELL.x(), WELL.z(), WELL_INSIDE, WELL_INSIDE, 0, 0, WELL_HIGH - 30),
                Course.Kind.GARDEN, Course.GARDEN_WATER, List.of()));
        for (final double side : new double[] {-1, 1}) {
            final Block post = Block.standing(WELL.x() + side * (WELL_RING - 6), WELL.z(), 10, 10, WELL_HIGH + 110, 0);
            well.add(new Course.Piece(post, Course.Kind.WALL, TIMBER, List.of(post)));
        }
        final Block beam = new Block(WELL.x(), WELL.z(), WELL_RING, 6, 0, WELL_HIGH + 100, WELL_HIGH + 112);
        well.add(new Course.Piece(beam, Course.Kind.WALL, TIMBER, List.of(beam)));
        return well;
    }

    private static List<Vec2> circle(final double r) {
        final List<Vec2> circle = new ArrayList<>();
        for (int k = 0; k < 32; k++) {
            final double a = 2 * Math.PI * k / 32;
            circle.add(new Vec2(WELL.x() + r * Math.sin(a), WELL.z() + r * Math.cos(a)));
        }
        return circle;
    }

    /**
     * The footbridge, square across the river at the village: its deck on the level over the water and its
     * banks, and past them; a ramp up to it from each side; its railings along both its sides over the deck.
     */
    private static List<Course.Piece> bridge() {
        final List<Course.Piece> bridge = new ArrayList<>();
        final double x = WELL.x();
        final double z = riverAt(x);
        // square across the river: along the way it runs, turned a quarter
        final double across = riverHeading(x) - 90;
        final Vec3 ahead = Num.heading(across);
        final double half = RIVER / 2 + BANK + BEYOND;
        final Block deck = new Block(x, z, BRIDGE_WIDE / 2, half, across, 0, DECK);
        bridge.add(new Course.Piece(deck, Course.Kind.GARDEN, Course.GARDEN_TIMBER, List.of(deck)));
        for (final double end : new double[] {-1, 1}) {
            final Vec3 head = new Vec3(x + ahead.x() * end * half, 0, z + ahead.z() * end * half);
            final Vec3 foot = new Vec3(x + ahead.x() * end * (half + RAMP), 0, z + ahead.z() * end * (half + RAMP));
            final Block ramp = Block.ramp(foot.x(), foot.z(), 0, head.x(), head.z(), DECK, BRIDGE_WIDE);
            bridge.add(new Course.Piece(ramp, Course.Kind.GARDEN, Course.GARDEN_TIMBER, List.of(ramp)));
            // a railing along each side of the deck
            final Vec3 side = new Vec3(ahead.z(), 0, -ahead.x()).multiply(end * (BRIDGE_WIDE / 2 - RAILING_THICK / 2));
            final Block railing = new Block(x + side.x(), z + side.z(), RAILING_THICK / 2, half, across, DECK, DECK + RAILING);
            bridge.add(new Course.Piece(railing, Course.Kind.WALL, TIMBER, List.of(railing)));
        }
        return bridge;
    }

    /** How near the world's middle trees grow; how far apart along the banks; how many across the meadow. */
    private static final double WOODED = 80_000;
    private static final double ALONG = 2_500;
    private static final int MEADOW_TREES = 30;
    /** How wide a tree's trunk is. */
    private static final double TRUNK = 36;

    /**
     * Its trees: along both banks, every so far, a little in and out; and across the meadow here and there;
     * none in the water, by the bridge, or by a house.
     */
    private static List<Course.Piece> trees() {
        final SplittableRandom random = new SplittableRandom(SEED + 1);
        final List<Vec2> at = new ArrayList<>();
        for (double x = -WOODED; x <= WOODED; x += ALONG) {
            for (final double side : new double[] {-1, 1}) {
                final double tx = x + (random.nextDouble() - 0.5) * ALONG * 0.6;
                final double out = RIVER / 2 + BANK + 300 + 600 * random.nextDouble();
                at.add(new Vec2(tx, riverAt(tx) + side * out));
            }
        }
        for (int k = 0; k < MEADOW_TREES; k++) {
            final Vec3 way = Num.heading(360 * random.nextDouble());
            final double out = 15_000 + 45_000 * random.nextDouble();
            at.add(new Vec2(WELL.x() + way.x() * out, WELL.z() + way.z() * out));
        }
        final List<Course.Piece> trees = new ArrayList<>();
        int k = 0;
        for (final Vec2 t : at) {
            final boolean wet = Math.abs(t.z() - riverAt(t.x())) < RIVER / 2 + BANK + 150;
            final boolean byTheBridge = Math.abs(t.x() - WELL.x()) < 1_500 && Math.abs(t.z() - riverAt(t.x())) < 3_000;
            final boolean byAHouse = VILLAGE.stream().anyMatch(h -> Math.hypot(h.place().x() - t.x(), h.place().z() - t.z())
                    < EarthHouse.of(h.size()).eave() + 500);
            final boolean byTheWell = Math.hypot(t.x() - WELL.x(), t.z() - WELL.z()) < 1_000;
            if (wet || byTheBridge || byAHouse || byTheWell) {
                continue;
            }
            // standing on the ground there: its foot as low as the ground under it goes, so it is never clear of it
            double foot = Double.POSITIVE_INFINITY;
            for (final double dx : new double[] {-TRUNK / 2, TRUNK / 2}) {
                for (final double dz : new double[] {-TRUNK / 2, TRUNK / 2}) {
                    foot = Math.min(foot, land().height(t.x() + dx, t.z() + dz));
                }
            }
            final Block trunk = new Block(t.x(), t.z(), TRUNK / 2, TRUNK / 2, 0, foot, foot + 320);
            trees.add(new Course.Piece(trunk, Course.Kind.TREE, k++ % 2, List.of(trunk)));
        }
        return trees;
    }
}
