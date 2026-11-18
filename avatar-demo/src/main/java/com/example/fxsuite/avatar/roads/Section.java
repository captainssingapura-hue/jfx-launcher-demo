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
