package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * A body lofted through its sections ({@link Station}), tail to nose: each a superellipse, so each
 * can be round, oval, or near enough square, as wide and as deep as it likes, raised or lowered.
 * Its skin goes smooth from each to the next, {@code sides} points round. A section of no size is
 * a point: a nose, a tail.
 *
 * <p>So one model is an airship's envelope (round, long, its tail drawn to a point), an airliner's
 * fuselage (a round nose, a long tube, a tail swept up and flattened), a helicopter's pod and boom,
 * an engine's nacelle, a propeller's spinner.
 *
 * @param name     what it is
 * @param paint    what it is finished in
 * @param stations its sections, tail to nose (along increasing)
 * @param sides    how many points round each
 */
public record Fuselage(String name, Paint paint, List<Station> stations, int sides) implements Body {

    /**
     * A section of a fuselage: {@code along} its length, {@code halfWidth} and {@code halfHeight} out
     * from its middle, that middle {@code up} from the body's axis; {@code square}, the
     * superellipse's power: 2 an ellipse, more for squarer (4 a rounded box), never less than 1.
     */
    public record Station(double along, double halfWidth, double halfHeight, double up, double square) {

        public Station {
            if (halfWidth < 0 || halfHeight < 0 || square < 1) {
                throw new IllegalArgumentException("a section " + halfWidth + " by " + halfHeight + ", power " + square);
            }
        }

        /** Round: as wide as it is deep, on the axis. */
        public static Station round(final double along, final double radius) {
            return new Station(along, radius, radius, 0, 2);
        }

        /** Part way ({@code t}) from this one to {@code other}. */
        Station toward(final Station other, final double t) {
            return new Station(along + (other.along - along) * t, halfWidth + (other.halfWidth - halfWidth) * t,
                    halfHeight + (other.halfHeight - halfHeight) * t, up + (other.up - up) * t,
                    square + (other.square - square) * t);
        }

        /** The point on it {@code around} degrees round: 0 its right, 90 its top. */
        Point at(final double around) {
            final double t = Math.toRadians(around);
            final double c = Math.cos(t);
            final double s = Math.sin(t);
            final double e = 2 / square;
            return new Point(halfWidth * Math.signum(c) * Math.pow(Math.abs(c), e), along,
                    up + halfHeight * Math.signum(s) * Math.pow(Math.abs(s), e));
        }
    }

    public Fuselage {
        stations = List.copyOf(stations);
        if (stations.size() < 2 || sides < 6) {
            throw new IllegalArgumentException(name + ": " + stations.size() + " sections, " + sides + " sides");
        }
        for (int k = 1; k < stations.size(); k++) {
            if (!(stations.get(k).along() > stations.get(k - 1).along())) {
                throw new IllegalArgumentException(name + ": its sections not tail to nose");
            }
        }
    }

    @Override
    public Skin skin() {
        final List<List<Point>> rings = new ArrayList<>();
        for (final Station station : stations) {
            final List<Point> ring = new ArrayList<>();
            for (int k = 0; k < sides; k++) {
                ring.add(station.at(360.0 * k / sides));
            }
            rings.add(ring);
        }
        return new Skin(name, paint, rings);
    }

    @Override
    public Point surface(final double along, final double around) {
        return section(along).at(around);
    }

    /** Its section at {@code along}: between the two either side of it. */
    public Station section(final double along) {
        if (along <= stations.getFirst().along()) {
            return stations.getFirst();
        }
        for (int k = 1; k < stations.size(); k++) {
            final Station b = stations.get(k);
            if (along <= b.along()) {
                final Station a = stations.get(k - 1);
                return a.toward(b, (along - a.along()) / (b.along() - a.along()));
            }
        }
        return stations.getLast();
    }

    public double length() {
        return stations.getLast().along() - stations.getFirst().along();
    }

    // ---------------------------------------------------------------- made to a profile

    /**
     * A body of revolution's sections, {@code count} of them from tail to nose over {@code length},
     * about its middle: {@code radius} of {@code t} (0 at its tail, 1 at its nose), times
     * {@code biggest}; packed tighter toward its ends, where it curves the most.
     */
    static List<Station> profile(final double length, final double biggest, final int count,
                                 final DoubleUnaryOperator radius, final DoubleUnaryOperator up,
                                 final DoubleUnaryOperator squash, final double square) {
        final List<Station> stations = new ArrayList<>();
        for (int k = 0; k <= count; k++) {
            // closer together at the ends: a cosine spacing
            final double t = (1 - Math.cos(Math.PI * k / count)) / 2;
            // next to nothing is nothing: a nose, a tail, closed to a point
            final double share = radius.applyAsDouble(t);
            final double r = share < 1e-6 ? 0 : share * biggest;
            stations.add(new Station((t - 0.5) * length, r, r * squash.applyAsDouble(t), up.applyAsDouble(t) * biggest,
                    square));
        }
        return stations;
    }

    /**
     * An airship's envelope, {@code length} long and {@code diameter} through: a full round nose
     * over its front quarter, a parallel middle, and a long tail drawn to a point over its back third.
     */
    public static Fuselage envelope(final String name, final Paint paint, final double length, final double diameter) {
        final DoubleUnaryOperator radius = t -> {
            if (t >= 0.72) {
                final double u = (t - 0.72) / 0.28;
                return Math.sqrt(Math.max(0, 1 - u * u));
            }
            if (t >= 0.4) {
                return 1;
            }
            // the tail: full at 0.4, to a point at 0, a gentle curve
            final double u = t / 0.4;
            return Math.pow(Math.sin(Math.PI / 2 * u), 0.85);
        };
        return new Fuselage(name, paint, profile(length, diameter / 2, 28, radius, t -> 0, t -> 1, 2), 28);
    }

    /**
     * An aeroplane's fuselage, {@code length} long and {@code diameter} through: a rounded nose, a
     * long cabin, and a tail cone swept up and flattened, to a small round end.
     */
    public static Fuselage aircraft(final String name, final Paint paint, final double length, final double diameter) {
        final DoubleUnaryOperator radius = t -> {
            if (t >= 0.86) {
                final double u = (t - 0.86) / 0.14;
                return Math.sqrt(Math.max(0, 1 - u * u * u));
            }
            if (t >= 0.36) {
                return 1;
            }
            final double u = t / 0.36;
            return 0.16 + 0.84 * Math.pow(u, 1.3);
        };
        // the tail cone rises: its bottom swept up to its top
        final DoubleUnaryOperator up = t -> t >= 0.36 ? 0 : 0.8 * (1 - t / 0.36) * (1 - radius.applyAsDouble(t));
        final List<Station> stations = profile(length, diameter / 2, 24, radius, up,
                t -> t >= 0.36 ? 1 : 0.85 + 0.15 * t / 0.36, 2);
        // closed at the tail by a small cap
        final Station end = stations.getFirst();
        stations.set(0, new Station(end.along(), 0, 0, end.up(), 2));
        stations.add(1, new Station(end.along() + 1, end.halfWidth(), end.halfHeight(), end.up(), 2));
        return new Fuselage(name, paint, stations, 24);
    }

    /**
     * A helicopter's pod and boom, {@code length} long: a bulbous cabin {@code width} wide and
     * {@code height} high over its front half, a little squared; drawn in to a thin round boom, rising
     * to its tail.
     */
    public static Fuselage pod(final String name, final Paint paint, final double length, final double width,
                               final double height) {
        final double boom = 0.13;
        final DoubleUnaryOperator radius = t -> {
            if (t >= 0.8) {
                final double u = (t - 0.8) / 0.2;
                return Math.sqrt(Math.max(0, 1 - u * u));
            }
            if (t >= 0.55) {
                return 1;
            }
            if (t >= 0.38) {
                final double u = (t - 0.38) / 0.17;
                return boom + (1 - boom) * (3 * u * u - 2 * u * u * u);
            }
            if (t >= 0.02) {
                return boom * (0.75 + 0.25 * (t - 0.02) / 0.36);
            }
            return boom * 0.75 * t / 0.02;
        };
        // the boom rises toward its tail, its top in line with the cabin's top at the back
        final DoubleUnaryOperator up = t -> t >= 0.55 ? 0 : (0.62 * (1 - t / 0.55)) * (height / width);
        final DoubleUnaryOperator squash = t -> t >= 0.38 ? height / width : 1;
        final List<Station> stations = new ArrayList<>(profile(length, width / 2, 30, radius, up, squash, 2.6));
        return new Fuselage(name, paint, stations, 24);
    }

    /**
     * An engine's nacelle, {@code length} long and {@code diameter} through: an open round intake (its
     * lip a little in), full through its middle, tapering to an exhaust two fifths as big. Its nose,
     * the intake, is at +along.
     */
    public static Fuselage nacelle(final String name, final Paint paint, final double length, final double diameter) {
        final double r = diameter / 2;
        final double h = length / 2;
        return new Fuselage(name, paint, List.of(
                Station.round(-h, 0),
                Station.round(-h + 0.005 * length, 0.4 * r),
                Station.round(-h * 0.7, 0.62 * r),
                Station.round(-h * 0.2, 0.95 * r),
                Station.round(h * 0.3, r),
                Station.round(h * 0.75, 0.97 * r),
                Station.round(h * 0.95, 0.88 * r),
                Station.round(h - 0.001 * length, 0.78 * r),
                Station.round(h, 0)), 16);
    }

    /** A propeller's spinner, {@code length} long and {@code diameter} across its back: a blunt cone. */
    public static Fuselage spinner(final String name, final Paint paint, final double length, final double diameter) {
        final DoubleUnaryOperator radius = t -> Math.sqrt(Math.max(0, 1 - t * t));
        final List<Station> stations = new ArrayList<>(profile(length, diameter / 2, 8, radius, t -> 0, t -> 1, 2));
        // its back: closed flat
        stations.set(0, Station.round(stations.getFirst().along(), 0));
        stations.add(1, Station.round(stations.getFirst().along() + 0.002 * length, diameter / 2));
        return new Fuselage(name, paint, stations, 14);
    }
}
