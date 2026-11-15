package com.example.fxsuite.avatar.air;

import com.example.fxsuite.avatar.build.Point;

import java.util.Collection;

/** How far something reaches each way, in an aircraft's frame: a box square to it, round all of it. */
public record Extent(double minAcross, double maxAcross, double minAlong, double maxAlong, double minUp,
                     double maxUp) {

    /** The box round {@code points}. */
    public static Extent of(final Collection<Point> points) {
        double[] e = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (final Point p : points) {
            e[0] = Math.min(e[0], p.across());
            e[1] = Math.max(e[1], p.across());
            e[2] = Math.min(e[2], p.along());
            e[3] = Math.max(e[3], p.along());
            e[4] = Math.min(e[4], p.up());
            e[5] = Math.max(e[5], p.up());
        }
        return new Extent(e[0], e[1], e[2], e[3], e[4], e[5]);
    }

    public double width() {
        return maxAcross - minAcross;
    }

    public double length() {
        return maxAlong - minAlong;
    }

    public double height() {
        return maxUp - minUp;
    }
}
