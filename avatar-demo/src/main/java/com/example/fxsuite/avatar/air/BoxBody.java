package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * A box-shaped body: {@code length} long, {@code width} wide and {@code height} high about its
 * middle, every edge and corner rounded {@code round} (a box rolled round a ball of that radius:
 * its faces flat, its edges a quarter round, its corners an eighth of a ball); drawn in toward its
 * nose by {@code noseTaper} (a share of its size there) and toward its tail by {@code tailTaper},
 * straight from its middle. A drone's body, a boxy helicopter's, a gondola, an engine's housing.
 *
 * @param name      what it is
 * @param paint     what it is finished in
 * @param length    nose to tail
 * @param width     side to side, at its middle
 * @param height    top to bottom, at its middle
 * @param round     how far its edges are rounded
 * @param noseTaper how much smaller it is at its nose, a share (0, none)
 * @param tailTaper how much smaller at its tail
 * @param chamfered whether its edges and corners are cut flat ({@code round} across), not rounded: drawn
 *                  flat, face by face; angular, as a military body is
 */
public record BoxBody(String name, Paint paint, double length, double width, double height, double round,
                      double noseTaper, double tailTaper, boolean chamfered) implements Body {

    /** Its edges rounded. */
    public BoxBody(final String name, final Paint paint, final double length, final double width, final double height,
                   final double round, final double noseTaper, final double tailTaper) {
        this(name, paint, length, width, height, round, noseTaper, tailTaper, false);
    }

    /** The same, its edges and corners cut flat. */
    public BoxBody withChamfers() {
        return new BoxBody(name, paint, length, width, height, round, noseTaper, tailTaper, true);
    }

    /** Points round each corner of a section; sections round each end's rounded edge. */
    private static final int CORNER = 5;
    private static final int EDGE = 4;

    public BoxBody {
        if (!(length > 0 && width > 0 && height > 0) || round < 0 || noseTaper < 0 || tailTaper < 0
                || noseTaper >= 1 || tailTaper >= 1) {
            throw new IllegalArgumentException(name + ": a box " + length + " by " + width + " by " + height);
        }
        final double least = Math.min(width, height) * (1 - Math.max(noseTaper, tailTaper));
        if (2 * round > Math.min(least, length)) {
            throw new IllegalArgumentException(name + ": rounded " + round + ", more than half of " + Math.min(least, length));
        }
    }

    /** Square-edged ends, rounded {@code round}, no taper. */
    public static BoxBody of(final String name, final Paint paint, final double length, final double width,
                             final double height, final double round) {
        return new BoxBody(name, paint, length, width, height, round, 0, 0);
    }

    /** How big it is at {@code along}, a share of its middle's size: drawn in straight to each end. */
    private double taper(final double along) {
        final double t = Math.clamp(along / (length / 2), -1, 1);
        return t >= 0 ? 1 - noseTaper * t : 1 + tailTaper * t;
    }

    @Override
    public Skin skin() {
        final double inner = length / 2 - round;
        final List<List<Point>> rings = new ArrayList<>();
        // the tail's flat face to a point in its middle, round its rounded edge to the full section
        rings.add(apex(-length / 2));
        final int edge = chamfered ? 1 : EDGE;
        for (int j = 0; j <= edge; j++) {
            final double phi = Math.PI / 2 * (1 - (double) j / edge);
            rings.add(section(-(inner + grownAt(phi, false)), grownAt(phi, true)));
        }
        // its middle, where it is biggest; then the same at the nose
        rings.add(section(0, round));
        for (int j = edge; j >= 0; j--) {
            final double phi = Math.PI / 2 * (1 - (double) j / edge);
            rings.add(section(inner + grownAt(phi, false), grownAt(phi, true)));
        }
        rings.add(apex(length / 2));
        return new Skin(name, paint, rings, chamfered);
    }

    /**
     * Round an end's edge, {@code phi} from its face (90°) to its side (0): how far beyond the inner
     * box it reaches along ({@code grown} false), and how far its section is grown ({@code grown} true);
     * on a quarter round, or, chamfered, straight across.
     */
    private double grownAt(final double phi, final boolean grown) {
        if (chamfered) {
            final double t = phi / (Math.PI / 2);
            return grown ? round * (1 - t) : round * t;
        }
        return grown ? round * Math.cos(phi) : round * Math.sin(phi);
    }

    private List<Point> apex(final double along) {
        final List<Point> ring = new ArrayList<>();
        for (int k = 0; k < 4 * (CORNER + 1); k++) {
            ring.add(new Point(0, along, 0));
        }
        return ring;
    }

    /** Its section at {@code along}: the inner box, tapered, grown by {@code grown} all round, its corners that round. */
    private List<Point> section(final double along, final double grown) {
        final double f = taper(along);
        final double a = width / 2 * f - round;
        final double b = height / 2 * f - round;
        final List<Point> ring = new ArrayList<>();
        final double[][] corners = {{a, b}, {-a, b}, {-a, -b}, {a, -b}};
        final int corner = chamfered ? 1 : CORNER;
        for (int c = 0; c < 4; c++) {
            for (int k = 0; k <= CORNER; k++) {
                // chamfered: the corner's points all at its two ends, a straight cut between them
                final double angle = Math.PI / 2 * (c + (double) Math.min(corner, k * corner / CORNER) / corner);
                ring.add(new Point(corners[c][0] + grown * Math.cos(angle), along, corners[c][1] + grown * Math.sin(angle)));
            }
        }
        return ring;
    }

    @Override
    public Point surface(final double along, final double around) {
        final double f = taper(along);
        final double a = width / 2 * f - round;
        final double b = height / 2 * f - round;
        final double inner = length / 2 - round;
        // over its rounded ends, the section shrinks as the ball rolled round it does
        final double beyond = Math.max(0, Math.abs(along) - inner);
        final double grown = chamfered ? Math.max(0, round - beyond) : Math.sqrt(Math.max(0, round * round - beyond * beyond));
        final double ha = a + grown;
        final double hb = b + grown;
        final double t = Math.toRadians(around);
        final double dx = Math.cos(t);
        final double dz = Math.sin(t);
        // a ray out from the middle: to a flat side, or to a rounded corner
        if (Math.abs(dx) > 1e-9) {
            final double s = ha / Math.abs(dx);
            if (Math.abs(dz * s) <= b + 1e-9) {
                return new Point(dx * s, along, dz * s);
            }
        }
        if (Math.abs(dz) > 1e-9) {
            final double s = hb / Math.abs(dz);
            if (Math.abs(dx * s) <= a + 1e-9) {
                return new Point(dx * s, along, dz * s);
            }
        }
        if (chamfered) {
            // the cut across the corner, from (±(a + grown), ±b) to (±a, ±(b + grown)): where the ray meets it
            final double sx = Math.signum(dx);
            final double sz = Math.signum(dz);
            final double px = sx * (a + grown);
            final double pz = sz * b;
            final double qx = sx * a;
            final double qz = sz * (b + grown);
            final double ex = qx - px;
            final double ez = qz - pz;
            final double s = (px * ez - pz * ex) / (dx * ez - dz * ex);
            return new Point(dx * s, along, dz * s);
        }
        // the corner's circle, about (±a, ±b): the far root of |s d - c| = grown
        final double cx = Math.signum(dx) * a;
        final double cz = Math.signum(dz) * b;
        final double dot = dx * cx + dz * cz;
        final double s = dot + Math.sqrt(Math.max(0, dot * dot - (cx * cx + cz * cz - grown * grown)));
        return new Point(dx * s, along, dz * s);
    }
}
