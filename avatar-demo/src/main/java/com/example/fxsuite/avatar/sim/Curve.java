package com.example.fxsuite.avatar.sim;

/**
 * How an edge of a form ({@link Form}) curves between its two vertices, instead of running
 * straight: points as {@link Vec3}s of across, along and up. Three kinds:
 *
 * <ul>
 *   <li>{@link Kind#ARC}: a circular arc through its two ends, its middle {@code control} (a
 *       direction, its length the arc's rise) off the middle of the straight line between them: a
 *       pointed arch's two halves, a half circle.</li>
 *   <li>{@link Kind#POWER}: bowed, its middle {@code control} off the straight line, the bow as full
 *       as {@code fullness} says: 2 a parabola, more keeping it near the line until near its ends,
 *       so the bend comes there (a spire's flare at its foot, an eave turned up at its corners); or
 *       part of one, from {@code t0} to {@code t1} of the way along the whole of it, from {@code from} to
 *       {@code to}.</li>
 *   <li>{@link Kind#CORNER}: part of a quarter superellipse into the corner {@code control}, from
 *       {@code from} to {@code to}, its squareness {@code fullness}, the part from {@code t0} to
 *       {@code t1} of the way along it (a floor plan's shaped corner, {@code build.Corner}, between
 *       two columns on it).</li>
 * </ul>
 *
 * <p>Pure figures; turned and moved with what it is part of ({@link #moved}, {@link #turned}).
 *
 * @param kind     which kind
 * @param control  an arc's or a bow's middle's offset (a direction); a corner's corner (a point)
 * @param fullness a bow's fullness; a corner's squareness
 * @param from     a corner's start (a point); null for the others
 * @param to       a corner's end (a point); null for the others
 * @param t0       how far along a corner's curve this part starts
 * @param t1       and ends
 */
public record Curve(Kind kind, Vec3 control, double fullness, Vec3 from, Vec3 to, double t0, double t1) {

    public enum Kind { ARC, POWER, CORNER }

    public Curve {
        if (kind == Kind.CORNER && (from == null || to == null)) {
            throw new IllegalArgumentException("a corner's curve runs from somewhere to somewhere");
        }
    }

    /** A circular arc, its middle {@code rise} off the straight line's middle. */
    public static Curve arc(final Vec3 rise) {
        return new Curve(Kind.ARC, rise, 2, null, null, 0, 1);
    }

    /** A bow, its middle {@code bow} off the straight line, as full as {@code fullness}. */
    public static Curve bow(final Vec3 bow, final double fullness) {
        return new Curve(Kind.POWER, bow, fullness, null, null, 0, 1);
    }

    /**
     * Part of a bow from {@code from} to {@code to}, its middle {@code bow} off the straight line, as full
     * as {@code fullness}: the part from {@code t0} to {@code t1} of the way along it, so a long bowed
     * line (an eave between its upturned corners) can be cut at points along it into edges.
     */
    public static Curve bow(final Vec3 bow, final double fullness, final Vec3 from, final Vec3 to, final double t0,
                            final double t1) {
        return new Curve(Kind.POWER, bow, fullness, from, to, t0, t1);
    }

    /** Where the bow from {@code from} to {@code to}, its middle {@code bow} off the line, is {@code t} of the way along. */
    public static Vec3 bowAt(final Vec3 bow, final double fullness, final Vec3 from, final Vec3 to, final double t) {
        return from.add(to.subtract(from).multiply(t)).add(bow.multiply(1 - Math.pow(Math.abs(2 * t - 1), fullness)));
    }

    /** Part of a quarter superellipse into {@code corner}, from {@code from} to {@code to}, between {@code t0} and {@code t1} of it. */
    public static Curve corner(final Vec3 corner, final double squareness, final Vec3 from, final Vec3 to,
                               final double t0, final double t1) {
        return new Curve(Kind.CORNER, corner, squareness, from, to, t0, t1);
    }

    /**
     * The point {@code s} of the way along it (0 at {@code a}, 1 at {@code b}), {@code a} and
     * {@code b} the ends of the edge it curves.
     */
    public Vec3 at(final Vec3 a, final Vec3 b, final double s) {
        return switch (kind) {
            case POWER -> from == null ? bowAt(control, fullness, a, b, s) : bowAt(control, fullness, from, to, t0 + s * (t1 - t0));
            case CORNER -> {
                final double t = t0 + s * (t1 - t0);
                final double angle = t * Math.PI / 2;
                final double back = 1 - Math.pow(Math.sin(angle), 2 / fullness);
                final double on = 1 - Math.pow(Math.cos(angle), 2 / fullness);
                yield control.add(from.subtract(control).multiply(back)).add(to.subtract(control).multiply(on));
            }
            case ARC -> arc(a, b, s);
        };
    }

    /** On the circle through {@code a}, the middle raised, and {@code b}: by angle, {@code s} of the way round. */
    private Vec3 arc(final Vec3 a, final Vec3 b, final double s) {
        final Vec3 middle = a.add(b).multiply(0.5);
        final double rise = control.magnitude();
        final double half = a.distance(b) / 2;
        if (rise < 1e-9 || half < 1e-9) {
            return a.add(b.subtract(a).multiply(s));
        }
        // the circle's radius, and its centre on the far side of the line from the rise
        final double radius = (half * half + rise * rise) / (2 * rise);
        final Vec3 up = control.multiply(1 / rise);
        final Vec3 across = b.subtract(a).multiply(1 / (2 * half));
        final Vec3 centre = middle.add(up.multiply(rise - radius));
        // in the plane of across and up, about the centre: a at the angle to (-half, radius - rise), b at
        // the angle to (half, radius - rise); round from a to b over the top, the angle falling
        // (higher than a half circle, a starts below the centre: taken round past a half turn, so the way is over the top)
        final double fromA = Math.atan2(radius - rise, -half) < 0 ? Math.atan2(radius - rise, -half) + 2 * Math.PI
                : Math.atan2(radius - rise, -half);
        final double toB = Math.atan2(radius - rise, half);
        final double angle = fromA + s * (toB - fromA);
        return centre.add(across.multiply(radius * Math.cos(angle))).add(up.multiply(radius * Math.sin(angle)));
    }

    /** The same, all its points moved by {@code by} (its directions as they are). */
    public Curve moved(final Vec3 by) {
        if (kind == Kind.CORNER) {
            return new Curve(kind, control.add(by), fullness, from.add(by), to.add(by), t0, t1);
        }
        // a part of a bow: its whole's ends moved (its bow a direction, as it is)
        return from == null ? this : new Curve(kind, control, fullness, from.add(by), to.add(by), t0, t1);
    }

    /** The same, turned {@code degrees} about the upright through the origin, anticlockwise seen from above. */
    public Curve turned(final double degrees) {
        return new Curve(kind, turn(control, degrees), fullness, from == null ? null : turn(from, degrees),
                to == null ? null : turn(to, degrees), t0, t1);
    }

    /** {@code p} (across, along, up) turned {@code degrees} about the upright through the origin. */
    public static Vec3 turn(final Vec3 p, final double degrees) {
        final double r = Math.toRadians(degrees);
        return new Vec3(p.x() * Math.cos(r) - p.y() * Math.sin(r), p.x() * Math.sin(r) + p.y() * Math.cos(r), p.z());
    }
}
