package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * A contour of a {@link Terrain}: a closed loop of points round the ground (x, z), the ground on it at
 * {@code height}. Loops never cross, so they nest: one inside a lower one is a hill, inside a higher one a
 * hollow. A {@code flat} one holds all the ground inside it at its height (a level pad to build on).
 *
 * @param height the ground's height on it, in centimetres
 * @param loop   its points in order round it, either way round, the last joined back to the first
 * @param flat   whether the ground inside it is level at its height
 */
public record Contour(double height, List<Vec2> loop, boolean flat) {

    public Contour {
        loop = List.copyOf(loop);
        if (loop.size() < 3 || Math.abs(Outline.twiceArea(loop)) < 1e-6) {
            throw new IllegalArgumentException("a contour goes round something");
        }
    }

    /** A contour the ground goes on through, up or down. */
    public Contour(final double height, final List<Vec2> loop) {
        this(height, loop, false);
    }

    /** How much ground it goes round, in square centimetres. */
    public double area() {
        return Math.abs(Outline.twiceArea(loop)) / 2;
    }

    /** Whether ({@code x}, {@code z}) is inside it. */
    public boolean inside(final double x, final double z) {
        return Outline.inside(loop, x, z);
    }

    /** How far ({@code x}, {@code z}) is from its line. */
    public double distance(final double x, final double z) {
        final Vec2 near = Outline.nearest(loop, x, z);
        return Math.hypot(near.x() - x, near.z() - z);
    }

    /** Whether its line crosses itself. */
    public boolean crossesItself() {
        final int n = loop.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 2; j < n; j++) {
                if (i == 0 && j == n - 1) {
                    continue; // its last side and its first meet at its first point
                }
                if (cross(loop.get(i), loop.get((i + 1) % n), loop.get(j), loop.get((j + 1) % n))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether its line and {@code other}'s cross or touch. */
    public boolean crosses(final Contour other) {
        final int n = loop.size();
        final int m = other.loop.size();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (cross(loop.get(i), loop.get((i + 1) % n), other.loop.get(j), other.loop.get((j + 1) % m))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The point inside it farthest from its line: where a top with nothing marked on it rises to (or a
     * hollow sinks to). Found by sampling across it, then closer and closer round the best so far.
     */
    public Vec2 pole() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (final Vec2 p : loop) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minZ = Math.min(minZ, p.z());
            maxZ = Math.max(maxZ, p.z());
        }
        final int samples = 24;
        double stepX = (maxX - minX) / samples;
        double stepZ = (maxZ - minZ) / samples;
        Vec2 best = null;
        double far = -1;
        for (int i = 0; i <= samples; i++) {
            for (int j = 0; j <= samples; j++) {
                final double x = minX + i * stepX;
                final double z = minZ + j * stepZ;
                if (inside(x, z) && distance(x, z) > far) {
                    far = distance(x, z);
                    best = new Vec2(x, z);
                }
            }
        }
        if (best == null) {
            double x = 0;
            double z = 0;
            for (final Vec2 p : loop) {
                x += p.x();
                z += p.z();
            }
            return new Vec2(x / loop.size(), z / loop.size());
        }
        for (int round = 0; round < 8; round++) {
            stepX /= 2;
            stepZ /= 2;
            final Vec2 around = best;
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    final double x = around.x() + i * stepX;
                    final double z = around.z() + j * stepZ;
                    if (inside(x, z) && distance(x, z) > far) {
                        far = distance(x, z);
                        best = new Vec2(x, z);
                    }
                }
            }
        }
        return best;
    }

    /** Whether the segments a–b and c–d cross or touch. */
    static boolean cross(final Vec2 a, final Vec2 b, final Vec2 c, final Vec2 d) {
        final double d1 = turn(c, d, a);
        final double d2 = turn(c, d, b);
        final double d3 = turn(a, b, c);
        final double d4 = turn(a, b, d);
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }
        return d1 == 0 && on(c, d, a) || d2 == 0 && on(c, d, b) || d3 == 0 && on(a, b, c) || d4 == 0 && on(a, b, d);
    }

    private static double turn(final Vec2 a, final Vec2 b, final Vec2 c) {
        return (b.x() - a.x()) * (c.z() - a.z()) - (b.z() - a.z()) * (c.x() - a.x());
    }

    private static boolean on(final Vec2 a, final Vec2 b, final Vec2 p) {
        return p.x() >= Math.min(a.x(), b.x()) && p.x() <= Math.max(a.x(), b.x())
                && p.z() >= Math.min(a.z(), b.z()) && p.z() <= Math.max(a.z(), b.z());
    }
}
