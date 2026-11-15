package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * The solid parts an aircraft's bodies carry, each made in a frame of its own and mounted
 * ({@link Mount}): wings and fins, rotors and propellers, ducts, wheels. Pieces ({@link Piece}),
 * as a building's parts are.
 */
public final class Parts {

    private Parts() {
    }

    /**
     * An aerofoil's section, a share of its chord back from its leading edge and of its thickness up
     * from its chord line: rounded at the front, thickest a third back, drawn to an edge behind.
     */
    private static final double[][] AEROFOIL = {
            {0, 0}, {0.08, 0.42}, {0.32, 0.5}, {0.62, 0.32}, {1, 0}, {0.62, -0.14}, {0.32, -0.2}, {0.08, -0.18}};

    /**
     * A wing from its root, at the origin, out along +across {@code span} to its tip: its chord
     * {@code rootChord} at the root, {@code tipChord} at the tip, back from a leading edge swept back
     * {@code sweep} degrees and raised {@code dihedral} degrees; {@code thickness} its depth, a
     * share of its chord. Its leading edge is at along 0 at its root. A fin is a wing mounted on
     * its side (rolled -90: its span up).
     */
    public static List<Piece> wing(final String name, final Paint paint, final double span, final double rootChord,
                                   final double tipChord, final double sweep, final double dihedral,
                                   final double thickness) {
        final Point[] root = section(new Point(0, 0, 0), rootChord, thickness);
        final Point[] tip = section(new Point(span, -span * Math.tan(Math.toRadians(sweep)),
                span * Math.tan(Math.toRadians(dihedral))), tipChord, thickness);
        // the section's eight points, front to back over the top and back under it, as three pieces
        // between root and tip: the rounded nose, the thick middle, the thin trailing part
        final int[][] quads = {{0, 1, 2, 7}, {2, 3, 5, 6}, {3, 4, 4, 5}};
        final List<Piece> pieces = new ArrayList<>();
        for (final int[] q : quads) {
            pieces.add(Piece.between(name, paint, new Point[] {root[q[0]], root[q[1]], root[q[2]], root[q[3]]},
                    new Point[] {tip[q[0]], tip[q[1]], tip[q[2]], tip[q[3]]}));
        }
        // the middle of the nose, between the front piece and the middle one: from 7 back to 6
        pieces.add(Piece.between(name, paint, new Point[] {root[7], root[2], root[6], root[6]},
                new Point[] {tip[7], tip[2], tip[6], tip[6]}));
        return pieces;
    }

    private static Point[] section(final Point leading, final double chord, final double thickness) {
        final Point[] points = new Point[AEROFOIL.length];
        for (int k = 0; k < AEROFOIL.length; k++) {
            points[k] = leading.plus(new Point(0, -AEROFOIL[k][0] * chord, AEROFOIL[k][1] * thickness * chord));
        }
        return points;
    }

    /**
     * A rotor about its hub at the origin, turning about +up: {@code blades} blades {@code radius}
     * out, each {@code chord} wide at its root and three fifths of it at its tip, pitched
     * {@code pitch} degrees at the root and a third of that at the tip, drooping {@code droop}
     * degrees; and its hub. A propeller is a rotor mounted nose-down (pitched -90: its axis ahead).
     */
    public static List<Piece> rotor(final String name, final Paint paint, final Paint hub, final int blades,
                                    final double radius, final double chord, final double pitch, final double droop) {
        final List<Piece> pieces = new ArrayList<>();
        final double root = Math.max(chord * 0.6, radius * 0.08);
        for (int k = 0; k < blades; k++) {
            final Mount turned = new Mount(Point.ZERO, 360.0 * k / blades, 0, 0);
            pieces.add(blade(name + " blade " + k, paint, root, radius, chord, pitch, droop).placed(turned));
        }
        pieces.add(Piece.box(name + " hub", hub, Point.ZERO, root * 1.6, root * 1.6, chord * 0.5));
        return pieces;
    }

    /** One blade out along +across from {@code from} to {@code to}, twisted from root to tip. */
    private static Piece blade(final String name, final Paint paint, final double from, final double to,
                               final double chord, final double pitch, final double droop) {
        final double thick = chord * 0.12;
        final Point[] root = bladeSection(from, chord, thick, pitch, from, droop);
        final Point[] tip = bladeSection(to, chord * 0.6, thick * 0.6, pitch / 3, from, droop);
        return Piece.between(name, paint, root, tip);
    }

    private static Point[] bladeSection(final double out, final double chord, final double thick, final double pitch,
                                        final double from, final double droop) {
        final double p = Math.toRadians(pitch);
        final double drop = -(out - from) * Math.tan(Math.toRadians(droop));
        final double[][] corners = {{chord / 2, thick / 2}, {-chord / 2, thick / 2}, {-chord / 2, -thick / 2},
                {chord / 2, -thick / 2}};
        final Point[] points = new Point[4];
        for (int k = 0; k < 4; k++) {
            // the section turned by the pitch, its leading edge (ahead) raised
            final double along = corners[k][0] * Math.cos(p) - corners[k][1] * Math.sin(p);
            final double up = corners[k][0] * Math.sin(p) + corners[k][1] * Math.cos(p);
            points[k] = new Point(out, along, up + drop);
        }
        return points;
    }

    /**
     * Along a propeller's blade, root to tip, a share of its radius out: how wide it is there (a share
     * of its broadest chord), and how far it is pitched there (a share of its pitch at the root). A
     * narrow round shank at the hub, broadest a third of the way out, tapering to a rounded tip; its
     * pitch easing off toward the tip, as a propeller's twist does.
     */
    private static final double[][] BLADE = {
            {0.12, 0.32, 1.25}, {0.2, 0.55, 1.15}, {0.34, 1, 1}, {0.55, 0.92, 0.8}, {0.75, 0.75, 0.62},
            {0.9, 0.52, 0.5}, {0.97, 0.3, 0.45}, {1, 0.1, 0.45}};

    /**
     * A propeller about its hub at the origin, turning about +up, as a rotor ({@link #rotor}): its
     * {@code blades} blades {@code radius} out, {@code chord} at their broadest, pitched {@code pitch}
     * degrees at their roots: each blade shaped as one is, root to tip ({@link #BLADE}), a thin
     * aerofoil in section; on a hub. Mounted nose-down (pitched -90), its axis ahead.
     */
    public static List<Piece> propeller(final String name, final Paint paint, final Paint hub, final int blades,
                                        final double radius, final double chord, final double pitch) {
        final List<Piece> pieces = new ArrayList<>();
        for (int k = 0; k < blades; k++) {
            final Mount turned = new Mount(Point.ZERO, 360.0 * k / blades, 0, 0);
            Point[] last = null;
            for (final double[] station : BLADE) {
                final Point[] section = aerofoil(station[0] * radius, station[1] * chord, pitch * station[2]);
                if (last != null) {
                    pieces.add(Piece.between(name + " blade " + k, paint, last, section).placed(turned));
                }
                last = section;
            }
        }
        pieces.add(Piece.box(name + " hub", hub, Point.ZERO, radius * 0.22, radius * 0.22, chord * 0.7));
        return pieces;
    }

    /**
     * A blade's section {@code out} along it (+across), {@code chord} wide and a ninth as thick: its
     * leading edge, top, trailing edge and bottom, turned by {@code pitch} degrees, its leading edge
     * (ahead, +along) raised.
     */
    private static Point[] aerofoil(final double out, final double chord, final double pitch) {
        final double p = Math.toRadians(pitch);
        final double t = chord / 9;
        final double[][] corners = {{chord / 2, 0}, {chord * 0.1, t / 2}, {-chord / 2, 0}, {chord * 0.1, -t / 2}};
        final Point[] points = new Point[4];
        for (int k = 0; k < 4; k++) {
            final double along = corners[k][0] * Math.cos(p) - corners[k][1] * Math.sin(p);
            final double up = corners[k][0] * Math.sin(p) + corners[k][1] * Math.cos(p);
            points[k] = new Point(out, along, up);
        }
        return points;
    }

    /**
     * A streamlined strut from {@code a} to {@code b}: an aerofoil in section, {@code chord} long, its
     * leading edge ahead (as near ahead as its run lets it point), {@code chord} by a quarter of it
     * thick: a pylon, an outrigger, a hanger.
     */
    public static Piece strut(final String name, final Paint paint, final Point a, final Point b, final double chord) {
        final Point run = Piece.unit(b.plus(a.times(-1)));
        // ahead, square to the run: its chord's line; and square to both, its thickness
        final Point forward = new Point(0, 1, 0);
        Point ahead = forward.plus(run.times(-forward.dot(run)));
        if (ahead.length() < 1e-6) {
            ahead = new Point(0, 0, 1).plus(run.times(-run.up()));
        }
        ahead = Piece.unit(ahead);
        final Point side = Piece.unit(Piece.cross(run, ahead));
        final double t = chord / 4;
        final Point[] section = {ahead.times(chord / 2), side.times(t / 2).plus(ahead.times(chord * 0.1)),
                ahead.times(-chord / 2), side.times(-t / 2).plus(ahead.times(chord * 0.1))};
        return Piece.between(name, paint,
                new Point[] {a.plus(section[0]), a.plus(section[1]), a.plus(section[2]), a.plus(section[3])},
                new Point[] {b.plus(section[0]), b.plus(section[1]), b.plus(section[2]), b.plus(section[3])});
    }

    /**
     * A ring about +up at the origin, {@code radius} to its inside, {@code wall} thick and
     * {@code depth} deep, in {@code sides} pieces: a ducted fan's duct, a wheel's rim.
     */
    public static List<Piece> ring(final String name, final Paint paint, final double radius, final double wall,
                                   final double depth, final int sides) {
        final List<Piece> pieces = new ArrayList<>();
        for (int k = 0; k < sides; k++) {
            final double a = 2 * Math.PI * k / sides;
            final double b = 2 * Math.PI * (k + 1) / sides;
            final Point[] lower = {at(radius, a, -depth / 2), at(radius + wall, a, -depth / 2),
                    at(radius + wall, b, -depth / 2), at(radius, b, -depth / 2)};
            final Point[] upper = {at(radius, a, depth / 2), at(radius + wall, a, depth / 2),
                    at(radius + wall, b, depth / 2), at(radius, b, depth / 2)};
            pieces.add(Piece.between(name, paint, lower, upper));
        }
        return pieces;
    }

    /** A wheel about +across at the origin, {@code radius} round and {@code width} wide: its tyre, and its hub. */
    public static List<Piece> wheel(final String name, final Paint tyre, final Paint hub, final double radius,
                                    final double width) {
        final Mount side = new Mount(Point.ZERO, 0, 0, 90);
        final List<Piece> pieces = new ArrayList<>();
        ring(name, tyre, radius * 0.55, radius * 0.45, width, 12).forEach(p -> pieces.add(p.placed(side)));
        pieces.add(Piece.box(name + " hub", hub, Point.ZERO, width * 0.8, radius * 1.1, radius * 1.1));
        return pieces;
    }

    /**
     * A patch on {@code body}'s skin, in its frame: from {@code from} to {@code to} along it and from
     * {@code start} to {@code end} degrees round it ({@link Body#surface}), standing {@code proud} out
     * of it, in {@code alongSteps} by {@code aroundSteps} pieces, so it lies on the skin as it
     * curves: a window, a stripe, an insignia; all the way round, a band.
     */
    public static List<Piece> patch(final String name, final Paint paint, final Body body, final double from,
                                    final double to, final double start, final double end, final double proud,
                                    final int alongSteps, final int aroundSteps) {
        final List<Piece> pieces = new ArrayList<>();
        for (int i = 0; i < alongSteps; i++) {
            for (int j = 0; j < aroundSteps; j++) {
                final double a0 = from + (to - from) * i / alongSteps;
                final double a1 = from + (to - from) * (i + 1) / alongSteps;
                final double r0 = start + (end - start) * j / aroundSteps;
                final double r1 = start + (end - start) * (j + 1) / aroundSteps;
                final double[][] corners = {{a0, r0}, {a1, r0}, {a1, r1}, {a0, r1}};
                final Point[] under = new Point[4];
                final Point[] over = new Point[4];
                for (int k = 0; k < 4; k++) {
                    final Point on = body.surface(corners[k][0], corners[k][1]);
                    final Point out = outward(body, corners[k][0], corners[k][1]);
                    // a hair below the skin, so no gap shows at its edge; proud of it above
                    under[k] = on.plus(out.times(-0.5));
                    over[k] = on.plus(out.times(proud));
                }
                pieces.add(Piece.between(name, paint, under, over));
            }
        }
        return pieces;
    }

    /** Which way is out of {@code body}'s skin at a point on it: square to it, by its slope each way there. */
    static Point outward(final Body body, final double along, final double around) {
        final double d = 0.5;
        final Point ahead = body.surface(along + d, around).plus(body.surface(along - d, around).times(-1));
        final Point round = body.surface(along, around + d).plus(body.surface(along, around - d).times(-1));
        Point n = Piece.cross(round, ahead);
        // and away from its middle, whichever way the cross product came out
        final Point here = body.surface(along, around);
        final Point middle = new Point(0, along, body instanceof Fuselage f ? f.section(along).up() : 0);
        final Point away = here.plus(middle.times(-1));
        if (n.length() < 1e-9) {
            n = away;
        }
        if (n.length() < 1e-9) {
            return new Point(0, Math.signum(along) == 0 ? 1 : Math.signum(along), 0);
        }
        return Piece.unit(n.dot(away) < 0 ? n.times(-1) : n);
    }

    private static Point at(final double r, final double angle, final double up) {
        return new Point(r * Math.cos(angle), r * Math.sin(angle), up);
    }
}
