package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Curve;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A building composed of parts, each made the way it is easiest to make it (a plan stacked, or a 3D
 * graph drawn), each placed ({@link Placed}: moved, and turned about the upright) and put together
 * into one skeleton ({@link Form}): where parts' vertices fall on one another, one vertex; where one
 * part's vertex falls on another's straight edge, that edge split there, so the two are joined. Each
 * part says which of its vertices it means to join another part at ({@link Placed#joints}); one that
 * meets nothing is said ({@link Result#problems}).
 */
public final class Assembly {

    private Assembly() {
    }

    /** Points nearer than this, in centimetres, are one. */
    public static final double TOLERANCE = 0.5;

    /**
     * A part, placed: its skeleton {@code form}, turned {@code turn} degrees about the upright
     * (anticlockwise, seen from above), then moved by {@code offset}; and the names of its vertices it
     * means to join another part at.
     */
    public record Placed(String name, Form form, Vec3 offset, double turn, List<String> joints) {

        public Placed {
            joints = List.copyOf(joints);
        }

        /** Where its vertex {@code v} is, placed. */
        public Vec3 at(final int v) {
            return Curve.turn(form.at(v), turn).add(offset);
        }
    }

    /** What came of putting the parts together: one skeleton, and what is wrong with how they meet. */
    public record Result(Form form, List<String> problems) {

        public Result {
            problems = List.copyOf(problems);
        }
    }

    /** {@code parts}, put together into one skeleton called {@code name}. */
    public static Result of(final String name, final List<Placed> parts) {
        final List<Vec3> points = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        final List<Set<Integer>> owners = new ArrayList<>();
        final Map<String, Integer> at = new HashMap<>();
        final List<int[]> index = new ArrayList<>();
        for (int p = 0; p < parts.size(); p++) {
            final Placed part = parts.get(p);
            final int[] mine = new int[part.form().vertices().size()];
            for (int v = 0; v < mine.length; v++) {
                final Vec3 placed = part.at(v);
                final String key = key(placed);
                final Integer there = at.get(key);
                if (there != null) {
                    mine[v] = there;
                } else {
                    mine[v] = points.size();
                    at.put(key, mine[v]);
                    points.add(placed);
                    names.add(part.name() + ", " + part.form().vertices().get(v).name());
                    owners.add(new HashSet<>());
                }
                owners.get(mine[v]).add(p);
            }
            index.add(mine);
        }
        // each part's edges, placed: a straight one split wherever another vertex falls on it
        final List<int[]> straight = new ArrayList<>();
        final List<Curve> curves = new ArrayList<>();
        final List<int[]> curved = new ArrayList<>();
        final Set<Long> joined = new HashSet<>();
        for (int p = 0; p < parts.size(); p++) {
            final Placed part = parts.get(p);
            for (final Form.Edge edge : part.form().edges()) {
                final int a = index.get(p)[edge.from()];
                final int b = index.get(p)[edge.to()];
                if (a == b || !joined.add(((long) Math.min(a, b) << 32) | Math.max(a, b))) {
                    continue;
                }
                if (edge.curved()) {
                    curved.add(new int[] {a, b});
                    curves.add(edge.curve().turned(part.turn()).moved(part.offset()));
                } else {
                    straight.add(new int[] {a, b, p});
                }
            }
        }
        final Form.Builder form = new Form.Builder(name);
        for (int v = 0; v < points.size(); v++) {
            form.vertex(names.get(v), points.get(v).x(), points.get(v).y(), points.get(v).z());
        }
        final Set<Long> laid = new HashSet<>();
        for (final int[] edge : straight) {
            final Vec3 a = points.get(edge[0]);
            final Vec3 b = points.get(edge[1]);
            final List<Integer> on = new ArrayList<>();
            for (int v = 0; v < points.size(); v++) {
                if (v != edge[0] && v != edge[1] && between(points.get(v), a, b)) {
                    on.add(v);
                    owners.get(v).add(edge[2]);
                }
            }
            on.sort((x, y) -> Double.compare(points.get(x).distance(a), points.get(y).distance(a)));
            int from = edge[0];
            on.add(edge[1]);
            for (final int to : on) {
                if (laid.add(((long) Math.min(from, to) << 32) | Math.max(from, to))) {
                    form.edge(from, to);
                }
                from = to;
            }
        }
        for (int k = 0; k < curved.size(); k++) {
            final int a = curved.get(k)[0];
            final int b = curved.get(k)[1];
            if (laid.add(((long) Math.min(a, b) << 32) | Math.max(a, b))) {
                form.edge(a, b, curves.get(k));
            }
        }
        // the joints each part meant: each must meet another part, its vertex shared, or on its edge
        final List<String> problems = new ArrayList<>();
        for (int p = 0; p < parts.size(); p++) {
            final Placed part = parts.get(p);
            for (final String joint : part.joints()) {
                final int v = part.form().vertex(joint);
                if (v < 0) {
                    problems.add(part.name() + ": no vertex " + joint + " to join at");
                } else if (owners.get(index.get(p)[v]).size() < 2) {
                    problems.add(part.name() + ": its " + joint + " meets no other part");
                }
            }
        }
        return new Result(form.build(), problems);
    }

    private static String key(final Vec3 p) {
        return Math.round(p.x() * 10) + "," + Math.round(p.y() * 10) + "," + Math.round(p.z() * 10);
    }

    /** Whether {@code p} is on the straight line from {@code a} to {@code b}, between its ends. */
    private static boolean between(final Vec3 p, final Vec3 a, final Vec3 b) {
        final Vec3 run = b.subtract(a);
        final double length = run.magnitude();
        if (length < TOLERANCE) {
            return false;
        }
        final double t = p.subtract(a).dotProduct(run) / (length * length);
        if (t * length < TOLERANCE || (1 - t) * length < TOLERANCE) {
            return false;
        }
        return a.add(run.multiply(t)).distance(p) < TOLERANCE;
    }
}
