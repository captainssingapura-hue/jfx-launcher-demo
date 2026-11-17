package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A line across the ground, point to point, and what is so near it ({@code reach}): where on it is nearest
 * a point, and how far. What a feature laid along a line is worked out from: a river's channel, a road.
 *
 * <p>Its segments are kept by where they are on a grid of cells ({@link #CELL}), each cell listing those
 * that come within reach of it: so a point far from it is told so at once, and one near it looks at a few.
 */
public final class Polyline {

    /** How wide the cells its segments are kept in. */
    static final double CELL = 2_000;

    /** The nearest point on it to a point: on segment {@code segment}, {@code t} of the way along it, {@code distance} off. */
    public record Near(int segment, double t, double distance) {
    }

    private final List<Vec2> points;
    private final double reach;
    private final Map<Long, int[]> cells = new HashMap<>();

    public Polyline(final List<Vec2> points, final double reach) {
        this.points = List.copyOf(points);
        if (points.size() < 2 || !(reach > 0)) {
            throw new IllegalArgumentException("a line goes somewhere, and reaches some way");
        }
        this.reach = reach;
        final Map<Long, List<Integer>> kept = new HashMap<>();
        for (int s = 0; s + 1 < points.size(); s++) {
            final Vec2 a = points.get(s);
            final Vec2 b = points.get(s + 1);
            for (long i = cell(Math.min(a.x(), b.x()) - reach); i <= cell(Math.max(a.x(), b.x()) + reach); i++) {
                for (long j = cell(Math.min(a.z(), b.z()) - reach); j <= cell(Math.max(a.z(), b.z()) + reach); j++) {
                    kept.computeIfAbsent(key(i, j), k -> new ArrayList<>()).add(s);
                }
            }
        }
        kept.forEach((k, segments) -> cells.put(k, segments.stream().mapToInt(Integer::intValue).toArray()));
    }

    public List<Vec2> points() {
        return points;
    }

    public double reach() {
        return reach;
    }

    /** Where on it is nearest ({@code x}, {@code z}); null if that is beyond its reach. */
    public Near nearest(final double x, final double z) {
        final int[] near = cells.get(key(cell(x), cell(z)));
        if (near == null) {
            return null;
        }
        Near best = null;
        for (final int s : near) {
            final Vec2 a = points.get(s);
            final Vec2 b = points.get(s + 1);
            final double dx = b.x() - a.x();
            final double dz = b.z() - a.z();
            final double length = dx * dx + dz * dz;
            final double t = length == 0 ? 0 : Num.clamp(((x - a.x()) * dx + (z - a.z()) * dz) / length, 0, 1);
            final double d = Math.hypot(x - a.x() - t * dx, z - a.z() - t * dz);
            if (d < reach && (best == null || d < best.distance())) {
                best = new Near(s, t, d);
            }
        }
        return best;
    }

    /** How far ({@code x}, {@code z}) is from it; infinity if beyond its reach. */
    public double distance(final double x, final double z) {
        final Near near = nearest(x, z);
        return near == null ? Double.POSITIVE_INFINITY : near.distance();
    }

    /** Whether any of it comes within its reach of the ground from ({@code minX}, {@code minZ}) to ({@code maxX}, {@code maxZ}). */
    public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
        for (long i = cell(minX); i <= cell(maxX); i++) {
            for (long j = cell(minZ); j <= cell(maxZ); j++) {
                final int[] near = cells.get(key(i, j));
                if (near == null) {
                    continue;
                }
                for (final int s : near) {
                    if (crossesBox(points.get(s), points.get(s + 1), minX - reach, minZ - reach, maxX + reach, maxZ + reach)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** How long it is, end to end along it. */
    public double length() {
        double length = 0;
        for (int s = 0; s + 1 < points.size(); s++) {
            length += Math.hypot(points.get(s + 1).x() - points.get(s).x(), points.get(s + 1).z() - points.get(s).z());
        }
        return length;
    }

    private static long cell(final double v) {
        return (long) Math.floor(v / CELL);
    }

    private static long key(final long i, final long j) {
        return i * 0x1_0000_0000L + (j & 0xffff_ffffL);
    }

    /** Whether segment a–b comes into the box: either end in it, or it crosses one of the box's sides. */
    private static boolean crossesBox(final Vec2 a, final Vec2 b, final double minX, final double minZ,
                                      final double maxX, final double maxZ) {
        if (inBox(a, minX, minZ, maxX, maxZ) || inBox(b, minX, minZ, maxX, maxZ)) {
            return true;
        }
        final Vec2[] corners = {new Vec2(minX, minZ), new Vec2(maxX, minZ), new Vec2(maxX, maxZ), new Vec2(minX, maxZ)};
        for (int k = 0; k < 4; k++) {
            if (Contour.cross(a, b, corners[k], corners[(k + 1) % 4])) {
                return true;
            }
        }
        return false;
    }

    private static boolean inBox(final Vec2 p, final double minX, final double minZ, final double maxX, final double maxZ) {
        return p.x() >= minX && p.x() <= maxX && p.z() >= minZ && p.z() <= maxZ;
    }
}
