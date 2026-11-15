package com.example.fxsuite.avatar.build;

import java.util.EnumMap;
import java.util.Map;

/**
 * A rectangle of a floor plan, cut into bays: a structural grid, its columns where its lines cross.
 * Its south-west corner at {@code x}, {@code y} (x across, to the east; y along, to the north), its
 * bays across {@code across} wide each, along {@code along}; each of its four corners shaped as it
 * says ({@link Corner}), where that corner is a corner of the plan it is part of.
 *
 * @param name    which it is
 * @param x       its west side
 * @param y       its south side
 * @param across  its bays' widths, west to east
 * @param along   its bays' depths, south to north
 * @param corners how each of its corners is shaped, if not square
 */
public record Grid(String name, double x, double y, double[] across, double[] along, Map<Quarter, Corner> corners) {

    /** Its corners, by the way they face. */
    public enum Quarter { SOUTH_WEST, SOUTH_EAST, NORTH_EAST, NORTH_WEST }

    public Grid {
        across = across.clone();
        along = along.clone();
        if (across.length == 0 || along.length == 0) {
            throw new IllegalArgumentException(name + " has no bays");
        }
        for (final double[] bays : new double[][] {across, along}) {
            for (final double bay : bays) {
                if (bay <= 0) {
                    throw new IllegalArgumentException(name + ": a bay " + bay + " wide");
                }
            }
        }
        final EnumMap<Quarter, Corner> shaped = new EnumMap<>(Quarter.class);
        shaped.putAll(corners);
        corners = Map.copyOf(shaped);
    }

    /** {@code width} by {@code depth}, its south-west corner at {@code x}, {@code y}, in bays as near {@code bay} as divide it evenly. */
    public static Grid of(final String name, final double x, final double y, final double width, final double depth,
                          final double bay) {
        return new Grid(name, x, y, even(width, bay), even(depth, bay), Map.of());
    }

    /** Its lines {@code xs} across and {@code ys} along, each in order, its corners shaped as {@code corners} say. */
    public static Grid lines(final String name, final double[] xs, final double[] ys,
                             final Map<Quarter, Corner> corners) {
        return new Grid(name, xs[0], ys[0], bays(xs), bays(ys), corners);
    }

    private static double[] bays(final double[] lines) {
        final double[] bays = new double[lines.length - 1];
        for (int k = 0; k < bays.length; k++) {
            bays[k] = lines[k + 1] - lines[k];
        }
        return bays;
    }

    /**
     * The same, its lines moved onto any of {@code xs} (across) and {@code ys} (along) within
     * {@code snap} of them, so it shares the lines of the grids it is put with; a bay that so comes to
     * nothing gone.
     */
    public Grid snapped(final java.util.NavigableSet<Double> xs, final java.util.NavigableSet<Double> ys,
                        final double snap) {
        return lines(name, snap(xs(), xs, snap), snap(ys(), ys, snap), corners);
    }

    private static double[] snap(final double[] lines, final java.util.NavigableSet<Double> onto, final double snap) {
        final java.util.TreeSet<Double> moved = new java.util.TreeSet<>();
        for (final double line : lines) {
            final Double below = onto.floor(line);
            final Double above = onto.ceiling(line);
            double to = line;
            double best = snap;
            for (final Double near : new Double[] {below, above}) {
                if (near != null && Math.abs(near - line) <= best) {
                    best = Math.abs(near - line);
                    to = near;
                }
            }
            moved.add(to);
        }
        return moved.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /** {@code length} in bays as near {@code bay} as divide it evenly; one at least. */
    public static double[] even(final double length, final double bay) {
        final int n = Math.max(1, (int) Math.round(length / bay));
        final double[] bays = new double[n];
        java.util.Arrays.fill(bays, length / n);
        return bays;
    }

    /** The same, its corner {@code quarter} shaped as {@code corner}. */
    public Grid shaped(final Quarter quarter, final Corner corner) {
        final EnumMap<Quarter, Corner> changed = new EnumMap<>(Quarter.class);
        changed.putAll(corners);
        changed.put(quarter, corner);
        return new Grid(name, x, y, across, along, changed);
    }

    /** The same, all four of its corners shaped as {@code corner}. */
    public Grid shaped(final Corner corner) {
        Grid all = this;
        for (final Quarter quarter : Quarter.values()) {
            all = all.shaped(quarter, corner);
        }
        return all;
    }

    /** How its corner {@code quarter} is shaped: square, unless it says. */
    public Corner corner(final Quarter quarter) {
        return corners.getOrDefault(quarter, Corner.SQUARE);
    }

    public double width() {
        return java.util.Arrays.stream(across).sum();
    }

    public double depth() {
        return java.util.Arrays.stream(along).sum();
    }

    /** Its lines across, west to east: its west side, between each two bays, its east side. */
    public double[] xs() {
        return lines(x, across);
    }

    /** Its lines along, south to north. */
    public double[] ys() {
        return lines(y, along);
    }

    private static double[] lines(final double from, final double[] bays) {
        final double[] at = new double[bays.length + 1];
        at[0] = from;
        for (int k = 0; k < bays.length; k++) {
            at[k + 1] = at[k] + bays[k];
        }
        return at;
    }

    /** Where its corner {@code quarter} is. */
    public double[] at(final Quarter quarter) {
        return switch (quarter) {
            case SOUTH_WEST -> new double[] {x, y};
            case SOUTH_EAST -> new double[] {x + width(), y};
            case NORTH_EAST -> new double[] {x + width(), y + depth()};
            case NORTH_WEST -> new double[] {x, y + depth()};
        };
    }

    /** Whether {@code px}, {@code py} is within it, its edges too. */
    public boolean covers(final double px, final double py) {
        return px >= x - 1e-6 && px <= x + width() + 1e-6 && py >= y - 1e-6 && py <= y + depth() + 1e-6;
    }
}
