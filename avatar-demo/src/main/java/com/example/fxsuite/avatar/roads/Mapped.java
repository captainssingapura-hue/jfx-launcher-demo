package com.example.fxsuite.avatar.roads;

import com.example.fxsuite.avatar.sim.Alignment;
import com.example.fxsuite.avatar.sim.RoadMap;
import com.example.fxsuite.avatar.sim.Vec2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A road map ({@link RoadMap}) as plans to lay ({@link Roadworks.Plan}): each way along its line worked out
 * ({@link Alignment}); of its first segment's kind, and where a later segment's kind is another, a part from that
 * vertex ({@link Roadway.Part}), eased over {@link #TRANSITION}, or less where the next change or its end is nearer; a
 * one-way road's line, described down its middle, moved to its pavement's edge, half its paved width there, however
 * that changes along it; its bridge segments given as structures; its pinned vertices pinned; and where a bridge
 * segment crosses another way's line, not where they share a node, {@link #HEADROOM} over the ground there.
 *
 * <p>Which kind across a segment is, of the way it is on, the caller says: by its kind's name, and the way, whose name
 * may say it is one way.
 */
public final class Mapped {

    /** How long a road takes to change from one kind to the next, at most. */
    static final double TRANSITION = 3_000;
    /** How much room is left under a bridge over another road, under its deck. */
    static final double HEADROOM = 500;
    /** How far either side of where a bridge crosses another road it is held over it, along it. */
    static final double OVER = 1_500;
    /** How near a node two ways meet at for them to meet there, not cross. */
    private static final double MEET = 300;

    private Mapped() {
    }

    /**
     * {@code map}'s ways as plans, in the order {@code order} puts them (as the map has them, if null), each segment
     * of the kind {@code kinds} says of it and its way; laid over {@code raw}, as far as heads are kept clear under
     * bridges.
     */
    public static List<Roadworks.Plan> plans(final RoadMap map, final BiFunction<RoadMap.Way, String, Section> kinds,
                                             final Profile raw, final Comparator<RoadMap.Way> order) {
        final List<RoadMap.Way> ways = new ArrayList<>(map.ways());
        if (order != null) {
            ways.sort(order);
        }
        final List<Alignment> lines = new ArrayList<>();
        final List<double[]> boxes = new ArrayList<>();
        for (final RoadMap.Way way : ways) {
            final Alignment line = Alignment.of(map, way, Alignment.STEP);
            lines.add(line);
            boxes.add(box(line.points(), 0));
        }
        final List<Roadworks.Plan> plans = new ArrayList<>();
        for (int w = 0; w < ways.size(); w++) {
            plans.add(plan(ways, lines, boxes, w, kinds, raw));
        }
        return plans;
    }

    private static Roadworks.Plan plan(final List<RoadMap.Way> ways, final List<Alignment> lines,
                                       final List<double[]> boxes, final int w,
                                       final BiFunction<RoadMap.Way, String, Section> kinds, final Profile raw) {
        final RoadMap.Way way = ways.get(w);
        final Alignment line = lines.get(w);
        final int segments = way.segments().size();
        final Section[] of = new Section[segments];
        for (int k = 0; k < segments; k++) {
            of[k] = kinds.apply(way, way.segments().get(k).kind());
            if (of[k] == null) {
                throw new IllegalStateException("\"" + way.name() + "\": no kind \"" + way.segments().get(k).kind() + "\"");
            }
        }
        final Section section = of[0];
        // where its kind changes, a part: eased over no more than the way to the change before it or after it
        final List<Integer> changes = new ArrayList<>();
        for (int k = 1; k < segments; k++) {
            if (!Objects.equals(of[k], of[k - 1])) {
                changes.add(k);
            }
        }
        final List<Roadway.Part> parts = new ArrayList<>();
        for (int c = 0; c < changes.size(); c++) {
            final double at = line.vertexAlong(changes.get(c));
            final double before = at - (c == 0 ? 0 : line.vertexAlong(changes.get(c - 1)));
            final double after = (c + 1 < changes.size() ? line.vertexAlong(changes.get(c + 1)) : line.length()) - at;
            parts.add(new Roadway.Part(at, of[changes.get(c)], Math.min(TRANSITION, Math.min(before, after))));
        }
        List<Vec2> points = line.points();
        final int side = section.pavedSide();
        if (side != 0) {
            // its pavement where its middle was described: its line at the pavement's edge, half its width aside there
            final double[] along = new double[points.size()];
            for (int i = 0; i < along.length; i++) {
                along[i] = line.along(i);
            }
            final Section[] at = Roadway.kinds(along, section, parts);
            final double[] by = new double[along.length];
            for (int i = 0; i < by.length; i++) {
                by[i] = -side * at[i].pavedEdge(side) / 2;
            }
            points = new ArrayList<>(Roadworks.offset(points, by));
            if (line.loop()) {
                points.set(points.size() - 1, points.getFirst());
            }
        }
        final List<Roadway.Given> given = new ArrayList<>();
        final List<Roadway.Clearance> clearances = new ArrayList<>();
        for (int k = 0; k < segments; k++) {
            if (!way.segments().get(k).bridge()) {
                continue;
            }
            final double from = line.vertexAlong(k);
            final double to = line.vertexAlong(k + 1);
            if (!given.isEmpty() && given.getLast().to() == from) {
                given.set(given.size() - 1, new Roadway.Given(given.getLast().from(), to, Carrier.STRUCTURE));
            } else {
                given.add(new Roadway.Given(from, to, Carrier.STRUCTURE));
            }
            clearances.addAll(over(ways, lines, boxes, w, from, to, raw));
        }
        final List<Roadway.Pin> pins = new ArrayList<>();
        for (int v = 0; v < way.vertices().size(); v++) {
            final RoadMap.Vertex vertex = way.vertices().get(v);
            if (vertex.pinned()) {
                pins.add(new Roadway.Pin(line.vertexAlong(v), vertex.height()));
            }
        }
        return new Roadworks.Plan(way.name(), points, section, given, clearances, 0, pins, parts);
    }

    /**
     * Where the {@code w}th way, from {@code from} to {@code to} along it, a bridge, crosses another way's line, not
     * at a node they share, nor where the other is a bridge too: held {@link #HEADROOM} and a deck over the ground
     * there, {@link #OVER} either side.
     */
    private static List<Roadway.Clearance> over(final List<RoadMap.Way> ways, final List<Alignment> lines,
                                                final List<double[]> boxes, final int w, final double from,
                                                final double to, final Profile raw) {
        final Alignment line = lines.get(w);
        final List<Vec2> span = new ArrayList<>();
        final List<Double> spanAlong = new ArrayList<>();
        for (int i = 0; i < line.size(); i++) {
            if (line.along(i) >= from - 1e-6 && line.along(i) <= to + 1e-6) {
                span.add(line.points().get(i));
                spanAlong.add(line.along(i));
            }
        }
        final List<Roadway.Clearance> found = new ArrayList<>();
        if (span.size() < 2) {
            return found;
        }
        final double[] box = box(span, 0);
        for (int o = 0; o < ways.size(); o++) {
            final double[] them = boxes.get(o);
            if (o == w || them[0] > box[2] || them[2] < box[0] || them[1] > box[3] || them[3] < box[1]) {
                continue;
            }
            final Alignment other = lines.get(o);
            for (int i = 0; i + 1 < span.size(); i++) {
                for (int j = 0; j + 1 < other.size(); j++) {
                    final double t = cross(span.get(i), span.get(i + 1), other.points().get(j), other.points().get(j + 1));
                    if (Double.isNaN(t)) {
                        continue;
                    }
                    final Vec2 p = new Vec2(span.get(i).x() + t * (span.get(i + 1).x() - span.get(i).x()),
                            span.get(i).z() + t * (span.get(i + 1).z() - span.get(i).z()));
                    if (meet(ways.get(w), ways.get(o), p) || ways.get(o).segments().get(other.segment(j)).bridge()) {
                        continue;
                    }
                    final double s = spanAlong.get(i) + t * (spanAlong.get(i + 1) - spanAlong.get(i));
                    // once, where it crosses at a point both lines have: found from either side of it
                    if (found.stream().anyMatch(c -> Math.abs(c.from() + OVER - s) < 100)) {
                        continue;
                    }
                    found.add(new Roadway.Clearance(s - OVER, s + OVER,
                            raw.height(p.x(), p.z()) + HEADROOM + Roadway.DECK));
                }
            }
        }
        return found;
    }

    /** Whether ways {@code a} and {@code b} share a node near {@code p}: they meet there, and do not cross. */
    private static boolean meet(final RoadMap.Way a, final RoadMap.Way b, final Vec2 p) {
        for (final RoadMap.Vertex v : a.vertices()) {
            if (v.node() == null || Math.hypot(v.at().x() - p.x(), v.at().z() - p.z()) > MEET) {
                continue;
            }
            for (final RoadMap.Vertex u : b.vertices()) {
                if (v.node().equals(u.node())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** How far from {@code a} to {@code b} it crosses {@code c} to {@code d}; not a number if it does not. */
    static double cross(final Vec2 a, final Vec2 b, final Vec2 c, final Vec2 d) {
        final double rx = b.x() - a.x();
        final double rz = b.z() - a.z();
        final double sx = d.x() - c.x();
        final double sz = d.z() - c.z();
        final double den = rx * sz - rz * sx;
        if (Math.abs(den) < 1e-9) {
            return Double.NaN;
        }
        final double t = ((c.x() - a.x()) * sz - (c.z() - a.z()) * sx) / den;
        final double u = ((c.x() - a.x()) * rz - (c.z() - a.z()) * rx) / den;
        return t >= 0 && t <= 1 && u >= 0 && u <= 1 ? t : Double.NaN;
    }

    /** The box round {@code points}, and {@code margin}: min x, min z, max x, max z. */
    private static double[] box(final List<Vec2> points, final double margin) {
        final double[] box = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY};
        for (final Vec2 p : points) {
            box[0] = Math.min(box[0], p.x() - margin);
            box[1] = Math.min(box[1], p.z() - margin);
            box[2] = Math.max(box[2], p.x() + margin);
            box[3] = Math.max(box[3], p.z() + margin);
        }
        return box;
    }
}
