package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Box;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The wall tower's ground ({@code Demo.WALL_TOWER}): the tower ({@link WallTower}) alone in the middle of open level
 * ground, its length along the world's z, its balconied west face toward the start; trees scattered round it for
 * kilometres, so it stands in a world many times its size, as one thing among hundreds. In the way through its rooms
 * ({@link Interior}): every room of it entered by the jet-pack, a balcony at a time; no stairs, no lifts.
 */
public final class TowerMap {

    /** The tower's key among the buildings drawn ({@link Sites}): after the village's. */
    public static final int KEYS = 600;
    /** Where it stands: the world's middle, its length along z, its west face toward -x. */
    public static final Place AT = new Place(0, 0, 0, 0);
    /** Where the avatar starts: 150 m west of it, facing its balconies. */
    public static final Vec3 START = new Vec3(-15_000, 0, 0);
    /** How many trees, how far out they are scattered, and how far they keep from the tower and the start. */
    static final int TREES = 400;
    static final double SCATTERED = 500_000;
    static final double CLEAR = 4_000;
    /** A tree's trunk: how thick, how high. */
    private static final double TRUNK = 40;
    private static final double TREE = 320;

    private static final WallTower TOWER = WallTower.standard();
    private static List<Solid> drawn;
    private static List<WallTower.Opening> openings;

    private TowerMap() {
    }

    /** Tells the simulation all of it: what stands on its ground, where the avatar starts. */
    public static void tell() {
        Course.tower(TowerMap::pieces, () -> START);
    }

    /** The tower, as one piece; and the trees, each one. */
    public static List<Course.Piece> pieces() {
        final List<Course.Piece> pieces = new ArrayList<>();
        pieces.add(tower());
        pieces.addAll(trees());
        return pieces;
    }

    /** The tower: drawn by its key, standing in its box; in the way through its rooms. */
    static Course.Piece tower() {
        final Interior interior = TOWER.interior(AT);
        final Box inside = interior.placed().inside().bounds();
        final double half = Math.max(-inside.minX(), inside.maxX());
        final Block box = new Block(AT.x(), AT.z(), half, Math.max(-inside.minZ(), inside.maxZ()), AT.yaw(), AT.height(),
                AT.height() + inside.high());
        return Course.Piece.shaped(box, Course.Kind.BUILDING, KEYS, List.of(), List.of(interior), AT);
    }

    /** Trees scattered over the ground round it, each where its seed puts it, none by the tower or the start. */
    static List<Course.Piece> trees() {
        final List<Course.Piece> trees = new ArrayList<>();
        for (int k = 0; k < TREES; k++) {
            final Vec3 way = Num.heading(360 * Num.hash(k, 0x7EE5));
            final double out = SCATTERED * Math.sqrt(Num.hash(k, 0x7EE6));
            final double x = AT.x() + way.x() * out;
            final double z = AT.z() + way.z() * out;
            if (Math.abs(x - AT.x()) < TOWER.size().length() / 2 + CLEAR
                    && Math.abs(z - AT.z()) < TOWER.size().length() / 2 + CLEAR
                    || Math.hypot(x - START.x(), z - START.z()) < CLEAR) {
                continue;
            }
            final Block trunk = new Block(x, z, TRUNK / 2, TRUNK / 2, 0, 0, TREE);
            trees.add(new Course.Piece(trunk, Course.Kind.TREE, k % 2, List.of(trunk)));
        }
        return trees;
    }

    /**
     * What the tower is drawn as, by its key from {@link #KEYS}, about its middle: all of it but its balconies' doors;
     * those drawn apart, each as far open as it is, unless it is broken ({@link #door}).
     */
    public static synchronized List<Solid> solids(final int k) {
        if (k != 0) {
            throw new IllegalArgumentException("one tower here, not " + (k + 1));
        }
        if (drawn == null) {
            drawn = TOWER.fixed(WallTower.palette());
            openings = TOWER.openings();
        }
        return drawn;
    }

    /** Door {@code door}, all of it, {@code open} as far open, 0 shut to 1 wide, as the tower's solids are laid out. */
    public static List<Solid> door(final int door, final double open) {
        solids(0);
        return openings.get(door).solids(open, WallTower.palette());
    }
}
