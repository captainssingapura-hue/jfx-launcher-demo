package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat outlines: a closed polygon of points, either way round, convex or not (but not crossing
 * itself). What the shapes in the way are made of ({@link Prism}, {@link Extrusion}): whether a
 * point is inside one, the nearest point of its edge, how far a circle (or a stroke) must move to
 * clear it, and the part of a convex one on one side of a line. Pure maths.
 */
public final class Outline {

    private Outline() {
    }

    /** Twice its area, positive if it goes round anticlockwise (x to the right, z up). */
    public static double twiceArea(final List<Vec2> points) {
        double sum = 0;
        for (int i = 0; i < points.size(); i++) {
            final Vec2 a = points.get(i);
            final Vec2 b = points.get((i + 1) % points.size());
            sum += a.x() * b.z() - b.x() * a.z();
        }
        return sum;
    }

    /** Whether ({@code x}, {@code z}) is inside it. */
    public static boolean inside(final List<Vec2> points, final double x, final double z) {
        boolean in = false;
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            final Vec2 a = points.get(i);
            final Vec2 b = points.get(j);
            if ((a.z() > z) != (b.z() > z) && x < (b.x() - a.x()) * (z - a.z()) / (b.z() - a.z()) + a.x()) {
                in = !in;
            }
        }
        return in;
    }

    /** The point on its edge nearest ({@code x}, {@code z}). */
    public static Vec2 nearest(final List<Vec2> points, final double x, final double z) {
        Vec2 best = points.getFirst();
        double least = Double.POSITIVE_INFINITY;
        for (int i = 0; i < points.size(); i++) {
            final Vec2 a = points.get(i);
            final Vec2 b = points.get((i + 1) % points.size());
            final double ex = b.x() - a.x();
            final double ez = b.z() - a.z();
            final double length = ex * ex + ez * ez;
            final double t = length < 1e-12 ? 0 : Num.clamp(((x - a.x()) * ex + (z - a.z()) * ez) / length, 0, 1);
            final double px = a.x() + ex * t;
            final double pz = a.z() + ez * t;
            final double d = (px - x) * (px - x) + (pz - z) * (pz - z);
            if (d < least) {
                least = d;
                best = new Vec2(px, pz);
            }
        }
        return best;
    }

    /** How far a circle at ({@code x}, {@code z}) reaches into it: its radius less its gap from it, or more if inside. */
    public static double depth(final List<Vec2> points, final double x, final double z, final double radius) {
        final Vec2 near = nearest(points, x, z);
        final double gap = Math.hypot(near.x() - x, near.z() - z);
        return inside(points, x, z) ? gap + radius : radius - gap;
    }

    /**
     * How far a circle at ({@code x}, {@code z}) must move to clear it; {@code null} if it already
     * does. Straight out from the nearest point of its edge.
     */
    public static Vec2 push(final List<Vec2> points, final double x, final double z, final double radius) {
        final Vec2 near = nearest(points, x, z);
        final double dx = near.x() - x;
        final double dz = near.z() - z;
        final double gap = Math.hypot(dx, dz);
        if (inside(points, x, z)) {
            // out through the nearest edge, as far again as the circle is wide
            if (gap < 1e-9) {
                return null;
            }
            return new Vec2(dx / gap * (gap + radius), dz / gap * (gap + radius));
        }
        if (gap >= radius || gap < 1e-9) {
            return null;
        }
        return new Vec2(-dx / gap * (radius - gap), -dz / gap * (radius - gap));
    }

    /**
     * How far a stroke from ({@code ax}, {@code az}) to ({@code bx}, {@code bz}), {@code radius}
     * thick, must move to clear it; {@code null} if it already does. Pushed as its deepest point is:
     * among its ends, where it comes nearest each corner, and where it crosses each edge.
     */
    public static Vec2 push(final List<Vec2> points, final double ax, final double az, final double bx, final double bz,
                            final double radius) {
        final double ex = bx - ax;
        final double ez = bz - az;
        final double length = ex * ex + ez * ez;
        final List<Double> at = new ArrayList<>(List.of(0.0, 1.0));
        for (int i = 0; i < points.size(); i++) {
            final Vec2 a = points.get(i);
            final Vec2 b = points.get((i + 1) % points.size());
            if (length > 1e-12) {
                at.add(Num.clamp(((a.x() - ax) * ex + (a.z() - az) * ez) / length, 0, 1));
            }
            // where it crosses this edge, if it does
            final double fx = b.x() - a.x();
            final double fz = b.z() - a.z();
            final double det = ex * fz - ez * fx;
            if (Math.abs(det) > 1e-12) {
                final double t = ((a.x() - ax) * fz - (a.z() - az) * fx) / det;
                final double s = ((a.x() - ax) * ez - (a.z() - az) * ex) / det;
                if (t >= 0 && t <= 1 && s >= 0 && s <= 1) {
                    // a little either side of it: on the edge itself, which way is out is not known
                    at.add(Math.max(0, t - 1e-6));
                    at.add(Math.min(1, t + 1e-6));
                }
            }
        }
        double deepest = 0;
        double best = -1;
        for (final double t : at) {
            final double d = depth(points, ax + ex * t, az + ez * t, radius);
            if (d > deepest) {
                deepest = d;
                best = t;
            }
        }
        return best < 0 ? null : push(points, ax + ex * best, az + ez * best, radius);
    }

    /**
     * The part of convex outline {@code points} where {@code a x + b z <= c}: clipped by a line, as
     * an outline again (empty if none of it is).
     */
    public static List<Vec2> clip(final List<Vec2> points, final double a, final double b, final double c) {
        final List<Vec2> kept = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            final Vec2 p = points.get(i);
            final Vec2 q = points.get((i + 1) % points.size());
            final double fp = a * p.x() + b * p.z() - c;
            final double fq = a * q.x() + b * q.z() - c;
            if (fp <= 0) {
                kept.add(p);
            }
            if ((fp < 0 && fq > 0) || (fp > 0 && fq < 0)) {
                final double t = fp / (fp - fq);
                kept.add(new Vec2(p.x() + (q.x() - p.x()) * t, p.z() + (q.z() - p.z()) * t));
            }
        }
        return kept;
    }

    /** Whether outlines {@code a} and {@code b} share any ground: one's edges cross the other's, or one is inside the other. */
    public static boolean intersects(final List<Vec2> a, final List<Vec2> b) {
        if (inside(b, a.getFirst().x(), a.getFirst().z()) || inside(a, b.getFirst().x(), b.getFirst().z())) {
            return true;
        }
        for (int i = 0; i < a.size(); i++) {
            final Vec2 p = a.get(i);
            final Vec2 q = a.get((i + 1) % a.size());
            for (int j = 0; j < b.size(); j++) {
                final Vec2 r = b.get(j);
                final Vec2 s = b.get((j + 1) % b.size());
                final double ex = q.x() - p.x();
                final double ez = q.z() - p.z();
                final double fx = s.x() - r.x();
                final double fz = s.z() - r.z();
                final double det = ex * fz - ez * fx;
                if (Math.abs(det) < 1e-12) {
                    continue;
                }
                final double t = ((r.x() - p.x()) * fz - (r.z() - p.z()) * fx) / det;
                final double u = ((r.x() - p.x()) * ez - (r.z() - p.z()) * ex) / det;
                if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A block's footprint, its four corners anticlockwise. */
    public static List<Vec2> corners(final Block block) {
        final Vec3 right = Num.right(block.yaw());
        final Vec3 ahead = Num.heading(block.yaw());
        final List<Vec2> corners = new ArrayList<>();
        for (final double[] c : new double[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            corners.add(new Vec2(block.x() + right.x() * c[0] * block.halfWidth() + ahead.x() * c[1] * block.halfDepth(),
                    block.z() + right.z() * c[0] * block.halfWidth() + ahead.z() * c[1] * block.halfDepth()));
        }
        return twiceArea(corners) < 0 ? corners.reversed() : corners;
    }

    /** The part of convex outline {@code points} also inside convex outline {@code by}. */
    public static List<Vec2> within(final List<Vec2> points, final List<Vec2> by) {
        List<Vec2> kept = points;
        final double turn = Math.signum(twiceArea(by));
        for (int i = 0; i < by.size() && !kept.isEmpty(); i++) {
            final Vec2 a = by.get(i);
            final Vec2 b = by.get((i + 1) % by.size());
            // inside is to the left of each edge going anticlockwise: (b - a) x (p - a) >= 0
            final double nx = (b.z() - a.z()) * turn;
            final double nz = -(b.x() - a.x()) * turn;
            kept = clip(kept, nx, nz, nx * a.x() + nz * a.z());
        }
        return kept;
    }
}
