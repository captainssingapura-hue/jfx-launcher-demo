package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * A {@link Terrain} worked out into heights on a grid, {@link Terrain#every} apart from edge to edge,
 * and read between them smoothly: what the land is drawn as and, in time, walked on.
 *
 * <p>Worked out in three goes:
 * <ol>
 *   <li><b>Between the loops.</b> Each point lies in the ground between the loop round it (or the
 *       world's edge, at the base height) and the loops and spot heights directly inside that. Its
 *       height is theirs, each weighed by how near it is (one over the distance): between one loop and
 *       one inside it, the ground goes straight from the one height to the other. Inside a flat loop it
 *       is level. A top with nothing marked inside it rises on, half as far again as it rose to it (a
 *       hollow sinks), to the point inside it farthest from its line.</li>
 *   <li><b>Smoothed.</b> A few passes, each point the mean of those round it, round off the creases
 *       left along the loops; the flat ground and the spot heights stay as they are.</li>
 *   <li><b>Roughened.</b> The terrain's noise, worked from its seed, laid over all but the flat
 *       ground, fading in away from it, and the spot heights, which are as marked.</li>
 * </ol>
 */
public final class Survey {

    /** How many times it is smoothed. */
    static final int PASSES = 2;
    /** Nearer a line or point than this and the ground is at its height. */
    private static final double TOUCH = 1e-6;

    private final Terrain terrain;
    private final int across;
    private final int along;
    private final double x0;
    private final double z0;
    private final double x1;
    private final double z1;
    private final double every;
    private final float[] heights;

    /** The ground between a loop (or the edge) and what is directly inside it. */
    private record Region(Contour round, double height, List<Contour> loops, List<double[]> points) {
    }

    Survey(final Terrain terrain) {
        this.terrain = terrain;
        this.across = terrain.across();
        this.along = terrain.along();
        this.x0 = terrain.x0();
        this.z0 = terrain.z0();
        this.x1 = terrain.x1();
        this.z1 = terrain.z1();
        this.every = terrain.every();
        final List<Contour> contours = terrain.contours();
        final int n = contours.size();
        final int[] parents = terrain.parents();
        final int[] depths = terrain.depths();
        // region 0 is outside every loop; region i + 1 is inside loop i
        final Region[] regions = new Region[n + 1];
        regions[0] = new Region(null, terrain.base(), new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < n; i++) {
            regions[i + 1] = new Region(contours.get(i), contours.get(i).height(), new ArrayList<>(), new ArrayList<>());
        }
        for (int i = 0; i < n; i++) {
            regions[parents[i] + 1].loops().add(contours.get(i));
        }
        for (final Terrain.Spot spot : terrain.spots()) {
            regions[terrain.within(spot.at().x(), spot.at().z()) + 1].points()
                    .add(new double[] {spot.at().x(), spot.at().z(), spot.height()});
        }
        for (int i = 0; i < n; i++) {
            final Contour top = contours.get(i);
            final Region inside = regions[i + 1];
            final double rise = top.height() - terrain.heightRound(i);
            if (!top.flat() && inside.loops().isEmpty() && inside.points().isEmpty() && rise != 0) {
                final Vec2 pole = top.pole();
                inside.points().add(new double[] {pole.x(), pole.z(), top.height() + rise / 2});
            }
        }
        final List<Contour> flats = contours.stream().filter(Contour::flat).toList();
        final Terrain.Noise noise = terrain.noise();

        // 1. between the loops, row by row
        final double[] raw = new double[across * along];
        final boolean[] held = new boolean[across * along];
        final double[] rough = new double[across * along];
        IntStream.range(0, along).parallel().forEach(j -> {
            final double z = z(j);
            final double[][] crossings = new double[n][];
            for (int k = 0; k < n; k++) {
                crossings[k] = crossings(contours.get(k).loop(), z);
            }
            final int[] next = new int[n];
            for (int i = 0; i < across; i++) {
                final double x = x(i);
                int innermost = -1;
                for (int k = 0; k < n; k++) {
                    while (next[k] < crossings[k].length && crossings[k][next[k]] < x) {
                        next[k]++;
                    }
                    if ((next[k] & 1) == 1 && (innermost < 0 || depths[k] > depths[innermost])) {
                        innermost = k;
                    }
                }
                final Region region = regions[innermost + 1];
                final int cell = j * across + i;
                held[cell] = region.round() != null && region.round().flat();
                raw[cell] = held[cell] ? region.height() : blend(region, x, z);
                if (noise.any()) {
                    double fade = 1;
                    for (final Contour flat : flats) {
                        fade = Math.min(fade, flat.inside(x, z) ? 0 : flat.distance(x, z) / noise.apart());
                    }
                    rough[cell] = fade * noise(x, z);
                }
            }
        });
        for (final Terrain.Spot spot : terrain.spots()) {
            final int i = (int) Math.round((spot.at().x() - x0) / every);
            final int j = (int) Math.round((spot.at().z() - z0) / every);
            if (i >= 0 && j >= 0 && i < across && j < along) {
                held[j * across + i] = true;
                rough[j * across + i] = 0;
            }
        }

        // 2. smoothed
        double[] from = raw;
        for (int pass = 0; pass < PASSES; pass++) {
            final double[] was = from;
            final double[] to = new double[across * along];
            IntStream.range(0, along).parallel().forEach(j -> {
                for (int i = 0; i < across; i++) {
                    final int cell = j * across + i;
                    if (held[cell]) {
                        to[cell] = was[cell];
                        continue;
                    }
                    double sum = 0;
                    int count = 0;
                    for (int b = Math.max(0, j - 1); b <= Math.min(along - 1, j + 1); b++) {
                        for (int a = Math.max(0, i - 1); a <= Math.min(across - 1, i + 1); a++) {
                            sum += was[b * across + a];
                            count++;
                        }
                    }
                    to[cell] = sum / count;
                }
            });
            from = to;
        }

        // 3. roughened
        heights = new float[across * along];
        for (int cell = 0; cell < heights.length; cell++) {
            heights[cell] = (float) (from[cell] + rough[cell]);
        }
    }

    /** Where loop {@code loop} crosses the line across at {@code z}, from left to right. */
    private static double[] crossings(final List<Vec2> loop, final double z) {
        final double[] xs = new double[loop.size()];
        int count = 0;
        for (int a = 0, b = loop.size() - 1; a < loop.size(); b = a++) {
            final Vec2 p = loop.get(a);
            final Vec2 q = loop.get(b);
            if ((p.z() > z) != (q.z() > z)) {
                xs[count++] = (q.x() - p.x()) * (z - p.z()) / (q.z() - p.z()) + p.x();
            }
        }
        final double[] found = Arrays.copyOf(xs, count);
        Arrays.sort(found);
        return found;
    }

    /** The height at ({@code x}, {@code z}) in {@code region}: what bounds it, each weighed by its nearness. */
    private double blend(final Region region, final double x, final double z) {
        final double out = region.round() == null ? Math.min(Math.min(x - x0, x1 - x), Math.min(z - z0, z1 - z))
                : region.round().distance(x, z);
        if (out < TOUCH) {
            return region.height();
        }
        double weights = 1 / out;
        double sum = region.height() / out;
        for (final Contour loop : region.loops()) {
            final double d = loop.distance(x, z);
            if (d < TOUCH) {
                return loop.height();
            }
            weights += 1 / d;
            sum += loop.height() / d;
        }
        for (final double[] point : region.points()) {
            final double d = Math.hypot(point[0] - x, point[1] - z);
            if (d < TOUCH) {
                return point[2];
            }
            weights += 1 / d;
            sum += point[2] / d;
        }
        return sum / weights;
    }

    /** The terrain's roughness at ({@code x}, {@code z}): bumps on bumps, each octave half the size of the last. */
    private double noise(final double x, final double z) {
        final Terrain.Noise noise = terrain.noise();
        double sum = 0;
        double most = 0;
        double apart = noise.apart();
        double height = 1;
        for (int octave = 0; octave < noise.octaves(); octave++) {
            sum += height * bump(x / apart, z / apart, octave);
            most += height;
            apart /= 2;
            height /= 2;
        }
        return noise.height() * sum / most;
    }

    /** Value noise: -1 to 1 at each whole (u, v), worked from the seed, eased between. */
    private double bump(final double u, final double v, final int octave) {
        final long i = (long) Math.floor(u);
        final long j = (long) Math.floor(v);
        final double s = ease(u - i);
        final double t = ease(v - j);
        final long seed = terrain.seed();
        final double a = Num.hash(seed, octave, i, j);
        final double b = Num.hash(seed, octave, i + 1, j);
        final double c = Num.hash(seed, octave, i, j + 1);
        final double d = Num.hash(seed, octave, i + 1, j + 1);
        return 2 * ((a + (b - a) * s) * (1 - t) + (c + (d - c) * s) * t) - 1;
    }

    private static double ease(final double f) {
        return f * f * (3 - 2 * f);
    }

    // --- reading it ---------------------------------------------------------------------------------------

    public Terrain terrain() {
        return terrain;
    }

    /** How many heights across it, west to east; and along it, south to north. */
    public int across() {
        return across;
    }

    public int along() {
        return along;
    }

    public double every() {
        return every;
    }

    /** Where across the {@code i}th height is. */
    public double x(final int i) {
        return x0 + i * every;
    }

    /** Where along the {@code j}th height is. */
    public double z(final int j) {
        return z0 + j * every;
    }

    /** The height {@code i} across and {@code j} along. */
    public double at(final int i, final int j) {
        return heights[j * across + i];
    }

    /** The ground's height at ({@code x}, {@code z}), read between the heights round it; at the edge, off it. */
    public double height(final double x, final double z) {
        final double u = Num.clamp((x - x0) / every, 0, across - 1);
        final double v = Num.clamp((z - z0) / every, 0, along - 1);
        final int i = Math.min((int) u, across - 2);
        final int j = Math.min((int) v, along - 2);
        final double s = u - i;
        final double t = v - j;
        final double a = at(i, j);
        final double b = at(i + 1, j);
        final double c = at(i, j + 1);
        final double d = at(i + 1, j + 1);
        return (a + (b - a) * s) * (1 - t) + (c + (d - c) * s) * t;
    }

    /** Which way and how fast the ground rises at ({@code x}, {@code z}): rise over run, across and along. */
    public Vec2 rise(final double x, final double z) {
        final double h = every / 2;
        return new Vec2((height(x + h, z) - height(x - h, z)) / every, (height(x, z + h) - height(x, z - h)) / every);
    }

    /** How steep the ground is at ({@code x}, {@code z}), rise over run. */
    public double slope(final double x, final double z) {
        return rise(x, z).magnitude();
    }

    public double lowest() {
        double low = Double.POSITIVE_INFINITY;
        for (final float h : heights) {
            low = Math.min(low, h);
        }
        return low;
    }

    public double highest() {
        double high = Double.NEGATIVE_INFINITY;
        for (final float h : heights) {
            high = Math.max(high, h);
        }
        return high;
    }

    /**
     * Its contour lines at {@code level}, drawn back out of its heights (marching squares): each a line of
     * points, a closed one ending where it began. What a map shows; and, drawn at a loop's height, near
     * the loop it was worked from.
     */
    public List<List<Vec2>> lines(final double level) {
        // each grid side the line crosses is a key: across sides even, along sides odd
        final Map<Long, Vec2> points = new HashMap<>();
        final Map<Long, List<Long>> joins = new HashMap<>();
        for (int j = 0; j + 1 < along; j++) {
            for (int i = 0; i + 1 < across; i++) {
                final double a = at(i, j);
                final double b = at(i + 1, j);
                final double c = at(i + 1, j + 1);
                final double d = at(i, j + 1);
                final boolean ha = a >= level;
                final boolean hb = b >= level;
                final boolean hc = c >= level;
                final boolean hd = d >= level;
                final long bottom = 2L * (j * (long) across + i);
                final long top = 2L * ((j + 1) * (long) across + i);
                final long left = 2L * (j * (long) across + i) + 1;
                final long right = 2L * (j * (long) across + i + 1) + 1;
                final List<Long> sides = new ArrayList<>(4);
                if (ha != hb) {
                    points.putIfAbsent(bottom, new Vec2(x(i) + every * (level - a) / (b - a), z(j)));
                    sides.add(bottom);
                }
                if (hb != hc) {
                    points.putIfAbsent(right, new Vec2(x(i + 1), z(j) + every * (level - b) / (c - b)));
                    sides.add(right);
                }
                if (hd != hc) {
                    points.putIfAbsent(top, new Vec2(x(i) + every * (level - d) / (c - d), z(j + 1)));
                    sides.add(top);
                }
                if (ha != hd) {
                    points.putIfAbsent(left, new Vec2(x(i), z(j) + every * (level - a) / (d - a)));
                    sides.add(left);
                }
                if (sides.size() == 2) {
                    join(joins, sides.get(0), sides.get(1));
                } else if (sides.size() == 4) {
                    // a saddle: which corners the high ground joins, by the middle's height
                    final boolean middle = (a + b + c + d) / 4 >= level;
                    if (middle == ha) {
                        // a and c joined: the line cuts off b and d
                        join(joins, bottom, right);
                        join(joins, top, left);
                    } else {
                        join(joins, left, bottom);
                        join(joins, right, top);
                    }
                }
            }
        }
        final List<List<Vec2>> lines = new ArrayList<>();
        // open lines first, from their ends at the edge; then the closed ones
        for (final boolean ends : new boolean[] {true, false}) {
            for (final Long start : new ArrayList<>(joins.keySet())) {
                final List<Long> from = joins.get(start);
                if (from == null || from.isEmpty() || ends && from.size() != 1) {
                    continue;
                }
                final List<Vec2> line = new ArrayList<>();
                line.add(points.get(start));
                long at = start;
                while (joins.containsKey(at) && !joins.get(at).isEmpty()) {
                    final long to = joins.get(at).removeFirst();
                    joins.get(to).remove(Long.valueOf(at));
                    line.add(points.get(to));
                    at = to;
                }
                lines.add(line);
            }
        }
        return lines;
    }

    private static void join(final Map<Long, List<Long>> joins, final long a, final long b) {
        joins.computeIfAbsent(a, k -> new ArrayList<>(2)).add(b);
        joins.computeIfAbsent(b, k -> new ArrayList<>(2)).add(a);
    }
}
