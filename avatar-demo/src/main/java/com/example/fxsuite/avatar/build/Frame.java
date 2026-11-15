package com.example.fxsuite.avatar.build;

import java.util.ArrayList;
import java.util.List;

/**
 * A tower's structure, laid out in 2D and shared by all its sections: its cores (each any outline:
 * a rectangle, an octagon, a cross, a triangle, two side by side, one at each corner) and its columns
 * (each a point, round or square, so big at the foot of the tower: on a grid, in a ring, a few mega
 * columns). A section's outline is its envelope; what holds it up is this, the same from the ground
 * up: a column stands in every section whose outline it is in, and stops where it is not (at a
 * setback's roof); a core runs all the way up, and on over the roof.
 *
 * <p>In centimetres, x across, y along, as plans are. Only seen, as yet: the same is what forces
 * would be worked through, were they to be.
 *
 * @param cores   its cores' outlines, each anticlockwise
 * @param columns its columns
 */
public record Frame(List<List<Plan.Pt>> cores, List<Column> columns) {

    /** A column's section. */
    public enum Section { ROUND, SQUARE }

    /** A column: where it stands, its section, and how big across at the foot of the tower. */
    public record Column(Plan.Pt at, Section section, double size) {
    }

    public Frame {
        cores = cores.stream().map(List::copyOf).toList();
        columns = List.copyOf(columns);
    }

    /** How far behind a curtain wall a column on its line is set, from the wall to its face. */
    public static final double BEHIND = 30;
    /** How much thinner a column is at the top of the tower than at its foot. */
    public static final double TAPER = 0.4;

    // ---------------------------------------------------------------- cores

    /** A rectangle about ({@code x}, {@code y}), {@code w} across and {@code d} along. */
    public static List<Plan.Pt> rectangle(final double x, final double y, final double w, final double d) {
        return List.of(new Plan.Pt(x - w / 2, y - d / 2), new Plan.Pt(x + w / 2, y - d / 2),
                new Plan.Pt(x + w / 2, y + d / 2), new Plan.Pt(x - w / 2, y + d / 2));
    }

    /** A rectangle about ({@code x}, {@code y}), {@code w} by {@code d}, its corners cut {@code cut}: an octagon. */
    public static List<Plan.Pt> octagon(final double x, final double y, final double w, final double d, final double cut) {
        final double a = w / 2;
        final double b = d / 2;
        return List.of(new Plan.Pt(x - a + cut, y - b), new Plan.Pt(x + a - cut, y - b), new Plan.Pt(x + a, y - b + cut),
                new Plan.Pt(x + a, y + b - cut), new Plan.Pt(x + a - cut, y + b), new Plan.Pt(x - a + cut, y + b),
                new Plan.Pt(x - a, y + b - cut), new Plan.Pt(x - a, y - b + cut));
    }

    /** A cross about ({@code x}, {@code y}), its arms {@code span} end to end and {@code arm} wide. */
    public static List<Plan.Pt> cross(final double x, final double y, final double span, final double arm) {
        final double s = span / 2;
        final double a = arm / 2;
        return List.of(new Plan.Pt(x - a, y - s), new Plan.Pt(x + a, y - s), new Plan.Pt(x + a, y - a),
                new Plan.Pt(x + s, y - a), new Plan.Pt(x + s, y + a), new Plan.Pt(x + a, y + a), new Plan.Pt(x + a, y + s),
                new Plan.Pt(x - a, y + s), new Plan.Pt(x - a, y + a), new Plan.Pt(x - s, y + a), new Plan.Pt(x - s, y - a),
                new Plan.Pt(x - a, y - a));
    }

    /** A circle about ({@code x}, {@code y}), {@code r} out, in {@code sides} straight pieces. */
    public static List<Plan.Pt> circle(final double x, final double y, final double r, final int sides) {
        final List<Plan.Pt> round = new ArrayList<>();
        for (int k = 0; k < sides; k++) {
            final double a = 2 * Math.PI * k / sides;
            round.add(new Plan.Pt(x + r * Math.cos(a), y + r * Math.sin(a)));
        }
        return round;
    }

    // ---------------------------------------------------------------- columns

    /**
     * Square columns {@code size} across on {@code plan}'s columns (its grid's, and those round its
     * curves), but those in a core; any on its outline set in behind the curtain wall.
     */
    public static List<Column> grid(final Plan plan, final double size, final List<List<Plan.Pt>> cores) {
        final Plan.Layout layout = plan.layout();
        final List<Plan.Pt> outline = layout.shaped().getFirst();
        final List<Column> columns = new ArrayList<>();
        for (final Plan.Pt c : layout.allColumns().values()) {
            // none in a core, nor on its wall: that is the core's
            if (cores.stream().anyMatch(core -> inside(core, c) || onEdge(core, c))) {
                continue;
            }
            columns.add(new Column(behind(outline, c, size / 2 + BEHIND), Section.SQUARE, size));
        }
        return columns;
    }

    /** {@code count} round columns {@code size} across in a ring {@code r} out about ({@code x}, {@code y}). */
    public static List<Column> ring(final double x, final double y, final double r, final int count, final double size) {
        final List<Column> columns = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            final double a = 2 * Math.PI * (k + 0.5) / count;
            columns.add(new Column(new Plan.Pt(x + r * Math.cos(a), y + r * Math.sin(a)), Section.ROUND, size));
        }
        return columns;
    }

    /** Square columns {@code size} across at {@code points}: mega columns at a tower's corners, say. */
    public static List<Column> at(final double size, final Plan.Pt... points) {
        return java.util.Arrays.stream(points).map(p -> new Column(p, Section.SQUARE, size)).toList();
    }

    /**
     * {@code p}, if it is on {@code outline}, set in from it by {@code in}: off each piece of it it is
     * on, square to that piece. As it is, if it is not on it.
     */
    static Plan.Pt behind(final List<Plan.Pt> outline, final Plan.Pt p, final double in) {
        double x = p.x();
        double y = p.y();
        final List<double[]> normals = new ArrayList<>();
        for (int k = 0; k < outline.size(); k++) {
            final Plan.Pt a = outline.get(k);
            final Plan.Pt b = outline.get((k + 1) % outline.size());
            if (toSegment(p, a, b) < Plan.TOLERANCE) {
                final double length = a.distance(b);
                // inside on its left, the outline anticlockwise
                final double[] n = {-(b.y() - a.y()) / length, (b.x() - a.x()) / length};
                if (normals.stream().noneMatch(m -> m[0] * n[0] + m[1] * n[1] > 0.99)) {
                    normals.add(n);
                }
            }
        }
        for (final double[] n : normals) {
            x += n[0] * in;
            y += n[1] * in;
        }
        return new Plan.Pt(x, y);
    }

    private static double toSegment(final Plan.Pt p, final Plan.Pt a, final Plan.Pt b) {
        final double ex = b.x() - a.x();
        final double ey = b.y() - a.y();
        final double length = ex * ex + ey * ey;
        final double t = length < 1e-12 ? 0 : Math.clamp(((p.x() - a.x()) * ex + (p.y() - a.y()) * ey) / length, 0, 1);
        return Math.hypot(a.x() + ex * t - p.x(), a.y() + ey * t - p.y());
    }

    /** Whether {@code p} is on {@code outline}'s edge. */
    static boolean onEdge(final List<Plan.Pt> outline, final Plan.Pt p) {
        for (int k = 0; k < outline.size(); k++) {
            if (toSegment(p, outline.get(k), outline.get((k + 1) % outline.size())) < Plan.TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code p} is inside {@code outline}. */
    static boolean inside(final List<Plan.Pt> outline, final Plan.Pt p) {
        boolean in = false;
        for (int i = 0, j = outline.size() - 1; i < outline.size(); j = i++) {
            final Plan.Pt a = outline.get(i);
            final Plan.Pt b = outline.get(j);
            if ((a.y() > p.y()) != (b.y() > p.y()) && p.x() < (b.x() - a.x()) * (p.y() - a.y()) / (b.y() - a.y()) + a.x()) {
                in = !in;
            }
        }
        return in;
    }

    /**
     * A tower's frame when it says nothing of its own: no core (a church's nave, a hall: no lifts to
     * carry); square columns on its foot's grid, set in behind its curtain wall. A skyscraper says its
     * own ({@link #with}).
     */
    public static Frame of(final Tower tower) {
        return new Frame(List.of(), grid(tower.sections().getFirst().plan(), 70, List.of()));
    }

    /** {@code cores}, and square columns {@code size} across on {@code plan}'s grid round them. */
    public static Frame with(final Plan plan, final double size, final List<List<Plan.Pt>> cores) {
        return new Frame(cores, grid(plan, size, cores));
    }

    // ---------------------------------------------------------------- in a section

    /** How big a column is at {@code up}, in a tower {@code high}: thinner the higher, by its taper at the top. */
    public static double size(final Column column, final double up, final double high) {
        return column.size() * (1 - TAPER * Math.clamp(up / high, 0, 1));
    }

    /** Its columns standing in a section of outline {@code layout}: those within it. */
    public List<Column> in(final Plan.Layout layout) {
        return columns.stream().filter(c -> layout.inside(c.at())).toList();
    }

    /** A column's footprint at {@code size} across: a square, or a round one in sixteen pieces. */
    public static List<Plan.Pt> footprint(final Column column, final double size) {
        final Plan.Pt c = column.at();
        return column.section() == Section.SQUARE ? rectangle(c.x(), c.y(), size, size) : circle(c.x(), c.y(), size / 2, 16);
    }
}
