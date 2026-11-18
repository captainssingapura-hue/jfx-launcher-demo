package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Sites;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Box;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.GridInside;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Lean;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Placed;
import com.example.fxsuite.avatar.sim.Road;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * The bridges on roads ({@link Road}), each built on a stretch of its road that is a bridge, worked out from the road
 * as it is laid there: its line, its width, its heights. A deck, its top the road's surface at the road's own level, a
 * concrete slab under it, a little past each end onto the road; a parapet along each side; and, wherever the ground or
 * the bed under it is low enough to need them, piers down to it, a pair of columns under a crosshead every so far
 * along. Each bridge one piece in the world, in the way through its own inside ({@link Interior}), and drawn by a key
 * of its own, registered with the buildings' ({@link Sites#register}).
 */
public final class Bridges {

    /** How deep a deck is, under its surface; how thick its surface; how far past each end of its stretch it goes. */
    static final double DECK = 100;
    static final double SURFACE = 5;
    static final double PAST = 100;
    /** A parapet: how high over the deck, how thick. */
    static final double PARAPET = 100;
    static final double PARAPET_THICK = 30;
    /** Its piers: how far apart along it; how thick a column; how deep a crosshead; how much clear under it needs one. */
    static final double SPAN = 3_000;
    static final double COLUMN = 120;
    static final double CROSSHEAD = 120;
    static final double CLEAR = 200;
    /** How far into the ground or the bed a pier goes. */
    static final double FOOTING = 50;
    /** How big the cells of a bridge's inside are, each way over the ground. */
    static final double CELL = 1_000;

    private Bridges() {
    }

    /** A bridge: the road it carries, and its stretch of it; all it is made of, in the world; its parts in the way. */
    public record Bridge(String road, int from, int to, List<Solid> solids, List<Lean> parts, Box bounds) {

        public Bridge {
            solids = List.copyOf(solids);
            parts = List.copyOf(parts);
        }
    }

    /**
     * The bridges on {@code roads}, as laid on {@code land} (what is under them, where their piers go down to): one
     * on each stretch of each road that is a bridge.
     */
    public static List<Bridge> of(final List<Road> roads, final Land land) {
        final List<Bridge> bridges = new ArrayList<>();
        for (final Road road : roads) {
            final int n = road.line().size();
            int i = 0;
            while (i < n) {
                if (!road.bridged(i)) {
                    i++;
                    continue;
                }
                int j = i;
                while (j + 1 < n && road.bridged(j + 1)) {
                    j++;
                }
                // its stretch ends at the point after its last on it
                final int to = Math.min(n - 1, j + 1);
                if (to > i) {
                    bridges.add(built(road, i, to, land));
                }
                i = j + 1;
            }
        }
        return bridges;
    }

    /**
     * The bridges as pieces of the world, each drawn by its key from {@code keys} on: registered, so the buildings'
     * drawing finds them ({@link Sites}).
     */
    public static List<Course.Piece> pieces(final List<Bridge> bridges, final int keys) {
        Sites.register(keys, keys + Math.max(1, bridges.size()), k -> drawn(bridges.get(k)));
        final List<Course.Piece> pieces = new ArrayList<>();
        for (int k = 0; k < bridges.size(); k++) {
            final Bridge b = bridges.get(k);
            final Box box = b.bounds();
            final double cx = (box.minX() + box.maxX()) / 2;
            final double cz = (box.minZ() + box.maxZ()) / 2;
            final Interior inside = new Interior(new Placed(new Place(0, 0, 0, 0), new GridInside(box, CELL, CELL,
                    Math.max(1, box.high() - box.low()))), b.parts());
            final Block block = new Block(cx, cz, (box.maxX() - box.minX()) / 2, (box.maxZ() - box.minZ()) / 2, 0,
                    box.low(), box.high());
            pieces.add(Course.Piece.shaped(block, Course.Kind.BUILDING, keys + k, List.of(), List.of(inside),
                    new Place(cx, cz, box.low(), 0)));
        }
        return pieces;
    }

    /** A bridge's solids about the middle of its box, up from its foot: as a building is drawn. */
    static List<Solid> drawn(final Bridge b) {
        final Box box = b.bounds();
        final double cx = (box.minX() + box.maxX()) / 2;
        final double cz = (box.minZ() + box.maxZ()) / 2;
        final List<Solid> drawn = new ArrayList<>(b.solids().size());
        for (final Solid s : b.solids()) {
            drawn.add(new Solid(s.name(), s.role(), s.material(), s.corners().stream()
                    .map(p -> new Point(p.across() - cx, p.along() - cz, p.up() - box.low())).toList()));
        }
        return drawn;
    }

    /** The bridge on {@code road}'s points {@code from} to {@code to}: its deck, its parapets, its piers. */
    static Bridge built(final Road road, final int from, final int to, final Land land) {
        final int n = to - from + 1;
        final double[] x = new double[n];
        final double[] z = new double[n];
        final double[] top = new double[n];
        final double[] half = new double[n];
        for (int k = 0; k < n; k++) {
            final Vec2 p = road.line().get(from + k);
            x[k] = p.x();
            z[k] = p.z();
            top[k] = road.level(from + k);
            half[k] = road.wide(from + k) / 2;
        }
        // a little past each end, onto the road, going on as it goes there
        extend(x, z, 0, 1, PAST);
        extend(x, z, n - 1, n - 2, PAST);
        // across it at each point: square to the way it goes there, the mean of the ways in and out
        final double[] sx = new double[n];
        final double[] sz = new double[n];
        for (int k = 0; k < n; k++) {
            final int a = Math.max(0, k - 1);
            final int b = Math.min(n - 1, k + 1);
            final double dx = x[b] - x[a];
            final double dz = z[b] - z[a];
            final double len = Math.max(1e-9, Math.hypot(dx, dz));
            // to its right, as a heading's right is: (cos, -sin) of a yaw whose heading is (sin, cos)
            sx[k] = dz / len;
            sz[k] = -dx / len;
        }
        final List<Solid> solids = new ArrayList<>();
        final List<Lean> parts = new ArrayList<>();
        final String name = road.name() + " bridge at " + from;
        double along = 0;
        final double[] at = new double[n];
        for (int k = 0; k + 1 < n; k++) {
            at[k + 1] = at[k] + Math.hypot(x[k + 1] - x[k], z[k + 1] - z[k]);
            final double t0 = top[k];
            final double t1 = top[k + 1];
            final double w0 = half[k];
            final double w1 = half[k + 1];
            final double o0 = w0 + PARAPET_THICK;
            final double o1 = w1 + PARAPET_THICK;
            // its deck: the slab, its surface on it, a parapet each side; each a strip mitred at the road's bends
            solids.add(strip(name + ", deck " + k, Role.STRUCTURE, Material.CONCRETE, x, z, sx, sz, k, -o0, o0, -o1, o1,
                    t0 - DECK, t0 - SURFACE, t1 - DECK, t1 - SURFACE));
            solids.add(strip(name + ", surface " + k, Role.STRUCTURE, Material.ASPHALT, x, z, sx, sz, k, -w0, w0, -w1, w1,
                    t0 - SURFACE, t0, t1 - SURFACE, t1));
            for (final int side : new int[] {-1, 1}) {
                solids.add(strip(name + ", parapet " + k + (side < 0 ? " left" : " right"), Role.WALL, Material.CONCRETE,
                        x, z, sx, sz, k, side < 0 ? -o0 : w0, side < 0 ? -w0 : o0, side < 0 ? -o1 : w1,
                        side < 0 ? -w1 : o1, t0 - SURFACE, t0 + PARAPET, t1 - SURFACE, t1 + PARAPET));
            }
            // in the way: a box along the stretch for its deck, and one each side for its parapets, a little longer,
            // so where the road bends there is no gap between one and the next
            final double mx = (x[k] + x[k + 1]) / 2;
            final double mz = (z[k] + z[k + 1]) / 2;
            final double len = Math.hypot(x[k + 1] - x[k], z[k + 1] - z[k]);
            final double yaw = Math.toDegrees(Math.atan2(x[k + 1] - x[k], z[k + 1] - z[k]));
            final double w = Math.max(w0, w1);
            final double t = (t0 + t1) / 2;
            final double reach = len / 2 + COLUMN / 2;
            parts.add(new Lean(mx, mz, yaw, -reach, reach, -w, w, -w, w, t - DECK, t));
            parts.add(new Lean(mx, mz, yaw, -reach, reach, -w - PARAPET_THICK, -w, -w - PARAPET_THICK, -w, t - DECK,
                    t + PARAPET));
            parts.add(new Lean(mx, mz, yaw, -reach, reach, w, w + PARAPET_THICK, w, w + PARAPET_THICK, t - DECK,
                    t + PARAPET));
            along = at[k + 1];
        }
        // its piers: every so far along, where what is under it is low enough to need one
        int pier = 0;
        for (double s = SPAN / 2; s < along - SPAN / 4; s += SPAN) {
            int k = 0;
            while (k + 2 < n && at[k + 1] < s) {
                k++;
            }
            final double f = (s - at[k]) / Math.max(1e-9, at[k + 1] - at[k]);
            final double px = x[k] + (x[k + 1] - x[k]) * f;
            final double pz = z[k] + (z[k + 1] - z[k]) * f;
            final double under = top[k] + (top[k + 1] - top[k]) * f - DECK;
            final double ground = land.height(px, pz);
            if (under - ground < CLEAR) {
                continue;
            }
            final double w = half[k] + (half[k + 1] - half[k]) * f;
            final double yaw = Math.toDegrees(Math.atan2(x[k + 1] - x[k], z[k + 1] - z[k]));
            final String p = name + ", pier " + pier++;
            final double cross = w + PARAPET_THICK;
            solids.add(block(p + ", crosshead", Role.STRUCTURE, Material.CONCRETE, px, pz, yaw, -cross, cross,
                    -COLUMN / 2, COLUMN / 2, under - CROSSHEAD, under));
            parts.add(new Lean(px, pz, yaw, -COLUMN / 2, COLUMN / 2, -cross, cross, -cross, cross, under - CROSSHEAD,
                    under));
            final double[] columns = w > 600 ? new double[] {-w * 0.55, w * 0.55} : new double[] {0};
            for (final double c : columns) {
                solids.add(block(p + ", column " + (c < 0 ? "left" : c > 0 ? "right" : "middle"), Role.STRUCTURE,
                        Material.CONCRETE, px, pz, yaw, c - COLUMN / 2, c + COLUMN / 2, -COLUMN / 2, COLUMN / 2,
                        ground - FOOTING, under - CROSSHEAD));
                parts.add(new Lean(px, pz, yaw, -COLUMN / 2, COLUMN / 2, c - COLUMN / 2, c + COLUMN / 2, c - COLUMN / 2,
                        c + COLUMN / 2, ground - FOOTING, under - CROSSHEAD));
            }
        }
        // all of it, square to the world
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (final Solid s : solids) {
            for (final Point p : s.corners()) {
                minX = Math.min(minX, p.across());
                maxX = Math.max(maxX, p.across());
                minZ = Math.min(minZ, p.along());
                maxZ = Math.max(maxZ, p.along());
                low = Math.min(low, p.up());
                high = Math.max(high, p.up());
            }
        }
        for (final Lean l : parts) {
            final Box b = l.placed();
            minX = Math.min(minX, b.minX());
            maxX = Math.max(maxX, b.maxX());
            minZ = Math.min(minZ, b.minZ());
            maxZ = Math.max(maxZ, b.maxZ());
        }
        return new Bridge(road.name(), from, to, solids, parts, new Box(minX, maxX, minZ, maxZ, low, high));
    }

    /** Point {@code k} moved on {@code by} past itself, away from point {@code from}. */
    private static void extend(final double[] x, final double[] z, final int k, final int from, final double by) {
        final double dx = x[k] - x[from];
        final double dz = z[k] - z[from];
        final double len = Math.hypot(dx, dz);
        if (len > 1e-9) {
            x[k] += dx / len * by;
            z[k] += dz / len * by;
        }
    }

    /**
     * A strip between points {@code k} and {@code k + 1}, from {@code a0} to {@code b0} across at the one (to its right,
     * along its across there) and {@code a1} to {@code b1} at the other; from {@code low0} to {@code high0} up at the
     * one, {@code low1} to {@code high1} at the other.
     */
    private static Solid strip(final String name, final Role role, final Material material, final double[] x,
                               final double[] z, final double[] sx, final double[] sz, final int k, final double a0,
                               final double b0, final double a1, final double b1, final double low0, final double high0,
                               final double low1, final double high1) {
        final List<Point> c = new ArrayList<>(8);
        for (int bit = 0; bit < 8; bit++) {
            final int p = (bit & 2) == 0 ? k : k + 1;
            final double across = (bit & 1) == 0 ? (p == k ? a0 : a1) : (p == k ? b0 : b1);
            final double up = (bit & 4) == 0 ? (p == k ? low0 : low1) : (p == k ? high0 : high1);
            c.add(new Point(x[p] + sx[p] * across, z[p] + sz[p] * across, up));
        }
        return new Solid(name, role, material, c);
    }

    /** A box turned {@code yaw}, about ({@code x}, {@code z}): across {@code a0} to {@code a1}, along {@code b0} to {@code b1}. */
    private static Solid block(final String name, final Role role, final Material material, final double x,
                               final double z, final double yaw, final double a0, final double a1, final double b0,
                               final double b1, final double low, final double high) {
        final double r = Math.toRadians(yaw);
        // across: to its right (cos, -sin); along: its heading (sin, cos)
        final double rx = Math.cos(r);
        final double rz = -Math.sin(r);
        final double hx = Math.sin(r);
        final double hz = Math.cos(r);
        final List<Point> c = new ArrayList<>(8);
        for (int bit = 0; bit < 8; bit++) {
            final double a = (bit & 1) == 0 ? a0 : a1;
            final double b = (bit & 2) == 0 ? b0 : b1;
            final double up = (bit & 4) == 0 ? low : high;
            c.add(new Point(x + rx * a + hx * b, z + rz * a + hz * b, up));
        }
        return new Solid(name, role, material, c);
    }
}
