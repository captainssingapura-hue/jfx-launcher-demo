package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.sim.Form;

import java.util.ArrayList;
import java.util.List;

/**
 * A body's lines, as a designer draws them before anything else: its longerons ({@code longerons}),
 * lines running its length (a keel, a chine, its widest line, a shoulder, a canopy's rail, a spine),
 * given where each crosses each of its sections ({@link Section}), tail to nose. From them, its
 * skeleton ({@link #skeleton}): a graph ({@link Form}) of a vertex where each longeron crosses each
 * section, edges round each section and along each longeron; and, dressed on that skeleton, its skin
 * ({@link #panels}): a flat panel between each two longerons and each two sections, each band
 * between two longerons painted as {@link Dress} says (a canopy's glass, a darker belly). So its
 * chines are lines it is drawn by, crisp, not a curve's facets.
 *
 * <p>{@code mirrored}: each section given for its right half only, from its keel up round to its
 * spine, both on its middle line, the left the right's mirror image; a fuselage. Or not: each section
 * given all the way round (a wing's, from its leading edge over its top and back under it).
 *
 * <p>Between the sections given, {@code steps} more to each span, on smooth curves through them, so
 * it is smooth along its length and crisp round it.
 *
 * @param name      what it is
 * @param longerons its lines' names, in order round a section (a mirrored one's from its keel to its
 *                  spine)
 * @param sections  its sections, tail to nose (along, increasing)
 * @param mirrored  whether each section is given for its right half only
 * @param steps     how many sections are put between each two given, to smooth it along its length
 */
public record Lines(String name, List<String> longerons, List<Section> sections, boolean mirrored, int steps) {

    /**
     * Where the longerons cross the body {@code along} its length: each {across, up}, in the order of
     * its longerons. All one point: the body's end, drawn to a point.
     */
    public record Section(double along, List<double[]> points) {

        public Section {
            points = points.stream().map(double[]::clone).toList();
        }

        /** All at one point: a nose, a tail. */
        public boolean point() {
            return points.stream().allMatch(p -> Math.abs(p[0] - points.getFirst()[0]) < 1e-6
                    && Math.abs(p[1] - points.getFirst()[1]) < 1e-6);
        }
    }

    /** How a band between two longerons is painted, {@code along} the body: by its name ("keel to chine"). */
    @FunctionalInterface
    public interface Dress {
        Paint paint(String band, double along);
    }

    public Lines {
        longerons = List.copyOf(longerons);
        sections = List.copyOf(sections);
        if (longerons.size() < (mirrored ? 2 : 3) || sections.size() < 2 || steps < 0) {
            throw new IllegalArgumentException(name + ": " + longerons.size() + " longerons, " + sections.size()
                    + " sections");
        }
        for (int k = 0; k < sections.size(); k++) {
            if (sections.get(k).points().size() != longerons.size()) {
                throw new IllegalArgumentException(name + ": section " + k + " crosses " + sections.get(k).points().size()
                        + " longerons, not " + longerons.size());
            }
            if (k > 0 && !(sections.get(k).along() > sections.get(k - 1).along())) {
                throw new IllegalArgumentException(name + ": its sections not tail to nose");
            }
        }
    }

    // ---------------------------------------------------------------- its sections, smoothed

    /** Its sections: those given, and {@code steps} between each two, on smooth curves through the given. */
    public List<Section> refined() {
        final List<Section> all = new ArrayList<>();
        final int n = sections.size();
        for (int k = 0; k + 1 < n; k++) {
            final Section a = sections.get(Math.max(0, k - 1));
            final Section b = sections.get(k);
            final Section c = sections.get(k + 1);
            final Section d = sections.get(Math.min(n - 1, k + 2));
            for (int s = 0; s <= steps; s++) {
                final double t = (double) s / (steps + 1);
                final List<double[]> points = new ArrayList<>();
                for (int i = 0; i < longerons.size(); i++) {
                    // drawn to a point at an end: straight in to it, not curving past it
                    final boolean end = b.point() || c.point();
                    points.add(new double[] {
                            end ? lerp(b.points().get(i)[0], c.points().get(i)[0], t)
                                    : curve(a.points().get(i)[0], b.points().get(i)[0], c.points().get(i)[0],
                                    d.points().get(i)[0], t),
                            end ? lerp(b.points().get(i)[1], c.points().get(i)[1], t)
                                    : curve(a.points().get(i)[1], b.points().get(i)[1], c.points().get(i)[1],
                                    d.points().get(i)[1], t)});
                }
                all.add(new Section(lerp(b.along(), c.along(), t), points));
            }
        }
        all.add(sections.getLast());
        return all;
    }

    private static double lerp(final double a, final double b, final double t) {
        return a + (b - a) * t;
    }

    /** A Catmull-Rom curve through {@code b} and {@code c}, {@code t} of the way, {@code a} and {@code d} beyond them. */
    private static double curve(final double a, final double b, final double c, final double d, final double t) {
        final double t2 = t * t;
        final double t3 = t2 * t;
        return 0.5 * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3);
    }

    // ---------------------------------------------------------------- round a section

    /** How many points round a section: a mirrored one's both halves, its middle line's once. */
    private int round() {
        return mirrored ? 2 * longerons.size() - 2 : longerons.size();
    }

    /** Which longeron the {@code i}th point round a section is on. */
    private int longeron(final int i) {
        return !mirrored || i < longerons.size() ? i : 2 * longerons.size() - 2 - i;
    }

    /** Whether the {@code i}th point round is on the left, a mirrored one's. */
    private boolean left(final int i) {
        return mirrored && i >= longerons.size();
    }

    /** Whether the {@code i}th point round is on its middle line: a mirrored one's keel or spine. */
    private boolean middle(final int i) {
        return mirrored && (i == 0 || i == longerons.size() - 1);
    }

    /** The {@code i}th point round {@code section}. */
    private Point point(final Section section, final int i) {
        final double[] p = section.points().get(longeron(i));
        return new Point(left(i) ? -p[0] : middle(i) ? 0 : p[0], section.along(), p[1]);
    }

    /** The {@code i}th point round's vertex's name, at section {@code k}. */
    private String vertexName(final int i, final int k) {
        return longerons.get(longeron(i)) + (middle(i) || !mirrored ? "" : left(i) ? " left" : " right") + " " + k;
    }

    // ---------------------------------------------------------------- its skeleton and its skin

    /**
     * Its skeleton, its lines plan: a vertex where each longeron crosses each section (one, where a
     * section is a point), edges along each longeron through every section, smooth, and round each
     * section given (not those put between).
     */
    public Form skeleton() {
        final Form.Builder form = new Form.Builder(name);
        final List<Section> all = refined();
        int[] last = null;
        for (int k = 0; k < all.size(); k++) {
            final Section section = all.get(k);
            final int[] at = new int[round()];
            if (section.point()) {
                final Point p = point(section, 0);
                final int v = form.vertex((k == 0 ? "tail" : "nose") + " " + name, p.across(), p.along(), p.up());
                java.util.Arrays.fill(at, v);
            } else {
                for (int i = 0; i < round(); i++) {
                    final Point p = point(section, i);
                    at[i] = form.vertex(vertexName(i, k), p.across(), p.along(), p.up());
                }
                // round it only where a section was given: its lines plan, not every section put between
                if (k % (steps + 1) == 0) {
                    form.loop(at);
                }
            }
            if (last != null) {
                for (int i = 0; i < round(); i++) {
                    if (last[i] != at[i] && (i == 0 || last[i] != last[i - 1] || at[i] != at[i - 1])) {
                        form.edge(last[i], at[i]);
                    }
                }
            }
            last = at;
        }
        return form.build();
    }

    /**
     * Its skin, dressed on its skeleton: a flat panel between each two neighbouring longerons and each
     * two neighbouring sections (three-cornered where a section is a point), each band's as
     * {@code dress} says.
     */
    public List<Panel> panels(final Dress dress) {
        final List<Section> all = refined();
        final List<Panel> panels = new ArrayList<>();
        for (int k = 0; k + 1 < all.size(); k++) {
            final Section a = all.get(k);
            final Section b = all.get(k + 1);
            final double along = (a.along() + b.along()) / 2;
            for (int i = 0; i < round(); i++) {
                final int j = (i + 1) % round();
                final String band = band(i, j);
                final Paint paint = dress.paint(band, along);
                final String named = band + (left(i) || left(j) ? " left " : mirrored ? " right " : " ") + k;
                if (a.point()) {
                    panels.add(new Panel(named, paint, List.of(point(a, i), point(b, j), point(b, i))));
                } else if (b.point()) {
                    panels.add(new Panel(named, paint, List.of(point(a, i), point(a, j), point(b, i))));
                } else {
                    panels.add(new Panel(named, paint, List.of(point(a, i), point(a, j), point(b, j), point(b, i))));
                }
            }
        }
        return panels;
    }

    /** The band between the {@code i}th and {@code j}th points round: its two longerons' names, lower first. */
    private String band(final int i, final int j) {
        final int x = longeron(i);
        final int y = longeron(j);
        return longerons.get(Math.min(x, y)) + " to " + longerons.get(Math.max(x, y));
    }

    /** Where longeron {@code longeron} (its right side's, if mirrored) is, {@code along} the body: between the sections either side. */
    public Point on(final String longeron, final double along) {
        final int i = longerons.indexOf(longeron);
        if (i < 0) {
            throw new IllegalArgumentException(name + " has no longeron " + longeron);
        }
        final List<Section> all = refined();
        for (int k = 1; k < all.size(); k++) {
            final Section b = all.get(k);
            if (along <= b.along() || k == all.size() - 1) {
                final Section a = all.get(k - 1);
                final double t = Math.clamp((along - a.along()) / (b.along() - a.along()), 0, 1);
                final double[] p = a.points().get(i);
                final double[] q = b.points().get(i);
                return new Point(lerp(p[0], q[0], t), along, lerp(p[1], q[1], t));
            }
        }
        throw new IllegalStateException();
    }

    /** How far it reaches each way. */
    public Extent extent() {
        final List<Point> points = new ArrayList<>();
        for (final Section section : refined()) {
            for (int i = 0; i < round(); i++) {
                points.add(point(section, i));
            }
        }
        return Extent.of(points);
    }
}
