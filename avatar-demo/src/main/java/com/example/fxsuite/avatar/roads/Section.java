package com.example.fxsuite.avatar.roads;

import java.util.ArrayList;
import java.util.List;

/**
 * A kind of road across: its strips either side of its line, from the line outward ({@code left}, {@code right}),
 * each so wide, built so, falling so; what it asks of the ground ({@link Earthworks}); how steep it may be at most
 * ({@code grade}, rise over run) and over how long a length its grade changes ({@code vertical}, cm).
 *
 * <p>Its line is where its heights are given: its crown, on a road falling both ways from its middle. Across is
 * measured from it, to the right going along it; up from its level there.
 */
public record Section(String name, List<Strip> left, List<Strip> right, Earthworks earthworks, double grade,
                      double vertical) {

    /** A strip as it lies across: on which side ({@code side}, -1 left, +1 right), from {@code inner} to {@code outer} off the line, its surface so high at each. */
    public record Laid(Strip strip, int side, double inner, double outer, double innerTop, double outerTop) {

        /** Where its inner edge is across. */
        public double innerAcross() {
            return side * inner;
        }

        /** Where its outer edge is across. */
        public double outerAcross() {
            return side * outer;
        }

        /** Its surface's height so far ({@code off}) from the line, on its side, between its edges. */
        public double top(final double off) {
            return innerTop - strip.fall() * (off - inner);
        }
    }

    /** A line painted along it: centred so far across, so wide; in dashes so long with gaps so long, or solid ({@code dash} nothing). */
    public record Marking(double across, double width, double dash, double gap) {

        public boolean solid() {
            return dash <= 0;
        }
    }

    /** A centre line's, a lane line's and an edge line's width, and their dashes and gaps. */
    static final double LINE = 10;
    static final double EDGE_LINE = 15;
    static final double CENTRE_DASH = 300;
    static final double CENTRE_GAP = 600;
    static final double LANE_DASH = 300;
    static final double LANE_GAP = 900;

    public Section {
        if (name == null || left == null || right == null || left.isEmpty() && right.isEmpty() || earthworks == null
                || !(grade > 0) || !(vertical >= 0)) {
            throw new IllegalArgumentException("a road across has a name, strips, earthworks and its limits");
        }
        left = List.copyOf(left);
        right = List.copyOf(right);
    }

    /** The strips one side ({@code side}, -1 left, +1 right), from the line outward. */
    public List<Strip> side(final int side) {
        return side < 0 ? left : right;
    }

    /** How far its outer edge is from the line on one side. */
    public double edge(final int side) {
        double edge = 0;
        for (final Strip s : side(side)) {
            edge += s.width();
        }
        return edge;
    }

    /** How wide it is, edge to edge. */
    public double width() {
        return edge(-1) + edge(1);
    }

    // ---- as it is changed: each a kind like it, but for one thing

    /** How many lanes it has each way: the most on either side. */
    public int lanes() {
        return (int) Math.max(left.stream().filter(s -> s.use() == Strip.Use.LANE).count(),
                right.stream().filter(s -> s.use() == Strip.Use.LANE).count());
    }

    /**
     * Like it, with {@code count} lanes each way: on each side that has lanes, the outermost repeated beside itself,
     * or the outermost taken off, until it has so many. A side with none has none still.
     */
    public Section withLanes(final int count) {
        if (count < 1) {
            throw new IllegalArgumentException("a road has a lane at least, each way it goes");
        }
        return new Section(name, laned(left, count), laned(right, count), earthworks, grade, vertical);
    }

    private static List<Strip> laned(final List<Strip> side, final int count) {
        final List<Strip> strips = new ArrayList<>(side);
        int last = -1;
        int lanes = 0;
        for (int k = 0; k < strips.size(); k++) {
            if (strips.get(k).use() == Strip.Use.LANE) {
                last = k;
                lanes++;
            }
        }
        if (lanes == 0) {
            return strips;
        }
        while (lanes < count) {
            strips.add(last + 1, strips.get(last).withRise(0));
            last++;
            lanes++;
        }
        while (lanes > count) {
            strips.remove(last);
            lanes--;
            last--;
            while (strips.get(last).use() != Strip.Use.LANE) {
                last--;
            }
        }
        return strips;
    }

    /** How wide its first strip for {@code use} is; not a number if it has none. */
    public double width(final Strip.Use use) {
        return all().stream().filter(s -> s.use() == use).mapToDouble(Strip::width).findFirst().orElse(Double.NaN);
    }

    /** How much its first strip for {@code use} falls; not a number if it has none. */
    public double fall(final Strip.Use use) {
        return all().stream().filter(s -> s.use() == use).mapToDouble(Strip::fall).findFirst().orElse(Double.NaN);
    }

    /** What its strips are for, each once, in the order they come from its left edge. */
    public List<Strip.Use> uses() {
        return laid().stream().map(l -> l.strip().use()).distinct().toList();
    }

    /** Like it, every strip for {@code use} {@code width} wide. */
    public Section withWidth(final Strip.Use use, final double width) {
        return new Section(name, changed(left, use, s -> s.withWidth(width)), changed(right, use, s -> s.withWidth(width)),
                earthworks, grade, vertical);
    }

    /** Like it, every strip for {@code use} falling {@code fall}. */
    public Section withFall(final Strip.Use use, final double fall) {
        return new Section(name, changed(left, use, s -> s.withFall(fall)), changed(right, use, s -> s.withFall(fall)),
                earthworks, grade, vertical);
    }

    /**
     * Like it, its carriageway (its lanes and shoulders) falling {@code left} going away from the line on its left and
     * {@code right} on its right: tilted, or its crown on its way out. Its kerbs, footways and ground as they were, each
     * from where the carriageway leaves off.
     */
    public Section withFalls(final double left, final double right) {
        return new Section(name, fallen(this.left, left), fallen(this.right, right), earthworks, grade, vertical);
    }

    private static List<Strip> fallen(final List<Strip> side, final double fall) {
        return side.stream().map(s -> s.use() == Strip.Use.LANE || s.use() == Strip.Use.SHOULDER ? s.withFall(fall) : s)
                .toList();
    }

    /**
     * Like it, a paved strip {@code width} wide more on one side ({@code side}), just past its paved strips there: the gore
     * between it and a road it leaves or joins, paved up to its nose. Built and falling as the paved strip next to it is.
     */
    public Section withGore(final int side, final double width) {
        final List<Strip> strips = new ArrayList<>(side(side));
        int at = 0;
        Strip like = null;
        for (int k = 0; k < strips.size(); k++) {
            if (strips.get(k).paved()) {
                at = k + 1;
                like = strips.get(k);
            }
        }
        if (like == null) {
            // none paved this side: as the first paved strip on the other, falling the other way
            final Strip other = side(-side).stream().filter(Strip::paved).findFirst().orElse(null);
            if (other == null) {
                return this;
            }
            like = other.withFall(-other.fall());
        }
        strips.add(at, Strip.paved(Strip.Use.SHOULDER, width, like.courses(), like.fall(), 0));
        return side < 0 ? new Section(name, strips, right, earthworks, grade, vertical)
                : new Section(name, left, strips, earthworks, grade, vertical);
    }

    /** The narrowest a strip is drawn, eased in or out: so a road changing kind keeps its strips, however narrow. */
    static final double NARROWEST = 0.5;

    /**
     * Between {@code a} and {@code b}, {@code t} of the way from the one to the other (0 to 1): each side's strips matched
     * in their groups, those inside its lanes, its lanes, those outside them, each eased from the one's to the other's;
     * a lane more grows from nothing, a lane fewer shrinks to nothing, each a shoulder until it is a lane again, so no
     * lane is painted that is not yet one. Its limits and its earthworks the nearer's.
     */
    public static Section blend(final Section a, final Section b, final double t) {
        if (t <= 0) {
            return a;
        }
        if (t >= 1) {
            return b;
        }
        final Section near = t < 0.5 ? a : b;
        return new Section(near.name, blended(a.left, b.left, t), blended(a.right, b.right, t), near.earthworks,
                near.grade, near.vertical);
    }

    private static List<Strip> blended(final List<Strip> a, final List<Strip> b, final double t) {
        final List<List<Strip>> ga = groups(a);
        final List<List<Strip>> gb = groups(b);
        final List<Strip> out = new ArrayList<>();
        for (int g = 0; g < 3; g++) {
            final List<Strip> sa = ga.get(g);
            final List<Strip> sb = gb.get(g);
            for (int k = 0; k < Math.max(sa.size(), sb.size()); k++) {
                final Strip x = k < sa.size() ? sa.get(k) : null;
                final Strip y = k < sb.size() ? sb.get(k) : null;
                out.add(eased(x, y, t));
            }
        }
        return out;
    }

    /** A side's strips in three: those inside its lanes, its lanes, those outside them. */
    private static List<List<Strip>> groups(final List<Strip> side) {
        int first = -1;
        int last = -1;
        for (int k = 0; k < side.size(); k++) {
            if (side.get(k).use() == Strip.Use.LANE) {
                first = first < 0 ? k : first;
                last = k;
            }
        }
        if (first < 0) {
            return List.of(side, List.of(), List.of());
        }
        return List.of(side.subList(0, first), side.subList(first, last + 1), side.subList(last + 1, side.size()));
    }

    /** A strip {@code t} of the way from {@code x} to {@code y}, either missing: growing from nothing, or shrinking to it. */
    private static Strip eased(final Strip x, final Strip y, final double t) {
        if (x == null || y == null) {
            final Strip s = x == null ? y : x;
            final double share = x == null ? t : 1 - t;
            final Strip.Use use = s.use() == Strip.Use.LANE ? Strip.Use.SHOULDER : s.use();
            return new Strip(use, Math.max(NARROWEST, s.width() * share), s.courses(), s.cover(), s.fall(), s.rise() * share);
        }
        final Strip like = t < 0.5 ? x : y;
        return new Strip(like.use(), Math.max(NARROWEST, x.width() + (y.width() - x.width()) * t), like.courses(),
                like.cover(), x.fall() + (y.fall() - x.fall()) * t, x.rise() + (y.rise() - x.rise()) * t);
    }

    /** Like it, asking {@code earthworks} of the ground. */
    public Section withEarthworks(final Earthworks earthworks) {
        return new Section(name, left, right, earthworks, grade, vertical);
    }

    /** Like it, no steeper than {@code grade}, its grade changing over {@code vertical}. */
    public Section withLimits(final double grade, final double vertical) {
        return new Section(name, left, right, earthworks, grade, vertical);
    }

    private List<Strip> all() {
        final List<Strip> all = new ArrayList<>(left.reversed());
        all.addAll(right);
        return all;
    }

    private static List<Strip> changed(final List<Strip> side, final Strip.Use use,
                                       final java.util.function.UnaryOperator<Strip> change) {
        return side.stream().map(s -> s.use() == use ? change.apply(s) : s).toList();
    }

    /** Its strips as they lie, from its left edge to its right. */
    public List<Laid> laid() {
        final List<Laid> laid = new ArrayList<>(laid(-1).reversed());
        laid.addAll(laid(1));
        return laid;
    }

    /** One side's strips as they lie, from the line outward. */
    public List<Laid> laid(final int side) {
        final List<Laid> laid = new ArrayList<>();
        double off = 0;
        double top = 0;
        for (final Strip s : side(side)) {
            top += s.rise();
            final double outerTop = top - s.fall() * s.width();
            laid.add(new Laid(s, side, off, off + s.width(), top, outerTop));
            off += s.width();
            top = outerTop;
        }
        return laid;
    }

    /** The strip as it lies {@code across}; the outermost, beyond the edge; null, on a side with none. */
    public Laid at(final double across) {
        final List<Laid> side = laid(across < 0 ? -1 : 1);
        if (side.isEmpty()) {
            return null;
        }
        final double off = Math.abs(across);
        for (final Laid l : side) {
            if (off <= l.outer()) {
                return l;
            }
        }
        return side.getLast();
    }

    /** How high its surface is {@code across}, over the line's level: its falls and its steps. Beyond its edge, as at it. */
    public double top(final double across) {
        final Laid l = at(across);
        return l == null ? 0 : l.top(Math.min(Math.abs(across), l.outer()));
    }

    /** How deep its courses are {@code across}. */
    public double depth(final double across) {
        final Laid l = at(across);
        return l == null ? 0 : l.strip().depth();
    }

    /** How high its formation is {@code across}, over the line's level: its surface, less its courses. */
    public double formation(final double across) {
        return top(across) - depth(across);
    }

    /** The ground's cover {@code across}, where a ground strip is; null over a paved one, or a side with none. */
    public Cover cover(final double across) {
        final Laid l = at(across);
        return l == null ? null : l.strip().cover();
    }

    /** Its paved strips as they lie, from its left edge to its right: what a structure carries. */
    public List<Laid> paved() {
        return laid().stream().filter(l -> l.strip().paved()).toList();
    }

    /** How far the outer edge of its outermost paved strip is from the line on one side; nothing if it has none there. */
    public double pavedEdge(final int side) {
        double edge = 0;
        for (final Laid l : laid(side)) {
            if (l.strip().paved()) {
                edge = l.outer();
            }
        }
        return edge;
    }

    /** The one side its paved strips are on, -1 or +1: a carriageway's; 0 if they are on both, or neither. */
    public int pavedSide() {
        final boolean l = left.stream().anyMatch(Strip::paved);
        final boolean r = right.stream().anyMatch(Strip::paved);
        return l == r ? 0 : l ? -1 : 1;
    }

    /**
     * The lines painted on it, worked out from its strips: a dashed centre line where lanes meet at the line; a
     * dashed lane line between lanes side by side; a solid edge line along a lane's edge where it meets anything
     * but a lane, just inside the lane.
     */
    public List<Marking> markings() {
        final List<Marking> lines = new ArrayList<>();
        final Strip l0 = left.isEmpty() ? null : left.getFirst();
        final Strip r0 = right.isEmpty() ? null : right.getFirst();
        if (lane(l0) && lane(r0)) {
            lines.add(new Marking(0, LINE, CENTRE_DASH, CENTRE_GAP));
        } else {
            if (lane(l0)) {
                lines.add(new Marking(-EDGE_LINE / 2, EDGE_LINE, 0, 0));
            }
            if (lane(r0)) {
                lines.add(new Marking(EDGE_LINE / 2, EDGE_LINE, 0, 0));
            }
        }
        for (final int side : new int[] {-1, 1}) {
            final List<Strip> strips = side(side);
            double off = 0;
            for (int k = 0; k < strips.size(); k++) {
                final Strip s = strips.get(k);
                off += s.width();
                final Strip next = k + 1 < strips.size() ? strips.get(k + 1) : null;
                if (lane(s) && lane(next)) {
                    lines.add(new Marking(side * off, LINE, LANE_DASH, LANE_GAP));
                } else if (lane(s)) {
                    lines.add(new Marking(side * (off - EDGE_LINE / 2), EDGE_LINE, 0, 0));
                } else if (lane(next)) {
                    lines.add(new Marking(side * (off + EDGE_LINE / 2), EDGE_LINE, 0, 0));
                }
            }
        }
        lines.sort(java.util.Comparator.comparingDouble(Marking::across));
        return lines;
    }

    private static boolean lane(final Strip s) {
        return s != null && s.use() == Strip.Use.LANE;
    }
}
