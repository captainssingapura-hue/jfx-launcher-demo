package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * A building as one thing in the world's way, however many volumes it is made of: to the world one volume, its box
 * all round, found as one entry among the world's ({@link VolumeGrid}) and passed over by a question asked away from
 * it; asked about where it is, its own parts answer, those of them in the cells of its own index the question reaches
 * ({@link #near}). So its fine model is at work only where something is at it.
 *
 * <p>Some of its parts break on their own ({@code breakable}: panes of its glass): each worn down by what strikes it
 * ({@link #struck}), so much at a time of what it can take ({@link #toughness}), and worn through, gone, in no one's
 * way, the building standing. Each drawn by a key of its own, from {@link #drawFrom} on, so its drawing can leave the
 * broken ones out.
 *
 * <p>Made in a building's own frame, then put where it stands ({@link #placed}), as any building's shape is.
 */
public final class Assembly implements Volume {

    /** Its index's cells: so wide each way across the ground, so high. */
    static final double CELL = 800;
    static final double CELL_UP = 500;
    /** How far up or down a question about what is under or over a body looks at a time, before looking further. */
    private static final double BAND = 800;
    /** How far a ray goes through it at a time, finding the cells it passes. */
    private static final double STRIDE = 400;

    private final List<Volume> parts;
    /** Each part's box: min x, max x, min z, max z, low, high, six to a part. */
    private final double[] box;
    private final Bounds bounds;
    private final double low;
    private final double high;
    private final int nx;
    private final int nz;
    private final int ny;
    private final int[][] cells;
    /** How many of its parts, from the first, never break; the rest break, each on its own. */
    private final int fixed;
    private final double toughness;
    private final int drawFrom;
    /** How much of each breakable part is whole, 1 to 0 broken; which parts are gone. */
    private final double[] whole;
    private final boolean[] gone;
    /** The heights its parts answer between ({@link #banded}): all of it, unless it is answering for a band of it. */
    private final double bandFrom;
    private final double bandTo;

    /**
     * Its parts that never break ({@code fixed}) and those that do ({@code breakable}), each able to take
     * {@code toughness} before it breaks; the breakable ones drawn by keys from {@code drawFrom} on, in order.
     */
    public Assembly(final List<Volume> fixed, final List<Volume> breakable, final double toughness, final int drawFrom) {
        final List<Volume> all = new ArrayList<>(fixed);
        all.addAll(breakable);
        if (all.isEmpty()) {
            throw new IllegalArgumentException("an assembly of something");
        }
        this.parts = List.copyOf(all);
        this.fixed = fixed.size();
        this.toughness = toughness;
        this.drawFrom = drawFrom;
        this.whole = new double[breakable.size()];
        Arrays.fill(this.whole, 1);
        this.gone = new boolean[parts.size()];
        this.bandFrom = Double.NEGATIVE_INFINITY;
        this.bandTo = Double.POSITIVE_INFINITY;
        this.box = new double[6 * parts.size()];
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        for (int p = 0; p < parts.size(); p++) {
            final Volume v = parts.get(p);
            final Bounds b = v.bounds();
            box[6 * p] = b.minX();
            box[6 * p + 1] = b.maxX();
            box[6 * p + 2] = b.minZ();
            box[6 * p + 3] = b.maxZ();
            box[6 * p + 4] = v.low();
            box[6 * p + 5] = v.high();
            minX = Math.min(minX, b.minX());
            maxX = Math.max(maxX, b.maxX());
            minZ = Math.min(minZ, b.minZ());
            maxZ = Math.max(maxZ, b.maxZ());
            lo = Math.min(lo, v.low());
            hi = Math.max(hi, v.high());
        }
        this.bounds = new Bounds(minX, maxX, minZ, maxZ);
        this.low = lo;
        this.high = hi;
        this.nx = Math.max(1, (int) Math.ceil((maxX - minX) / CELL));
        this.nz = Math.max(1, (int) Math.ceil((maxZ - minZ) / CELL));
        this.ny = Math.max(1, (int) Math.ceil((hi - lo) / CELL_UP));
        final List<List<Integer>> lists = new ArrayList<>(nx * nz * ny);
        for (int c = 0; c < nx * nz * ny; c++) {
            lists.add(new ArrayList<>());
        }
        for (int p = 0; p < parts.size(); p++) {
            final int[] r = range(box[6 * p], box[6 * p + 1], box[6 * p + 2], box[6 * p + 3], box[6 * p + 4],
                    box[6 * p + 5]);
            for (int i = r[0]; i <= r[1]; i++) {
                for (int k = r[2]; k <= r[3]; k++) {
                    for (int j = r[4]; j <= r[5]; j++) {
                        lists.get(cell(i, k, j)).add(p);
                    }
                }
            }
        }
        this.cells = new int[lists.size()][];
        for (int c = 0; c < cells.length; c++) {
            cells[c] = lists.get(c).stream().mapToInt(Integer::intValue).toArray();
        }
    }

    /** The same, its breakable parts as whole as {@code whole}, the parts {@code gone} gone: all else shared. */
    private Assembly(final Assembly was, final double[] whole, final boolean[] gone) {
        this(was, whole, gone, was.bandFrom, was.bandTo);
    }

    /** The same, answering only for its parts between {@code from} and {@code to} up: all else shared. */
    private Assembly(final Assembly was, final double[] whole, final boolean[] gone, final double from,
                     final double to) {
        this.bandFrom = from;
        this.bandTo = to;
        this.parts = was.parts;
        this.box = was.box;
        this.bounds = was.bounds;
        this.low = was.low;
        this.high = was.high;
        this.nx = was.nx;
        this.nz = was.nz;
        this.ny = was.ny;
        this.cells = was.cells;
        this.fixed = was.fixed;
        this.toughness = was.toughness;
        this.drawFrom = was.drawFrom;
        this.whole = whole;
        this.gone = gone;
    }

    /** Its parts put where its building stands, {@code scale}d: as broken as it is. */
    public Assembly placed(final Place at, final double scale) {
        final List<Volume> moved = parts.stream().map(v -> Volume.placed(v, at, scale)).toList();
        final Assembly placed = new Assembly(moved.subList(0, fixed), moved.subList(fixed, moved.size()), toughness,
                drawFrom);
        return new Assembly(placed, whole.clone(), gone.clone());
    }

    /**
     * The same, answering only for its parts that reach between {@code from} and {@code to} up (a band of its storeys,
     * the slab under the lowest among them): the rest in no one's way, as if not there. Its state, as broken as it is,
     * shared; {@link #unbanded} all of it again.
     */
    public Assembly banded(final double from, final double to) {
        return from == bandFrom && to == bandTo ? this : new Assembly(this, whole, gone, from, to);
    }

    /** The same, answering for all its parts. */
    public Assembly unbanded() {
        return banded(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Whether part {@code p} reaches into the band it answers for, a centimetre either way. */
    private boolean inBand(final int p) {
        return box[6 * p + 5] >= bandFrom - 1 && box[6 * p + 4] <= bandTo + 1;
    }

    /** How many of its parts break, each on its own. */
    public int breakable() {
        return whole.length;
    }

    /** How much breakable part {@code b} can take before it breaks. */
    public double toughness() {
        return toughness;
    }

    /** The key breakable part {@code b} is drawn by: one of its own, from this on. */
    public int drawFrom() {
        return drawFrom;
    }

    /** How much of breakable part {@code b} is whole: 1 unharmed, 0 broken. */
    public double whole(final int b) {
        return whole[b];
    }

    /** Whether breakable part {@code b} is broken, and gone. */
    public boolean broken(final int b) {
        return whole[b] <= 0;
    }

    /** The same, breakable part {@code b} struck, {@code share} of it worn away; worn away entirely, gone. */
    public Assembly struck(final int b, final double share) {
        final double[] left = whole.clone();
        // worn to within a hair of nothing, broken: shares of a whole, added up, may not come to it exactly
        left[b] = left[b] - share <= 1e-9 ? 0 : left[b] - share;
        boolean[] without = gone;
        if (left[b] <= 0) {
            without = gone.clone();
            without[fixed + b] = true;
        }
        return new Assembly(this, left, without);
    }

    /**
     * The breakable part a point in the world is on, or in (a round's, a blade's, where it met it); -1 if it is on
     * none, or on a broken one.
     */
    public int breakableAt(final Vec3 at) {
        final double h = -at.y();
        for (final int p : near(at.x() - 1, at.x() + 1, at.z() - 1, at.z() + 1, h - 1, h + 1)) {
            if (p < fixed || gone[p] || !inBand(p)) {
                continue;
            }
            final int k = 6 * p;
            if (at.x() >= box[k] - 1 && at.x() <= box[k + 1] + 1 && at.z() >= box[k + 2] - 1 && at.z() <= box[k + 3] + 1
                    && h >= box[k + 4] - 1 && h <= box[k + 5] + 1) {
                return p - fixed;
            }
        }
        return -1;
    }

    private int cell(final int i, final int k, final int j) {
        return (j * nz + k) * nx + i;
    }

    /** The cells a box reaches, as ranges: least and most across, along, up; clamped to its own. */
    private int[] range(final double minX, final double maxX, final double minZ, final double maxZ, final double lo,
                        final double hi) {
        return new int[] {
            clamp((minX - bounds.minX()) / CELL, nx), clamp((maxX - bounds.minX()) / CELL, nx),
            clamp((minZ - bounds.minZ()) / CELL, nz), clamp((maxZ - bounds.minZ()) / CELL, nz),
            clamp((lo - low) / CELL_UP, ny), clamp((hi - low) / CELL_UP, ny)};
    }

    private static int clamp(final double at, final int count) {
        return Math.max(0, Math.min(count - 1, (int) Math.floor(at)));
    }

    /** The parts filed in the cells a box reaches, each once, in order: none if it is nowhere near. */
    int[] near(final double minX, final double maxX, final double minZ, final double maxZ, final double lo,
               final double hi) {
        if (maxX < bounds.minX() || minX > bounds.maxX() || maxZ < bounds.minZ() || minZ > bounds.maxZ() || hi < low
                || lo > high) {
            return new int[0];
        }
        final int[] r = range(minX, maxX, minZ, maxZ, lo, hi);
        int n = 0;
        int[] found = new int[64];
        for (int j = r[4]; j <= r[5]; j++) {
            for (int k = r[2]; k <= r[3]; k++) {
                for (int i = r[0]; i <= r[1]; i++) {
                    final int[] in = cells[cell(i, k, j)];
                    if (n + in.length > found.length) {
                        found = Arrays.copyOf(found, Math.max(2 * found.length, n + in.length));
                    }
                    System.arraycopy(in, 0, found, n, in.length);
                    n += in.length;
                }
            }
        }
        Arrays.sort(found, 0, n);
        int kept = 0;
        for (int i = 0; i < n; i++) {
            if (kept == 0 || found[i] != found[kept - 1]) {
                found[kept++] = found[i];
            }
        }
        return Arrays.copyOf(found, kept);
    }

    /** Whether part {@code p} is there to answer, and its box from above comes within {@code radius} of a point. */
    private boolean answers(final int p, final double x, final double z, final double radius) {
        final int k = 6 * p;
        return !gone[p] && inBand(p) && x + radius >= box[k] && x - radius <= box[k + 1]
                && z + radius >= box[k + 2] && z - radius <= box[k + 3];
    }

    @Override
    public Bounds bounds() {
        return bounds;
    }

    @Override
    public double low() {
        return low;
    }

    @Override
    public double high() {
        return high;
    }

    @Override
    public boolean covers(final double x, final double z, final double radius) {
        for (final int p : near(x - radius, x + radius, z - radius, z + radius, low, high)) {
            if (answers(p, x, z, radius) && parts.get(p).covers(x, z, radius)) {
                return true;
            }
        }
        return false;
    }

    /** Steep where the part whose top it is is steep there. */
    @Override
    public boolean steep(final double x, final double z, final double radius, final double top) {
        for (final int p : near(x - radius, x + radius, z - radius, z + radius, top - 1, top + 1)) {
            if (answers(p, x, z, radius) && parts.get(p).topAt(x, z, radius, top + 1) == top
                    && parts.get(p).steep(x, z, radius, top)) {
                return true;
            }
        }
        return false;
    }

    /** Looking down a band at a time from {@code below}: the first band with a top in it has the highest. */
    @Override
    public double topAt(final double x, final double z, final double radius, final double below) {
        double best = Double.NEGATIVE_INFINITY;
        for (double to = Math.min(below, high); to >= low; to -= BAND) {
            final double from = to - BAND;
            for (final int p : near(x - radius, x + radius, z - radius, z + radius, from, to)) {
                if (answers(p, x, z, radius)) {
                    best = Math.max(best, parts.get(p).topAt(x, z, radius, below));
                }
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
        for (double from = Math.max(above, low); from <= high; from += BAND) {
            final double to = from + BAND;
            for (final int p : near(x - radius, x + radius, z - radius, z + radius, from, to)) {
                if (answers(p, x, z, radius)) {
                    best = Math.min(best, parts.get(p).undersideAt(x, z, radius, above));
                }
            }
            if (best <= to) {
                break;
            }
        }
        return best;
    }

    @Override
    public Vec2 push(final double x, final double z, final double radius, final double feet, final double head) {
        return push(x, z, x, z, radius, feet, head);
    }

    /** Clear of every part near it, between its feet and its head: each pushed clear of in turn, twice, for corners. */
    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        if (head <= feet || head <= low || feet >= high) {
            return null;
        }
        final boolean point = ax == bx && az == bz;
        final double reach = 3 * radius;
        final int[] near = near(Math.min(ax, bx) - reach, Math.max(ax, bx) + reach, Math.min(az, bz) - reach,
                Math.max(az, bz) + reach, feet, head);
        double dx = 0;
        double dz = 0;
        boolean pushed = false;
        for (int pass = 0; pass < 2; pass++) {
            for (final int p : near) {
                if (gone[p] || !inBand(p)) {
                    continue;
                }
                final Volume part = parts.get(p);
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
        final double[] at = {from.x(), from.z(), -from.y()};
        final double[] go = {direction.x(), direction.z(), -direction.y()};
        final double[] lo = {bounds.minX(), bounds.minZ(), low};
        final double[] hi = {bounds.maxX(), bounds.maxZ(), high};
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
            for (final int p : near(Math.min(a.x(), b.x()) - 1, Math.max(a.x(), b.x()) + 1, Math.min(a.z(), b.z()) - 1,
                    Math.max(a.z(), b.z()) + 1, Math.min(-a.y(), -b.y()) - 1, Math.max(-a.y(), -b.y()) + 1)) {
                if (tried.get(p) || gone[p] || !inBand(p)) {
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
        final double reach = block.halfWidth() + block.halfDepth();
        // a block's heights as it gives them, either way up (a wheel's, under its axle)
        final double lo = Math.min(block.bottom(), block.highest());
        final double hi = Math.max(block.bottom(), block.highest());
        for (final int p : near(block.x() - reach, block.x() + reach, block.z() - reach, block.z() + reach, lo, hi)) {
            if (!gone[p] && inBand(p) && parts.get(p).overlaps(block)) {
                return true;
            }
        }
        return false;
    }
}
