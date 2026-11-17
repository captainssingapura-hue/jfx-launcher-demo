package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A piece of the {@link Land} as triangles, as fine as it needs to be and no finer: a square of it split
 * in four, and each of those again, only where its corners alone would put the land more than so far
 * ({@code tolerance}) from where it is, or where a feature of it lies ({@link Land#featured}) and the
 * square is bigger than so much; never smaller than so much ({@code finest}), never bigger than so much
 * ({@code largest}). An open meadow comes out in big squares, a river's bank in small ones.
 *
 * <p>No square is left more than twice the size of one beside it, and where one is, its side is broken
 * where the smaller ones' corners are, so no crack shows between them: a square with no smaller ones
 * beside it is two triangles, one with is a fan of them round its middle. A skirt hangs from the piece's
 * edges, to meet whatever piece is drawn beside it, finer or coarser.
 *
 * <p>Pure geometry: its points (the world's y down), the way the ground faces at each, its height, and
 * its triangles, each three of its points' numbers, wound so its face is up.
 */
final class LandSheet {

    /** What a piece comes to: its points, x, y and z; the way up at each; its height (the skirt's, its top's); its triangles. */
    record Made(float[] points, float[] normals, double[] heights, int[] triangles) {

        int size() {
            return heights.length;
        }
    }

    /** How far either side the way the ground faces is looked for: near a feature, and elsewhere. */
    static final double NEAR = 25;

    private final Land land;
    private final double x0;
    private final double z0;
    private final double wide;
    private final double deep;
    private final double tolerance;
    private final double finest;
    private final double largest;
    private final double featured;
    private final double far;
    /** Each square there is, by its key: whether it is split. */
    private final Map<Long, Boolean> squares = new HashMap<>();
    private final Map<Long, Integer> numbered = new HashMap<>();
    private final List<double[]> at = new ArrayList<>();

    private LandSheet(final Land land, final double x0, final double z0, final double x1, final double z1,
                      final double tolerance, final double finest, final double largest, final double featured,
                      final double far) {
        this.land = land;
        this.x0 = x0;
        this.z0 = z0;
        this.wide = x1 - x0;
        this.deep = z1 - z0;
        this.tolerance = tolerance;
        this.finest = finest;
        this.largest = largest;
        this.featured = featured;
        this.far = far;
    }

    /**
     * The land from ({@code x0}, {@code z0}) to ({@code x1}, {@code z1}): no more than {@code tolerance} out,
     * squares no smaller than {@code finest} and no bigger than {@code largest}, nor than {@code featured} where
     * a feature lies; the way it faces looked for {@code far} either side away from features; its skirt
     * hanging {@code skirt}.
     */
    static Made of(final Land land, final double x0, final double z0, final double x1, final double z1,
                   final double tolerance, final double finest, final double largest, final double featured,
                   final double far, final double skirt) {
        return new LandSheet(land, x0, z0, x1, z1, tolerance, finest, largest, featured, far).made(skirt);
    }

    private static long key(final int depth, final long ix, final long iz) {
        return (long) depth << 56 | ix << 28 | iz;
    }

    private double w(final int depth) {
        return wide / (1L << depth);
    }

    private double d(final int depth) {
        return deep / (1L << depth);
    }

    private Made made(final double skirt) {
        // split as the land asks
        final Deque<long[]> todo = new ArrayDeque<>();
        todo.add(new long[] {0, 0, 0});
        while (!todo.isEmpty()) {
            final long[] s = todo.poll();
            final int depth = (int) s[0];
            final boolean split = splits(depth, s[1], s[2]);
            squares.put(key(depth, s[1], s[2]), split);
            if (split) {
                for (int a = 0; a < 2; a++) {
                    for (int b = 0; b < 2; b++) {
                        todo.add(new long[] {depth + 1, 2 * s[1] + a, 2 * s[2] + b});
                    }
                }
            }
        }
        // and no square more than twice the size of one beside it
        boolean changed = true;
        while (changed) {
            changed = false;
            for (final Map.Entry<Long, Boolean> square : new ArrayList<>(squares.entrySet())) {
                if (square.getValue()) {
                    continue;
                }
                final long k = square.getKey();
                final int depth = (int) (k >>> 56);
                final long ix = k >>> 28 & 0xfff_ffffL;
                final long iz = k & 0xfff_ffffL;
                if (crowded(depth, ix, iz)) {
                    squares.put(k, true);
                    for (int a = 0; a < 2; a++) {
                        for (int b = 0; b < 2; b++) {
                            squares.put(key(depth + 1, 2 * ix + a, 2 * iz + b), false);
                        }
                    }
                    changed = true;
                }
            }
        }
        // each square left whole, as triangles
        final List<Integer> faces = new ArrayList<>();
        for (final Map.Entry<Long, Boolean> square : squares.entrySet()) {
            if (!square.getValue()) {
                final long k = square.getKey();
                triangles(faces, (int) (k >>> 56), k >>> 28 & 0xfff_ffffL, k & 0xfff_ffffL);
            }
        }
        // the skirt: round the edges, each point of them again, hanging
        final List<List<Integer>> edges = List.of(edge(p -> p[2] == z0, 0), edge(p -> p[2] == z0 + deep, 0),
                edge(p -> p[0] == x0, 2), edge(p -> p[0] == x0 + wide, 2));
        final List<int[]> hung = new ArrayList<>();
        for (final List<Integer> edge : edges) {
            final int[] down = new int[edge.size()];
            for (int k = 0; k < edge.size(); k++) {
                final double[] p = at.get(edge.get(k));
                down[k] = at.size();
                at.add(new double[] {p[0], p[1] - skirt, p[2], p[1]});
            }
            hung.add(down);
        }
        for (int e = 0; e < edges.size(); e++) {
            final List<Integer> edge = edges.get(e);
            final int[] down = hung.get(e);
            for (int k = 0; k + 1 < edge.size(); k++) {
                add(faces, edge.get(k), edge.get(k + 1), down[k]);
                add(faces, edge.get(k + 1), down[k + 1], down[k]);
            }
        }
        final int n = at.size();
        final float[] points = new float[3 * n];
        final float[] normals = new float[3 * n];
        final double[] heights = new double[n];
        for (int v = 0; v < n; v++) {
            final double[] p = at.get(v);
            points[3 * v] = (float) p[0];
            points[3 * v + 1] = (float) -p[1];
            points[3 * v + 2] = (float) p[2];
            // a skirt's point as the point it hangs from: as high, for its tint, and facing as it does
            heights[v] = p[3];
            System.arraycopy(facing(p[0], p[2]), 0, normals, 3 * v, 3);
        }
        return new Made(points, normals, heights, faces.stream().mapToInt(Integer::intValue).toArray());
    }

    /** Whether square ({@code ix}, {@code iz}) {@code depth} down is to be split. */
    private boolean splits(final int depth, final long ix, final long iz) {
        final double w = w(depth);
        final double d = d(depth);
        final double size = Math.max(w, d);
        if (size <= finest) {
            return false;
        }
        if (size > largest) {
            return true;
        }
        final double ax = x0 + ix * w;
        final double az = z0 + iz * d;
        final boolean feature = land.featured(ax, az, ax + w, az + d);
        if (size > featured && feature) {
            return true;
        }
        // where what covers the ground changes across it (a road's edge, a bank's top), finer: its colour is
        // worked out at its corners, and read between them
        if (feature && covered(ax, az, w, d)) {
            return true;
        }
        // how far the land is from what its corners alone make of it, looked at across it
        final double h00 = land.height(ax, az);
        final double h10 = land.height(ax + w, az);
        final double h01 = land.height(ax, az + d);
        final double h11 = land.height(ax + w, az + d);
        final int looks = 4;
        for (int a = 0; a <= looks; a++) {
            for (int b = 0; b <= looks; b++) {
                final double s = (double) a / looks;
                final double t = (double) b / looks;
                final double made = (h00 * (1 - s) + h10 * s) * (1 - t) + (h01 * (1 - s) + h11 * s) * t;
                if (Math.abs(land.height(ax + s * w, az + t * d) - made) > tolerance) {
                    return true;
                }
            }
        }
        return false;
    }

    /** How far what covers the ground may be from what a square's corners make of it, and it still be left whole. */
    static final double COVER = 0.08;

    /** Whether how bare or paved the ground is changes across a square more than its corners alone would have it. */
    private boolean covered(final double ax, final double az, final double w, final double d) {
        final double[][] corners = new double[4][];
        for (int k = 0; k < 4; k++) {
            final double x = ax + (k & 1) * w;
            final double z = az + (k >> 1) * d;
            corners[k] = new double[] {land.bare(x, z), land.paved(x, z)};
        }
        final int looks = 4;
        for (int a = 0; a <= looks; a++) {
            for (int b = 0; b <= looks; b++) {
                final double s = (double) a / looks;
                final double t = (double) b / looks;
                final double x = ax + s * w;
                final double z = az + t * d;
                final double[] here = {land.bare(x, z), land.paved(x, z)};
                for (int c = 0; c < 2; c++) {
                    final double made = (corners[0][c] * (1 - s) + corners[1][c] * s) * (1 - t)
                            + (corners[2][c] * (1 - s) + corners[3][c] * s) * t;
                    if (Math.abs(here[c] - made) > COVER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Whether a square left whole has, beside it, one split whose halves next to it are split too. */
    private boolean crowded(final int depth, final long ix, final long iz) {
        // west, east, south, north
        final long[][] sides = {{ix - 1, iz}, {ix + 1, iz}, {ix, iz - 1}, {ix, iz + 1}};
        for (int s = 0; s < 4; s++) {
            final long nx = sides[s][0];
            final long nz = sides[s][1];
            if (!Boolean.TRUE.equals(squares.get(key(depth, nx, nz)))) {
                continue;
            }
            // its two halves along the side it shares
            for (int k = 0; k < 2; k++) {
                final long cx = s < 2 ? 2 * nx + (s == 0 ? 1 : 0) : 2 * nx + k;
                final long cz = s < 2 ? 2 * nz + k : 2 * nz + (s == 2 ? 1 : 0);
                if (Boolean.TRUE.equals(squares.get(key(depth + 1, cx, cz)))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Square ({@code ix}, {@code iz}) {@code depth} down as triangles: two, or a fan round its middle where smaller ones are beside it. */
    private void triangles(final List<Integer> faces, final int depth, final long ix, final long iz) {
        final double w = w(depth);
        final double d = d(depth);
        final double ax = x0 + ix * w;
        final double az = z0 + iz * d;
        final double bx = x0 + (ix + 1) * w;
        final double bz = z0 + (iz + 1) * d;
        final boolean south = Boolean.TRUE.equals(squares.get(key(depth, ix, iz - 1)));
        final boolean east = Boolean.TRUE.equals(squares.get(key(depth, ix + 1, iz)));
        final boolean north = Boolean.TRUE.equals(squares.get(key(depth, ix, iz + 1)));
        final boolean west = Boolean.TRUE.equals(squares.get(key(depth, ix - 1, iz)));
        final int a = point(ax, az);
        final int b = point(bx, az);
        final int c = point(bx, bz);
        final int e = point(ax, bz);
        if (!south && !east && !north && !west) {
            // wound so its face is up, the world's y down
            add(faces, a, b, e);
            add(faces, b, c, e);
            return;
        }
        // round it the same way: corner, the middle of its side if smaller ones are beside it, corner
        final List<Integer> round = new ArrayList<>();
        round.add(a);
        if (south) {
            round.add(point((ax + bx) / 2, az));
        }
        round.add(b);
        if (east) {
            round.add(point(bx, (az + bz) / 2));
        }
        round.add(c);
        if (north) {
            round.add(point((ax + bx) / 2, bz));
        }
        round.add(e);
        if (west) {
            round.add(point(ax, (az + bz) / 2));
        }
        final int middle = point((ax + bx) / 2, (az + bz) / 2);
        for (int k = 0; k < round.size(); k++) {
            add(faces, middle, round.get(k), round.get((k + 1) % round.size()));
        }
    }

    /** The point at ({@code x}, {@code z}), on the land: its number, the same each time it is asked for. */
    private int point(final double x, final double z) {
        final long k = Math.round(x * 4) * 0x1_0000_0000L + (Math.round(z * 4) & 0xffff_ffffL);
        return numbered.computeIfAbsent(k, n -> {
            final double h = land.height(x, z);
            at.add(new double[] {x, h, z, h});
            return at.size() - 1;
        });
    }

    /** Its points along one edge, in order along it ({@code along} 0 for x, 2 for z). */
    private List<Integer> edge(final java.util.function.Predicate<double[]> on, final int along) {
        final List<Integer> edge = new ArrayList<>();
        for (int v = 0; v < at.size(); v++) {
            if (on.test(at.get(v))) {
                edge.add(v);
            }
        }
        edge.sort(Comparator.comparingDouble(v -> at.get(v)[along]));
        return edge;
    }

    private static void add(final List<Integer> faces, final int a, final int b, final int c) {
        faces.add(a);
        faces.add(b);
        faces.add(c);
    }

    /** The way up from the land at ({@code x}, {@code z}), the world's y down: looked for nearer by, near a feature. */
    private float[] facing(final double x, final double z) {
        final double apart = land.featured(x - far, z - far, x + far, z + far) ? NEAR : far;
        final Vec2 rise = land.rise(x, z, apart);
        final double length = Math.sqrt(rise.x() * rise.x() + 1 + rise.z() * rise.z());
        return new float[] {(float) (-rise.x() / length), (float) (-1 / length), (float) (-rise.z() / length)};
    }
}
