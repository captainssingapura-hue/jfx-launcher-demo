package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A road worked out from its description ({@link RoadMap.Way}) and laid on the land: a feature of it
 * ({@link Land.Layer}). Along its line ({@link Alignment}), at each point:
 *
 * <ul>
 *   <li><b>how high it is</b>: its heights fitted to the ground under its line as a whole (a long even sweep,
 *       its grade changing over its kind's vertical length, not with every lump), held through its pinned
 *       heights and, where it is a road's own, its ends; then kept within its steepest grade. Over a bridge
 *       the ground under it is not asked: it is carried across;</li>
 *   <li><b>across it</b>: its kind's width and surface, its carriageway falling from its crown by its camber;
 *       each eased from one segment's into the next's over the transition at the vertex between them;</li>
 *   <li><b>beside it</b>: from its edges a side slope down or up to the ground, at its kind's slope, so a
 *       cutting 2 m deep has a slope as wide as a bank 2 m high, and where it lies on the ground, almost none.</li>
 * </ul>
 *
 * <p>Over a bridge it leaves the land as it is: the bridge itself is built on it.
 */
public final class Road implements Land.Layer {

    /** How far either side of its edges its surface thins out into the grass. */
    public static final double EDGE = 75;
    /** The narrowest a side slope is. */
    static final double SLOPE = 50;

    private final String name;
    private final Polyline line;
    private final double[] levels;
    private final double[] halves;
    private final double[] cambers;
    private final double[] slopes;
    private final double[][] covers;
    private final boolean[] bridged;
    private final Alignment alignment;
    private final double deepest;
    private final double tallest;
    private final List<String> problems;

    private Road(final String name, final Polyline line, final double[] levels, final double[] halves, final double[] cambers,
                 final double[] slopes, final double[][] covers, final boolean[] bridged, final Alignment alignment,
                 final double deepest, final double tallest, final List<String> problems) {
        this.name = name;
        this.line = line;
        this.levels = levels;
        this.halves = halves;
        this.cambers = cambers;
        this.slopes = slopes;
        this.covers = covers;
        this.bridged = bridged;
        this.alignment = alignment;
        this.deepest = deepest;
        this.tallest = tallest;
        this.problems = List.copyOf(problems);
    }

    /**
     * {@code way}, its kinds as {@code map} has them, laid on {@code under}: a point every {@code step}; vertex
     * {@code v} pinned as high as {@code pins} says, as well as any of its own.
     */
    public static Road of(final RoadMap map, final RoadMap.Way way, final Land under, final Map<Integer, Double> pins,
                          final double step) {
        final Alignment line = Alignment.of(map, way, step);
        final List<String> problems = new ArrayList<>(line.problems());
        final int n = line.size();
        final List<Vec2> points = line.points();
        // each point's segment's attributes, eased through the transitions at the vertices
        final double[] halves = new double[n];
        final double[] cambers = new double[n];
        final double[] slopes = new double[n];
        final double[] grades = new double[n];
        final double[] verticals = new double[n];
        final double[][] covers = new double[Land.Cover.values().length][n];
        final boolean[] bridged = new boolean[n];
        for (int i = 0; i < n; i++) {
            final int k = line.segment(i);
            final RoadMap.Segment seg = way.segments().get(k);
            final RoadMap.Kind kind = map.kind(seg.kind());
            double[] mixed = attributes(kind);
            // within a transition at either end of its segment, eased toward the next or from the last
            final double s = line.along(i);
            for (final int v : new int[] {k, k + 1}) {
                if (v >= way.vertices().size() && !way.loop()) {
                    continue;
                }
                final int at = v % way.vertices().size();
                final double sv = v == way.vertices().size() ? line.length() : line.vertexAlong(at);
                final double from = sv - line.before(at);
                final double to = sv + line.after(at);
                if (to > from && s >= from && s <= to) {
                    final int inSeg = (at - 1 + way.segments().size()) % way.segments().size();
                    final int outSeg = at % way.segments().size();
                    final double[] a = attributes(map.kind(way.segments().get(inSeg).kind()));
                    final double[] b = attributes(map.kind(way.segments().get(outSeg).kind()));
                    final double t = eased((s - from) / (to - from));
                    mixed = new double[a.length];
                    for (int m = 0; m < a.length; m++) {
                        mixed[m] = a[m] + (b[m] - a[m]) * t;
                    }
                }
            }
            halves[i] = mixed[0] / 2;
            cambers[i] = mixed[1];
            slopes[i] = mixed[2];
            grades[i] = kind.grade();
            verticals[i] = kind.vertical();
            for (int c = 0; c < covers.length; c++) {
                covers[c][i] = mixed[3 + c];
            }
            bridged[i] = seg.bridge();
        }
        // the ground under its line, but over a bridge; its pins
        final double[] ground = new double[n];
        final double[] weight = new double[n];
        final double[] pinned = new double[n];
        java.util.Arrays.fill(pinned, Double.NaN);
        for (int i = 0; i < n; i++) {
            ground[i] = under.height(points.get(i).x(), points.get(i).z());
            weight[i] = bridged[i] ? 0 : 1;
        }
        for (int v = 0; v < way.vertices().size(); v++) {
            final RoadMap.Vertex vertex = way.vertices().get(v);
            final Double pin = pins.containsKey(v) ? pins.get(v) : vertex.pinned() ? vertex.height() : null;
            if (pin != null) {
                pinned[nearest(line, line.vertexAlong(v))] = pin;
            }
        }
        if (!way.loop()) {
            // its ends on the ground they come from and go to, unless pinned
            if (Double.isNaN(pinned[0])) {
                pinned[0] = ground[0];
            }
            if (Double.isNaN(pinned[n - 1])) {
                pinned[n - 1] = ground[n - 1];
            }
        }
        final double[] levels = fitted(line, ground, weight, pinned, verticals);
        within(line, levels, pinned, grades);
        // and how steep it is, now, against how steep it may be
        for (int i = 1; i < n; i++) {
            final double run = line.along(i) - line.along(i - 1);
            if (run > 0 && Math.abs(levels[i] - levels[i - 1]) / run > grades[i] + 1e-6) {
                problems.add("\"" + way.name() + "\": steeper than it may be " + RoadMap.metres(line.along(i)) + " m along,"
                        + " to reach a height pinned on it");
                break;
            }
        }
        double reach = 0;
        double deepest = 0;
        double tallest = 0;
        for (int i = 0; i < n; i++) {
            final double off = ground[i] - levels[i];
            deepest = Math.max(deepest, off);
            tallest = Math.max(tallest, -off);
            reach = Math.max(reach, halves[i] + Math.max(SLOPE, (Math.abs(off) + GROUNDED) * slopes[i]) + EDGE);
        }
        return new Road(way.name(), new Polyline(points, reach), levels, halves, cambers, slopes, covers, bridged, line,
                deepest + GROUNDED, tallest + GROUNDED, problems);
    }

    /** How far the ground beside its line may differ from the ground under it, for how far its slopes may reach. */
    private static final double GROUNDED = 200;

    /** A kind's width, camber, side slope and how much of each cover it lays. */
    private static double[] attributes(final RoadMap.Kind kind) {
        final double[] a = new double[3 + Land.Cover.values().length];
        a[0] = kind.width();
        a[1] = kind.camber();
        a[2] = kind.slope();
        a[3 + kind.surface().ordinal()] = 1;
        return a;
    }

    private static double eased(final double t) {
        return t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t);
    }

    /** Which of the line's points is nearest {@code s} along it. */
    private static int nearest(final Alignment line, final double s) {
        int best = 0;
        for (int i = 0; i < line.size(); i++) {
            if (Math.abs(line.along(i) - s) < Math.abs(line.along(best) - s)) {
                best = i;
            }
        }
        return best;
    }

    /**
     * Heights along the line, as near the ground under it as they can be while changing evenly: the least of
     * how far each is from the ground (as much as it is {@code weight}ed) and how much each differs from the next
     * (the more, the longer the {@code vertical} length), the pinned ones held. Solved at once, along it.
     */
    private static double[] fitted(final Alignment line, final double[] ground, final double[] weight, final double[] pinned,
                                   final double[] vertical) {
        final int n = ground.length;
        final double[] a = new double[n];
        final double[] b = new double[n];
        final double[] c = new double[n];
        final double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(pinned[i])) {
                b[i] = 1;
                d[i] = pinned[i];
                continue;
            }
            final double left = i > 0 ? stiff(vertical[i], line.along(i) - line.along(i - 1)) : 0;
            final double right = i < n - 1 ? stiff(vertical[i], line.along(i + 1) - line.along(i)) : 0;
            a[i] = -left;
            c[i] = -right;
            b[i] = weight[i] + left + right;
            d[i] = weight[i] * ground[i];
            if (b[i] == 0) {
                b[i] = 1;
            }
        }
        // along the line, once each way
        for (int i = 1; i < n; i++) {
            final double m = a[i] / b[i - 1];
            b[i] -= m * c[i - 1];
            d[i] -= m * d[i - 1];
        }
        final double[] h = new double[n];
        h[n - 1] = d[n - 1] / b[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            h[i] = (d[i] - c[i] * h[i + 1]) / b[i];
        }
        return h;
    }

    /** How strongly two heights {@code apart} are held to each other, for a vertical length {@code vertical}. */
    private static double stiff(final double vertical, final double apart) {
        return apart <= 0 ? 0 : (vertical / apart) * (vertical / apart);
    }

    /** {@code levels} kept within each point's steepest grade, up the line and down it, the pinned ones held. */
    private static void within(final Alignment line, final double[] levels, final double[] pinned, final double[] grades) {
        final int n = levels.length;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 1; i < n; i++) {
                if (Double.isNaN(pinned[i])) {
                    final double most = grades[i] * (line.along(i) - line.along(i - 1));
                    levels[i] = Num.clamp(levels[i], levels[i - 1] - most, levels[i - 1] + most);
                }
            }
            for (int i = n - 2; i >= 0; i--) {
                if (Double.isNaN(pinned[i])) {
                    final double most = grades[i + 1] * (line.along(i + 1) - line.along(i));
                    levels[i] = Num.clamp(levels[i], levels[i + 1] - most, levels[i + 1] + most);
                }
            }
        }
    }

    public String name() {
        return name;
    }

    /** Its line, worked out. */
    public Alignment alignment() {
        return alignment;
    }

    /** Its line's points. */
    public List<Vec2> line() {
        return line.points();
    }

    /** How high it is at its line's {@code i}th point. */
    public double level(final int i) {
        return levels[i];
    }

    /** How high its line is nearest ({@code x}, {@code z}); infinity if that is beyond its reach. */
    public double level(final double x, final double z) {
        final Polyline.Near near = line.nearest(x, z);
        return near == null ? Double.POSITIVE_INFINITY
                : levels[near.segment()] + (levels[near.segment() + 1] - levels[near.segment()]) * near.t();
    }

    /** How wide it is at its line's {@code i}th point. */
    public double wide(final int i) {
        return 2 * halves[i];
    }

    /** Whether its line's {@code i}th point is on a bridge. */
    public boolean bridged(final int i) {
        return bridged[i];
    }

    /** How steep it is at most along it, rise over run, between any two of its line's points. */
    public double steepest() {
        double steepest = 0;
        for (int i = 1; i < levels.length; i++) {
            final double run = alignment.along(i) - alignment.along(i - 1);
            if (run > 0) {
                steepest = Math.max(steepest, Math.abs(levels[i] - levels[i - 1]) / run);
            }
        }
        return steepest;
    }

    /** What is wrong with it: its line's problems, and any grade it could not keep to. */
    public List<String> problems() {
        return problems;
    }

    @Override
    public double shape(final double x, final double z, final double under) {
        final Polyline.Near near = line.nearest(x, z);
        if (near == null) {
            return under;
        }
        final int j = near.segment();
        final double t = near.t();
        if (bridged[j] && bridged[j + 1]) {
            return under;
        }
        final double level = levels[j] + (levels[j + 1] - levels[j]) * t;
        final double half = halves[j] + (halves[j + 1] - halves[j]) * t;
        final double camber = cambers[j] + (cambers[j + 1] - cambers[j]) * t;
        final double slope = slopes[j] + (slopes[j + 1] - slopes[j]) * t;
        final double d = near.distance();
        if (d <= half) {
            return level - camber * d;
        }
        // its side slope, from its edge to the ground, as wide as the slope needs
        final double edge = level - camber * half;
        final double rise = under - edge;
        final double wide = Math.max(SLOPE, Math.abs(rise) * slope);
        if (d >= half + wide) {
            return under;
        }
        return edge + rise * (d - half) / wide;
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

    /** Its surface: all of it across, thinning out into the grass over {@link #EDGE} either side of its edges; none on a bridge. */
    @Override
    public double cover(final Land.Cover cover, final double x, final double z) {
        final Polyline.Near near = line.nearest(x, z);
        if (near == null) {
            return 0;
        }
        final int j = near.segment();
        final double t = near.t();
        if (bridged[j] && bridged[j + 1]) {
            return 0;
        }
        final double half = halves[j] + (halves[j + 1] - halves[j]) * t;
        final double amount = covers[cover.ordinal()][j] + (covers[cover.ordinal()][j + 1] - covers[cover.ordinal()][j]) * t;
        return amount * eased((half + EDGE - near.distance()) / (2 * EDGE));
    }
}
