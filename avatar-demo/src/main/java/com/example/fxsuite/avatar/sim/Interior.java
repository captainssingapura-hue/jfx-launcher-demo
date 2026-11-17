package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * A building as one thing in the world's way, however many parts it is built of: its parts ({@link Lean}) filed in the
 * cells of its inside ({@link Inside}) that each reaches, and every question asked of it (what is under a body, over
 * it, in its way, where a ray meets it) answered from the parts in the cells the question reaches, not from all of
 * them. The world's level ({@link Obstacles}, by its {@link VolumeGrid}) finds it as one entry; its inside narrows
 * the question to a room or two.
 */
public final class Interior implements Volume {

    /** How far up or down a question about what is under or over a body looks at a time, before looking further. */
    private static final double BAND = 800;
    /** How far a ray goes through it at a time, finding the cells it passes. */
    private static final double STRIDE = 300;

    private final Placed placed;
    private final List<Lean> parts;
    private final int[][] byCell;
    private final Bounds bounds;

    /** Its parts, each given in its own frame, and filed in the cells of its inside; set in the world as it is placed. */
    public Interior(final Placed placed, final List<Lean> parts) {
        this.placed = placed;
        final Inside inside = placed.inside();
        final List<List<Integer>> lists = new ArrayList<>(inside.count());
        for (int c = 0; c < inside.count(); c++) {
            lists.add(new ArrayList<>());
        }
        final List<Lean> world = new ArrayList<>(parts.size());
        for (int p = 0; p < parts.size(); p++) {
            for (final int c : inside.cells(parts.get(p).box())) {
                lists.get(c).add(p);
            }
            world.add(parts.get(p).within(placed.at(), 1));
        }
        this.parts = List.copyOf(world);
        byCell = new int[inside.count()][];
        for (int c = 0; c < byCell.length; c++) {
            byCell[c] = lists.get(c).stream().mapToInt(Integer::intValue).toArray();
        }
        final Box b = placed.world();
        bounds = new Bounds(b.minX(), b.maxX(), b.minZ(), b.maxZ());
    }

    public Placed placed() {
        return placed;
    }

    /** All its parts, in the world. */
    public List<Lean> parts() {
        return parts;
    }

    /** The parts filed in the cells a box in the world reaches: each once, in order. */
    public int[] near(final Box world) {
        if (!placed.world().meets(world)) {
            return new int[0];
        }
        int n = 0;
        int[] found = new int[64];
        for (final int c : placed.cells(world)) {
            final int[] in = byCell[c];
            if (n + in.length > found.length) {
                found = Arrays.copyOf(found, Math.max(2 * found.length, n + in.length));
            }
            System.arraycopy(in, 0, found, n, in.length);
            n += in.length;
        }
        Arrays.sort(found, 0, n);
        int kept = 0;
        for (int k = 0; k < n; k++) {
            if (kept == 0 || found[k] != found[kept - 1]) {
                found[kept++] = found[k];
            }
        }
        return Arrays.copyOf(found, kept);
    }

    private static Box column(final double x, final double z, final double radius, final double from, final double to) {
        return new Box(x - radius, x + radius, z - radius, z + radius, from, to);
    }

    @Override
    public Bounds bounds() {
        return bounds;
    }

    @Override
    public double low() {
        return placed.world().low();
    }

    @Override
    public double high() {
        return placed.world().high();
    }

    /** Whether a circle is over or under it at all: what is under or over it, if anything, its parts say. */
    @Override
    public boolean covers(final double x, final double z, final double radius) {
        return x + radius > bounds.minX() && x - radius < bounds.maxX() && z + radius > bounds.minZ()
                && z - radius < bounds.maxZ();
    }

    /** Its parts' tops are flat: none too steep to stand on. */
    @Override
    public boolean steep(final double x, final double z, final double radius, final double top) {
        return false;
    }

    /** Looking down a band at a time from {@code below}: the first band with a top in it has the highest. */
    @Override
    public double topAt(final double x, final double z, final double radius, final double below) {
        double best = Double.NEGATIVE_INFINITY;
        final double floor = low();
        for (double to = Math.min(below, high()); to >= floor; to -= BAND) {
            final double from = to - BAND;
            for (final int p : near(column(x, z, radius, from, to))) {
                best = Math.max(best, parts.get(p).topAt(x, z, radius, below));
            }
            if (best >= from) {
                break;
            }
        }
        return best;
    }

    /** Looking up a band at a time from {@code above}: the first band with an underside in it has the lowest. */
    @Override
    public double undersideAt(final double x, final double z, final double radius, final double above) {
        double best = Double.POSITIVE_INFINITY;
        final double roof = high();
        for (double from = Math.max(above, low()); from <= roof; from += BAND) {
            final double to = from + BAND;
            for (final int p : near(column(x, z, radius, from, to))) {
                best = Math.min(best, parts.get(p).undersideAt(x, z, radius, above));
            }
            if (best <= to) {
                break;
            }
        }
        return best;
    }

    /** Clear of every part near it, between its feet and its head: each pushed clear of in turn, twice, for corners. */
    @Override
    public Vec2 push(final double x, final double z, final double radius, final double feet, final double head) {
        return push(x, z, x, z, radius, feet, head);
    }

    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        if (head <= low() || feet >= high()) {
            return null;
        }
        final boolean point = ax == bx && az == bz;
        final double reach = radius + 2 * radius;
        final int[] near = near(new Box(Math.min(ax, bx) - reach, Math.max(ax, bx) + reach, Math.min(az, bz) - reach,
                Math.max(az, bz) + reach, feet, head));
        double dx = 0;
        double dz = 0;
        boolean pushed = false;
        for (int pass = 0; pass < 2; pass++) {
            for (final int p : near) {
                final Lean part = parts.get(p);
                final Vec2 push = point ? part.push(ax + dx, az + dz, radius, feet, head)
                        : part.push(ax + dx, az + dz, bx + dx, bz + dz, radius, feet, head);
                if (push != null) {
                    pushed = true;
                    dx += push.x();
                    dz += push.z();
                }
            }
        }
        return pushed && (dx != 0 || dz != 0) ? new Vec2(dx, dz) : null;
    }

    /**
     * Where a ray first meets any of its parts: only the part of the ray within its box is followed, a stride at a
     * time, the parts in the cells each stride passes tested as they are first come to; once the nearest hit so far
     * is within the strides followed, none further on can be nearer.
     */
    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        final Box box = placed.world();
        // the ray's way in and out of its box (heights up, y down)
        final double[] at = {from.x(), from.z(), -from.y()};
        final double[] go = {direction.x(), direction.z(), -direction.y()};
        final double[] lo = {box.minX(), box.minZ(), box.low()};
        final double[] hi = {box.maxX(), box.maxZ(), box.high()};
        double enter = 0;
        double exit = reach;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(go[axis]) < 1e-12) {
                if (at[axis] < lo[axis] || at[axis] > hi[axis]) {
                    return null;
                }
                continue;
            }
            final double t1 = (lo[axis] - at[axis]) / go[axis];
            final double t2 = (hi[axis] - at[axis]) / go[axis];
            enter = Math.max(enter, Math.min(t1, t2));
            exit = Math.min(exit, Math.max(t1, t2));
        }
        if (enter > exit) {
            return null;
        }
        final BitSet tried = new BitSet(parts.size());
        Hit first = null;
        for (double t = enter; t <= exit; t += STRIDE) {
            final double end = Math.min(exit, t + STRIDE);
            final Vec3 a = from.add(direction.multiply(t));
            final Vec3 b = from.add(direction.multiply(end));
            final Box stride = new Box(Math.min(a.x(), b.x()) - 1, Math.max(a.x(), b.x()) + 1,
                    Math.min(a.z(), b.z()) - 1, Math.max(a.z(), b.z()) + 1, Math.min(-a.y(), -b.y()) - 1,
                    Math.max(-a.y(), -b.y()) + 1);
            for (final int p : near(stride)) {
                if (tried.get(p)) {
                    continue;
                }
                tried.set(p);
                final Hit hit = parts.get(p).ray(from, direction, first == null ? reach : first.distance());
                if (hit != null && (first == null || hit.distance() < first.distance())) {
                    first = hit;
                }
            }
            if (first != null && first.distance() <= end) {
                break;
            }
        }
        return first;
    }

    @Override
    public boolean overlaps(final Block block) {
        final Vec2 reach = new Vec2(block.halfWidth() + block.halfDepth(), block.halfWidth() + block.halfDepth());
        for (final int p : near(new Box(block.x() - reach.x(), block.x() + reach.x(), block.z() - reach.z(),
                block.z() + reach.z(), block.bottom(), block.highest()))) {
            if (parts.get(p).overlaps(block)) {
                return true;
            }
        }
        return false;
    }
}
