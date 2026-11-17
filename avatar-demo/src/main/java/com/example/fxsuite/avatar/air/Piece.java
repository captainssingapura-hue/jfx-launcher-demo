package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.List;

/**
 * A solid part of an aircraft, as a building's parts are ({@link com.example.fxsuite.avatar.build.Solid}):
 * eight corners, corner {@code k} at the low or high end of its first edge as bit 0 of {@code k}
 * says, of its second as bit 1, of its third as bit 2; not necessarily square, so a wedge, a
 * tapered spar, a blade twisted along its length are each one. Drawn flat, face by face.
 *
 * @param name    what it is
 * @param paint   what it is finished in
 * @param corners its eight corners
 */
public record Piece(String name, Paint paint, List<Point> corners) {

    public Piece {
        corners = List.copyOf(corners);
        if (corners.size() != 8) {
            throw new IllegalArgumentException(name + ": a piece has eight corners, not " + corners.size());
        }
    }

    /**
     * The piece between two quadrilaterals, {@code a} at one end and {@code b} at the other, each
     * given round in order (the same way round), {@code b}'s points each over {@code a}'s.
     */
    public static Piece between(final String name, final Paint paint, final Point[] a, final Point[] b) {
        return new Piece(name, paint, List.of(a[0], a[1], a[3], a[2], b[0], b[1], b[3], b[2]));
    }

    /** A box about {@code middle}, square to the aircraft, {@code across}, {@code along} and {@code up} through. */
    public static Piece box(final String name, final Paint paint, final Point middle, final double across,
                            final double along, final double up) {
        final Point[] corners = new Point[8];
        for (int k = 0; k < 8; k++) {
            corners[k] = middle.plus(new Point((k & 1) == 0 ? -across / 2 : across / 2,
                    (k & 2) == 0 ? -along / 2 : along / 2, (k & 4) == 0 ? -up / 2 : up / 2));
        }
        return new Piece(name, paint, List.of(corners));
    }

    /**
     * A bar from {@code a} to {@code b}, {@code wide} across its run (square to it, level as far as
     * it can be) and {@code deep} the other way: a strut, a spar, an arm.
     */
    public static Piece bar(final String name, final Paint paint, final Point a, final Point b, final double wide,
                            final double deep) {
        final Point run = b.plus(a.times(-1));
        final double length = run.length();
        final Point unit = run.times(1 / length);
        // square to the run, across it as level as can be; and square to both
        final Point level = Math.abs(unit.up()) > 0.95 ? new Point(1, 0, 0) : new Point(0, 0, 1);
        final Point side = unit(cross(unit, level));
        final Point other = unit(cross(side, unit));
        final Point s = side.times(wide / 2);
        final Point o = other.times(deep / 2);
        final Point[] end = {s.plus(o), s.plus(o.times(-1)), s.times(-1).plus(o.times(-1)), s.times(-1).plus(o)};
        return between(name, paint, new Point[] {a.plus(end[0]), a.plus(end[1]), a.plus(end[2]), a.plus(end[3])},
                new Point[] {b.plus(end[0]), b.plus(end[1]), b.plus(end[2]), b.plus(end[3])});
    }

    static Point cross(final Point a, final Point b) {
        return new Point(a.along() * b.up() - a.up() * b.along(), a.up() * b.across() - a.across() * b.up(),
                a.across() * b.along() - a.along() * b.across());
    }

    static Point unit(final Point p) {
        return p.times(1 / p.length());
    }

    /** The same piece, mounted ({@link Mount}). */
    public Piece placed(final Mount mount) {
        return new Piece(name, paint, corners.stream().map(mount::place).toList());
    }

    /** The same piece on the other side: across turned to the left. */
    public Piece mirrored() {
        return new Piece(name, paint, corners.stream().map(Skin::mirror).toList());
    }
}
