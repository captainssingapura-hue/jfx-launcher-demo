package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Box;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Pond;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The wall towers' ground ({@code Demo.WALL_TOWER}): three towers ({@link WallTower}) on open level ground, end to end
 * along a slight curve, as the bay's hotel towers stand under the park on their roofs: each along the curve, its
 * splayed leg on the outside of it, to the east, its balconied west face on the inside, toward the start; the middle
 * one at the world's middle; the hull ({@link SkyHull}) lying across their roofs, bent round their curve, its bow
 * running out past the north tower as its cantilever. Each the same tower, its doors open and shut as its own seed
 * draws them. Trees scattered
 * round them for kilometres, so they stand in a world many times their size, three things among hundreds. Each in the
 * way through its rooms ({@link Interior}): every room entered by the jet-pack, a balcony at a time; no stairs, no lifts.
 */
public final class TowerMap {

    /** The towers' keys among the buildings drawn ({@link Sites}): after the village's, one each. */
    public static final int KEYS = 600;
    /** How many towers: three, end to end, south to north. */
    public static final int TOWERS = 3;
    /** How far apart along the curve their shafts' middles are: so 35 m between one's end and the next's. */
    public static final double SPACING = 9_500;
    /** How far the curve's middle is from them, to the west: a gentle one, each tower turned 5.4° from the next. */
    public static final double CURVE = 100_000;
    /** Where the avatar starts: 250 m west of the middle tower, facing their balconied west faces. */
    public static final Vec3 START = new Vec3(-25_000, 0, 0);
    /** How many trees, how far out they are scattered, and how far they keep from the towers' feet and the start. */
    static final int TREES = 400;
    static final double SCATTERED = 500_000;
    static final double CLEAR = 4_000;
    /** A tree's trunk: how thick, how high. */
    private static final double TRUNK = 40;
    private static final double TREE = 320;

    /** How far the hull's stern runs past the south tower's end. */
    static final double OVERHANG = 2_500;

    private static final WallTower TOWER = WallTower.standard();
    /** The hull lying on their roofs, bent round the curve they stand on. */
    private static final SkyHull SHAPE = SkyHull.of(SkyHull.Size.STANDARD);
    /** The park on its deck: a pitch, a pool, a court, a garden; its deck opened for the pool sunk in it. */
    private static final SkyPark LAID_OUT = new SkyPark(SHAPE);
    private static final SkyHull HULL = SHAPE.opened(LAID_OUT.holes());
    private static final List<Solid> PARK = LAID_OUT.solids();
    private static List<Solid> drawn;
    private static List<Solid> hullDrawn;
    private static List<WallTower.Opening> openings;

    private TowerMap() {
    }

    /** Tells the simulation all of it: what stands on its ground, where the avatar starts. */
    public static void tell() {
        Course.tower(TowerMap::pieces, () -> START, TowerMap::ponds);
    }

    /**
     * Where tower {@code i} stands, 0 the south one, 1 the middle, 2 the north: on the curve, {@link #SPACING} along it
     * from the next; along it, the curve; across it, west to east, pointing out from the curve's middle, so its splayed
     * leg is on the outside of the curve.
     */
    public static Place at(final int i) {
        final double turn = (i - (TOWERS - 1) / 2.0) * SPACING / CURVE;
        // across a tower is Num.right(yaw), (cos yaw, -sin yaw): pointing out, (cos turn, sin turn), at yaw = -turn
        return new Place(-CURVE + CURVE * Math.cos(turn), CURVE * Math.sin(turn), 0, Num.wrap(-Math.toDegrees(turn)));
    }

    /** Tower {@code i}: the same tower, its doors drawn from its own seed. */
    public static WallTower tower(final int i) {
        return TOWER.seeded(i + 1);
    }

    /** The towers, each one piece; the hull on their roofs, one; and the trees, each one. */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int i = 0; i < TOWERS; i++) {
            pieces.add(tower(i, at(i)));
        }
        pieces.add(hull());
        pieces.addAll(trees());
        return pieces;
    }

    /** How high the towers' roofs are: what the hull lies on. */
    public static double roof() {
        return TOWER.size().high() + WallTower.FLOOR;
    }

    /** How far along the curve from the middle tower the hull's middle is: its stern a little south of the south tower. */
    static double hullMiddle() {
        final double south = -SPACING * (TOWERS - 1) / 2 - TOWER.size().length() / 2;
        return south - OVERHANG + HULL.size().length() / 2;
    }

    /** How far the hull runs past the north tower's end: its cantilever. */
    public static double cantilever() {
        return hullMiddle() + HULL.size().length() / 2 - (SPACING * (TOWERS - 1) / 2 + TOWER.size().length() / 2);
    }

    /**
     * Where the hull lies: on the towers' roofs, on their curve, its middle line over theirs, its stern a little past
     * the south tower's end, so its bow runs out past the north tower as its cantilever.
     */
    public static Place hullAt() {
        final double turn = hullMiddle() / CURVE;
        return new Place(-CURVE + CURVE * Math.cos(turn), CURVE * Math.sin(turn), roof(),
                Num.wrap(-Math.toDegrees(turn)));
    }

    /**
     * Where water lies still: the park's pool, its water's outline bent with the hull and set where the hull lies, a
     * point every metre along each long side; its level a little under the pool's rim. Drawn as the rivers' and the
     * ponds' water is; gone into, to the pool's floor.
     */
    public static List<Pond> ponds() {
        final Box w = LAID_OUT.water();
        final Place at = hullAt();
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        final int n = (int) Math.ceil((w.maxZ() - w.minZ()) / 100);
        final List<Vec2> outline = new ArrayList<>();
        for (int side = 0; side < 2; side++) {
            for (int k = 0; k <= n; k++) {
                final double y = side == 0 ? w.minZ() + (w.maxZ() - w.minZ()) * k / n
                        : w.maxZ() - (w.maxZ() - w.minZ()) * k / n;
                final Point p = HULL.bent(new Point(side == 0 ? w.maxX() : w.minX(), y, 0));
                outline.add(new Vec2(at.x() + right.x() * p.across() + ahead.x() * p.along(),
                        at.z() + right.z() * p.across() + ahead.z() * p.along()));
            }
        }
        return List.of(new Pond(outline, at.height() + w.high()));
    }

    /** The hull on the roofs: drawn by its key after the towers', standing in its box; in the way through its inside. */
    static Course.Piece hull() {
        final Place at = hullAt();
        final Interior interior = HULL.interior(at, PARK);
        final Box inside = interior.placed().inside().bounds();
        final Block box = new Block(at.x(), at.z(), Math.max(-inside.minX(), inside.maxX()),
                Math.max(-inside.minZ(), inside.maxZ()), at.yaw(), at.height(), at.height() + inside.high());
        return Course.Piece.shaped(box, Course.Kind.BUILDING, KEYS + TOWERS, List.of(), List.of(interior), at);
    }

    /** Tower {@code i}, set {@code at} its place: drawn by its key, standing in its box; in the way through its rooms. */
    static Course.Piece tower(final int i, final Place at) {
        final Interior interior = tower(i).interior(at);
        final Box inside = interior.placed().inside().bounds();
        final Block box = new Block(at.x(), at.z(), Math.max(-inside.minX(), inside.maxX()),
                Math.max(-inside.minZ(), inside.maxZ()), at.yaw(), at.height(), at.height() + inside.high());
        return Course.Piece.shaped(box, Course.Kind.BUILDING, KEYS + i, List.of(), List.of(interior), at);
    }

    /** How far from its middle a tower's foot reaches, splayed: no tree nearer than this and a little more. */
    static double reach() {
        return Math.hypot(TOWER.eastOuter(0), TOWER.size().length() / 2);
    }

    /** Trees scattered over the ground round them, each where its seed puts it, none by the towers or the start. */
    static List<Course.Piece> trees() {
        final List<Course.Piece> trees = new ArrayList<>();
        for (int k = 0; k < TREES; k++) {
            final Vec3 way = Num.heading(360 * Num.hash(k, 0x7EE5));
            final double out = SCATTERED * Math.sqrt(Num.hash(k, 0x7EE6));
            final double x = way.x() * out;
            final double z = way.z() * out;
            boolean byOne = Math.hypot(x - START.x(), z - START.z()) < CLEAR;
            for (int i = 0; i < TOWERS; i++) {
                byOne |= Math.hypot(x - at(i).x(), z - at(i).z()) < reach() + CLEAR;
            }
            if (byOne) {
                continue;
            }
            final Block trunk = new Block(x, z, TRUNK / 2, TRUNK / 2, 0, 0, TREE);
            trees.add(new Course.Piece(trunk, Course.Kind.TREE, k % 2, List.of(trunk)));
        }
        return trees;
    }

    /**
     * What tower {@code k} is drawn as, by its key from {@link #KEYS}, about its middle: all of it but its balconies'
     * doors, the same for every one; its doors drawn apart, each as far open as it is, unless it is broken
     * ({@link #door}).
     */
    public static synchronized List<Solid> solids(final int k) {
        if (k == TOWERS) {
            if (hullDrawn == null) {
                hullDrawn = HULL.solids(SkyHull.palette(), PARK);
            }
            return hullDrawn;
        }
        if (k < 0 || k > TOWERS) {
            throw new IllegalArgumentException(TOWERS + " towers here and a hull, not " + (k + 1));
        }
        if (drawn == null) {
            drawn = TOWER.fixed(WallTower.palette());
            openings = TOWER.openings();
        }
        return drawn;
    }

    /** Door {@code door}, all of it, {@code open} as far open, 0 shut to 1 wide, as a tower's solids are laid out. */
    public static List<Solid> door(final int door, final double open) {
        solids(0);
        return openings.get(door).solids(open, WallTower.palette());
    }
}
