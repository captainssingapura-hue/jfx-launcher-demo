package com.example.fxsuite.avatar.build;

import java.util.ArrayList;
import java.util.List;

/**
 * One of a building's high-level surfaces: a flat rectangle on it, a front, an end wall, its roof,
 * the underside of a floor, that a design ({@link Design}) dresses. The building says where each is
 * and what lines of its structure cross it; how it is dressed (its windows, balconies, fins, what it
 * is made of) is its design's to say, so one building can be dressed many ways.
 *
 * <p>Its own frame: {@code u} along it, left to right as seen from outside; {@code v} up it (or, for
 * a roof or a floor, across it); {@code w} out of it, toward whoever looks at it, 0 being its plane
 * (the line its structure runs along: a wall's middle). In the building's frame, {@code origin} is
 * its corner at u and v 0, and {@code u}, {@code v} and {@code out} are its directions, each of
 * length one and square to the others.
 *
 * @param name    which it is
 * @param kind    what sort it is ("front", "end", "roof"): how a set of designs says which it dresses
 * @param origin  its corner at u 0, v 0
 * @param u       its direction along
 * @param v       its direction up
 * @param out     its direction out
 * @param width   how far along it goes, in u
 * @param height  how far up it goes, in v
 * @param columns where the building's lines of columns cross it, along u, from 0 to its width
 * @param floors  where its storeys start, up v, from 0; the last runs up to its parapet
 * @param parapet how high a parapet tops it (0 for none): the band at its top above its last storey
 */
public record Surface(String name, String kind, Point origin, Point u, Point v, Point out, double width,
                      double height, double[] columns, double[] floors, double parapet) {

    public Surface {
        columns = columns.clone();
        floors = floors.clone();
        for (final Point axis : List.of(u, v, out)) {
            if (Math.abs(axis.length() - 1) > 1e-9) {
                throw new IllegalArgumentException(name + ": its directions are each of length one");
            }
        }
        if (Math.abs(u.dot(v)) > 1e-9 || Math.abs(u.dot(out)) > 1e-9 || Math.abs(v.dot(out)) > 1e-9) {
            throw new IllegalArgumentException(name + ": its directions are square to one another");
        }
    }

    /** The point {@code u} along it, {@code v} up it and {@code w} out of it, in the building's frame. */
    public Point at(final double u, final double v, final double w) {
        return origin.plus(this.u.times(u)).plus(this.v.times(v)).plus(out.times(w));
    }

    /**
     * A box on it, from {@code u0} to {@code u1} along, {@code v0} to {@code v1} up, {@code w0} to
     * {@code w1} out; doing {@code role}, of what {@code palette} makes that of.
     */
    public Solid box(final String called, final Palette palette, final Role role, final double u0, final double u1,
                     final double v0, final double v1, final double w0, final double w1) {
        final List<Point> corners = new ArrayList<>(8);
        for (int k = 0; k < 8; k++) {
            corners.add(at((k & 1) == 0 ? u0 : u1, (k & 2) == 0 ? v0 : v1, (k & 4) == 0 ? w0 : w1));
        }
        return new Solid(name + ", " + called, role, palette.of(role), corners);
    }

    /** How many storeys it has, below its parapet. */
    public int storeys() {
        return floors.length;
    }

    /** Where storey {@code s} starts, and where it ends: the next's start, or its parapet. */
    public double storeyTop(final int s) {
        return s + 1 < floors.length ? floors[s + 1] : height - parapet;
    }

    /** How many bays it has: the spaces between its lines of columns. */
    public int bays() {
        return columns.length - 1;
    }
}
