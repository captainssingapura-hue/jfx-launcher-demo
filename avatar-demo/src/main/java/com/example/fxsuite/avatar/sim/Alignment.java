package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * A road's line, worked out from its vertices and its segments' curvatures ({@link RoadMap.Way}): each segment
 * a straight or an arc between its vertices, the short way round; and at each vertex where the segments either
 * side differ in anything, a transition, the line easing from the one into the other over the transition's
 * length, half before the vertex and half after (a curve, so neither its heading nor where it is jumps). A
 * point every so far along it, each with how far along it is and which segment it is on.
 *
 * <p>What cannot be worked out, or should be looked at, is reported ({@link #problems}): a curvature that cannot
 * reach from one vertex to the next (an arc turns half way round at most: its sign says which side), or is
 * tighter than its kind turns; a kink at
 * a vertex where its kind allows no corner; a segment too short for the transitions at its ends.
 */
public final class Alignment {

    /** How far apart its points are, about. */
    public static final double STEP = 200;
    /** A kink of less than this, in radians, is none. */
    private static final double STRAIGHT_ON = Math.toRadians(0.5);
    /** The most of a segment the transitions at its ends may take, each; and the least of what they ask they may be cut to. */
    private static final double SHARE = 0.45;
    private static final double LEAST = 0.5;

    /** A segment's line: from {@code a} to {@code b}, {@code length} long, heading {@code start} at {@code a} and {@code end} at {@code b}. */
    private record Leg(Vec2 a, Vec2 b, double curvature, double length, Vec2 start, Vec2 end) {

        Vec2 at(final double s) {
            if (curvature == 0) {
                return new Vec2(a.x() + start.x() * s, a.z() + start.z() * s);
            }
            final double cx = a.x() - start.z() / curvature;
            final double cz = a.z() + start.x() / curvature;
            final double turn = curvature * s;
            final double rx = a.x() - cx;
            final double rz = a.z() - cz;
            return new Vec2(cx + rx * Math.cos(turn) - rz * Math.sin(turn), cz + rx * Math.sin(turn) + rz * Math.cos(turn));
        }

        Vec2 heading(final double s) {
            return turned(start, curvature * s);
        }
    }

    private final List<Vec2> points;
    private final double[] along;
    private final int[] segment;
    /** How far along each vertex is; and how far before and after it its transition reaches (0, 0 if it has none). */
    private final double[] vertexAlong;
    private final double[] before;
    private final double[] after;
    private final List<String> problems;
    private final boolean loop;

    private Alignment(final List<Vec2> points, final double[] along, final int[] segment, final double[] vertexAlong,
                      final double[] before, final double[] after, final List<String> problems, final boolean loop) {
        this.points = List.copyOf(points);
        this.along = along;
        this.segment = segment;
        this.vertexAlong = vertexAlong;
        this.before = before;
        this.after = after;
        this.problems = List.copyOf(problems);
        this.loop = loop;
    }

    /** {@code way}'s line, its kinds as {@code map} has them, a point every {@code step} or so. */
    public static Alignment of(final RoadMap map, final RoadMap.Way way, final double step) {
        final List<String> problems = new ArrayList<>();
        final String name = "\"" + way.name() + "\"";
        final int n = way.segments().size();
        final List<Leg> legs = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            final RoadMap.Segment seg = way.segments().get(k);
            final Vec2 a = way.vertices().get(k).at();
            final Vec2 b = way.end(k).at();
            final double chord = Math.hypot(b.x() - a.x(), b.z() - a.z());
            if (chord == 0) {
                throw new IllegalArgumentException(name + ": segment " + (k + 1) + " goes nowhere");
            }
            final Vec2 u = new Vec2((b.x() - a.x()) / chord, (b.z() - a.z()) / chord);
            double curvature = seg.curvature();
            final RoadMap.Kind kind = map.kind(seg.kind());
            if (kind != null && Math.abs(curvature) > kind.tightest() * (1 + 1e-9)) {
                problems.add(name + ": segment " + (k + 1) + " turns tighter than a " + kind.name() + " does");
            }
            if (chord * Math.abs(curvature) / 2 > 1) {
                problems.add(name + ": segment " + (k + 1) + "'s curve cannot reach from one vertex to the next");
                curvature = 0;
            }
            if (curvature == 0) {
                legs.add(new Leg(a, b, 0, chord, u, u));
            } else {
                final double angle = 2 * Math.asin(chord * Math.abs(curvature) / 2);
                final double half = Math.signum(curvature) * angle / 2;
                legs.add(new Leg(a, b, curvature, angle / Math.abs(curvature), turned(u, -half), turned(u, half)));
            }
        }
        // where each vertex is along it
        final int vertices = way.vertices().size();
        final double[] vertexAlong = new double[vertices + 1];
        for (int k = 0; k < n; k++) {
            vertexAlong[k + 1] = vertexAlong[k] + legs.get(k).length();
        }
        final double total = vertexAlong[n];
        // the transitions: at each vertex between two segments that differ in anything, or meet at a kink
        final double[] before = new double[vertices];
        final double[] after = new double[vertices];
        for (int v = 0; v < vertices; v++) {
            final int in = v - 1 >= 0 ? v - 1 : way.loop() ? n - 1 : -1;
            final int out = v < n ? v : -1;
            if (in < 0 || out < 0) {
                continue;
            }
            final RoadMap.Segment a = way.segments().get(in);
            final RoadMap.Segment b = way.segments().get(out);
            final double kink = angle(legs.get(in).end(), legs.get(out).start());
            final RoadMap.Kind ka = map.kind(a.kind());
            final RoadMap.Kind kb = map.kind(b.kind());
            if (kink > STRAIGHT_ON && way.vertices().get(v).node() == null
                    && (ka != null && !ka.corners() || kb != null && !kb.corners())) {
                problems.add(name + ": a corner of " + Math.round(Math.toDegrees(kink)) + "° at vertex " + (v + 1)
                        + ", where its kind allows none");
            }
            if (a.equals(b) && kink <= STRAIGHT_ON) {
                continue;
            }
            final double asked = Math.max(ka == null ? 0 : ka.transition(), kb == null ? 0 : kb.transition()) / 2;
            before[v] = Math.min(asked, SHARE * legs.get(in).length());
            after[v] = Math.min(asked, SHARE * legs.get(out).length());
            if (before[v] < LEAST * asked || after[v] < LEAST * asked) {
                problems.add(name + ": too short either side of vertex " + (v + 1) + " for its transition");
            }
        }
        // the line, a point every step or so along each segment, eased through each transition
        final List<Vec2> points = new ArrayList<>();
        final List<Double> at = new ArrayList<>();
        final List<Integer> on = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            final Leg leg = legs.get(k);
            final int steps = Math.max(1, (int) Math.ceil(leg.length() / step));
            for (int i = 0; i < steps; i++) {
                final double s = leg.length() * i / steps;
                points.add(eased(legs, way, before, after, k, s));
                at.add(vertexAlong[k] + s);
                on.add(k);
            }
        }
        if (way.loop()) {
            points.add(points.getFirst());
        } else {
            points.add(legs.getLast().b());
        }
        at.add(total);
        on.add(n - 1);
        final double[] along = new double[points.size()];
        final int[] segment = new int[points.size()];
        for (int i = 0; i < along.length; i++) {
            along[i] = at.get(i);
            segment[i] = on.get(i);
        }
        return new Alignment(points, along, segment, vertexAlong, before, after, problems, way.loop());
    }

    /**
     * Where the line is {@code s} along segment {@code k}: on it, or, within a transition at either of its ends, on
     * the curve eased from the segment before into the one after (a cubic, so where it is and which way it heads
     * agree with each at its ends).
     */
    private static Vec2 eased(final List<Leg> legs, final RoadMap.Way way, final double[] before, final double[] after,
                              final int k, final double s) {
        final Leg leg = legs.get(k);
        final int n = legs.size();
        final int first = k;
        final int last = (k + 1) % way.vertices().size();
        if (s < after[first] && (k > 0 || way.loop())) {
            final Leg in = legs.get((k - 1 + n) % n);
            return hermite(in, in.length() - before[first], leg, after[first], (before[first] + s) / (before[first] + after[first]));
        }
        if (leg.length() - s < before[last] && (k < n - 1 || way.loop())) {
            final Leg out = legs.get((k + 1) % n);
            return hermite(leg, leg.length() - before[last], out, after[last],
                    (s - (leg.length() - before[last])) / (before[last] + after[last]));
        }
        return leg.at(s);
    }

    /** The cubic from {@code from} at {@code s0} to {@code to} at {@code s1}, {@code t} of the way. */
    private static Vec2 hermite(final Leg from, final double s0, final Leg to, final double s1, final double t) {
        final Vec2 p0 = from.at(s0);
        final Vec2 p1 = to.at(s1);
        final Vec2 t0 = from.heading(s0);
        final Vec2 t1 = to.heading(s1);
        final double d = Math.hypot(p1.x() - p0.x(), p1.z() - p0.z());
        final double t2 = t * t;
        final double t3 = t2 * t;
        final double h00 = 2 * t3 - 3 * t2 + 1;
        final double h10 = t3 - 2 * t2 + t;
        final double h01 = -2 * t3 + 3 * t2;
        final double h11 = t3 - t2;
        return new Vec2(h00 * p0.x() + h10 * d * t0.x() + h01 * p1.x() + h11 * d * t1.x(),
                h00 * p0.z() + h10 * d * t0.z() + h01 * p1.z() + h11 * d * t1.z());
    }

    /** {@code v} turned {@code angle} to the left (radians), going along it. */
    static Vec2 turned(final Vec2 v, final double angle) {
        final double c = Math.cos(angle);
        final double s = Math.sin(angle);
        return new Vec2(v.x() * c - v.z() * s, v.x() * s + v.z() * c);
    }

    private static double angle(final Vec2 a, final Vec2 b) {
        return Math.acos(Num.clamp(a.x() * b.x() + a.z() * b.z(), -1, 1));
    }

    /**
     * The curvature an arc needs to go from {@code a}, heading {@code heading} (a unit vector), to {@code b}: signed,
     * + bending left. For a road's segments to meet with no kink, each heading on as the last ended.
     */
    public static double bending(final Vec2 a, final Vec2 heading, final Vec2 b) {
        final double dx = b.x() - a.x();
        final double dz = b.z() - a.z();
        final double chord = Math.hypot(dx, dz);
        // the angle from the heading to the chord, left positive
        final double sin = (heading.x() * dz - heading.z() * dx) / chord;
        return 2 * sin / chord;
    }

    /** Which way an arc of curvature {@code curvature} from {@code a} to {@code b}, the short way round, heads at {@code b}. */
    public static Vec2 arriving(final Vec2 a, final Vec2 b, final double curvature) {
        final double chord = Math.hypot(b.x() - a.x(), b.z() - a.z());
        final Vec2 u = new Vec2((b.x() - a.x()) / chord, (b.z() - a.z()) / chord);
        if (curvature == 0) {
            return u;
        }
        final double angle = 2 * Math.asin(Num.clamp(chord * Math.abs(curvature) / 2, 0, 1));
        return turned(u, Math.signum(curvature) * angle / 2);
    }

    public List<Vec2> points() {
        return points;
    }

    public int size() {
        return points.size();
    }

    /** How far along its {@code i}th point is. */
    public double along(final int i) {
        return along[i];
    }

    /** Which segment its {@code i}th point is on. */
    public int segment(final int i) {
        return segment[i];
    }

    /** How long it is. */
    public double length() {
        return along[along.length - 1];
    }

    /** How far along vertex {@code v} is. */
    public double vertexAlong(final int v) {
        return vertexAlong[v];
    }

    /** How far before vertex {@code v}, and after it, its transition reaches: none, if it has none. */
    public double before(final int v) {
        return before[v];
    }

    public double after(final int v) {
        return after[v];
    }

    public boolean loop() {
        return loop;
    }

    public List<String> problems() {
        return problems;
    }

    /** Where it crosses {@code other}, if it does, the first place it is found; null if not. */
    public Vec2 crossing(final Alignment other) {
        final Polyline them = new Polyline(other.points, STEP);
        for (int i = 0; i + 1 < points.size(); i++) {
            final Vec2 a = points.get(i);
            final Vec2 b = points.get(i + 1);
            if (them.distance((a.x() + b.x()) / 2, (a.z() + b.z()) / 2) == Double.POSITIVE_INFINITY) {
                continue;
            }
            for (int j = 0; j + 1 < other.points.size(); j++) {
                final Vec2 c = other.points.get(j);
                final Vec2 d = other.points.get(j + 1);
                if (Contour.cross(a, b, c, d)) {
                    return a;
                }
            }
        }
        return null;
    }
}
