package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * A road laid on the land ({@link Land.Layer}): along its middle line, {@code wide} across and level
 * across, as high as the ground under its line, that smoothed along it ({@link #over}), so it climbs and
 * falls gently where the ground is lumpy, cut into a rise and built up over a dip; its verges easing
 * from it into the ground beside it over {@code verge} each side. Its top paved ({@link #paved}): gravel,
 * a country road's, its edges thinning into the grass.
 */
public final class Road implements Land.Layer {

    /** How far either side of its edges its gravel thins out into the grass. */
    static final double EDGE = 75;
    /** How many times its line's heights are smoothed, each over {@code smoothing} each way. */
    private static final int PASSES = 3;

    private final Polyline line;
    private final double half;
    private final double verge;
    /** How high it is at each point of its line. */
    private final double[] levels;
    private final double deepest;
    private final double tallest;

    private Road(final Polyline line, final double half, final double verge, final double[] levels, final double deepest,
                 final double tallest) {
        this.line = line;
        this.half = half;
        this.verge = verge;
        this.levels = levels;
        this.deepest = deepest;
        this.tallest = tallest;
    }

    /**
     * A road along {@code line} over {@code under}, {@code wide}, its verges {@code verge}: its line's
     * heights the ground's under it, each the mean of those within {@code smoothing} of it along it, a
     * few times over; its two ends as high as the ground there, so it meets what it comes from and goes to.
     */
    public static Road over(final Land under, final List<Vec2> line, final double wide, final double verge,
                            final double smoothing) {
        if (!(wide > 0) || !(verge > 0) || !(smoothing >= 0)) {
            throw new IllegalArgumentException("a road is some width, its verges some way");
        }
        final Polyline path = new Polyline(line, wide / 2 + verge);
        final int n = line.size();
        final double[] along = new double[n];
        final double[] ground = new double[n];
        for (int k = 0; k < n; k++) {
            if (k > 0) {
                along[k] = along[k - 1] + Math.hypot(line.get(k).x() - line.get(k - 1).x(), line.get(k).z() - line.get(k - 1).z());
            }
            ground[k] = under.height(line.get(k).x(), line.get(k).z());
        }
        double[] levels = ground.clone();
        for (int pass = 0; pass < PASSES; pass++) {
            final double[] was = levels;
            final double[] now = new double[n];
            int from = 0;
            int to = 0;
            double sum = 0;
            for (int k = 0; k < n; k++) {
                while (to < n && along[to] <= along[k] + smoothing) {
                    sum += was[to++];
                }
                while (along[from] < along[k] - smoothing) {
                    sum -= was[from++];
                }
                now[k] = sum / (to - from);
            }
            levels = now;
        }
        // its ends brought back to the ground there, easing in over the smoothing's length, not with a step
        final double first = ground[0] - levels[0];
        final double last = ground[n - 1] - levels[n - 1];
        final double total = along[n - 1];
        final double ease = Math.max(1, Math.min(2 * smoothing, total / 2));
        for (int k = 0; k < n; k++) {
            levels[k] += first * eased(1 - along[k] / ease) + last * eased(1 - (total - along[k]) / ease);
        }
        double deepest = 0;
        double tallest = 0;
        for (int k = 0; k < n; k++) {
            deepest = Math.max(deepest, ground[k] - levels[k]);
            tallest = Math.max(tallest, levels[k] - ground[k]);
        }
        // across a bend, the ground beside the line differs from under it: some room for that
        return new Road(path, wide / 2, verge, levels, deepest + verge, tallest + verge);
    }

    private static double eased(final double t) {
        return t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t);
    }

    public List<Vec2> line() {
        return line.points();
    }

    /** How steep it is at most along it, rise over run, between any two of its line's points. */
    public double steepest() {
        double steepest = 0;
        final List<Vec2> points = line.points();
        for (int k = 1; k < points.size(); k++) {
            final double run = Math.hypot(points.get(k).x() - points.get(k - 1).x(), points.get(k).z() - points.get(k - 1).z());
            if (run > 0) {
                steepest = Math.max(steepest, Math.abs(levels[k] - levels[k - 1]) / run);
            }
        }
        return steepest;
    }

    /** How wide it is. */
    public double wide() {
        return 2 * half;
    }

    /** How high it is at its line's point {@code k}. */
    public double level(final int k) {
        return levels[k];
    }

    @Override
    public double shape(final double x, final double z, final double under) {
        final Polyline.Near near = line.nearest(x, z);
        if (near == null) {
            return under;
        }
        final double level = levels[near.segment()] + (levels[near.segment() + 1] - levels[near.segment()]) * near.t();
        final double d = near.distance();
        if (d <= half) {
            return level;
        }
        final double t = 1 - (d - half) / verge;
        return under + (level - under) * t * t * (3 - 2 * t);
    }

    @Override
    public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
        return line.touches(minX, minZ, maxX, maxZ);
    }

    @Override
    public double deepest() {
        return deepest;
    }

    @Override
    public double tallest() {
        return tallest;
    }

    /** Its top, gravel: all of it across, thinning out into the grass over {@link #EDGE} either side of its edges. */
    @Override
    public double paved(final double x, final double z) {
        final double t = (half + EDGE - line.distance(x, z)) / (2 * EDGE);
        return t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t);
    }
}
