package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * An outline across the ground ({@code outline}, its points' x and z in the world), stood straight
 * up from {@code bottom} to {@code top}: a round tower's shaft, a wall's run, a pyramid's step. Any
 * outline that does not cross itself, convex or not; its top flat.
 */
public record Prism(List<Vec2> outline, double bottom, double top) implements Volume {

    public Prism {
        outline = List.copyOf(outline);
        if (outline.size() < 3 || Math.abs(Outline.twiceArea(outline)) < 1e-6 || !(top > bottom)) {
            throw new IllegalArgumentException("not a prism: " + outline.size() + " points, " + bottom + " to " + top);
        }
    }

    @Override
    public double low() {
        return bottom;
    }

    @Override
    public double high() {
        return top;
    }

    /** Whether a circle at ({@code x}, {@code z}) is anywhere near it: its outline's box, grown by the radius. */
    private boolean near(final double x, final double z, final double radius) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (final Vec2 p : outline) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minZ = Math.min(minZ, p.z());
            maxZ = Math.max(maxZ, p.z());
        }
        return x > minX - radius && x < maxX + radius && z > minZ - radius && z < maxZ + radius;
    }

    @Override
    public boolean covers(final double x, final double z, final double radius) {
        return near(x, z, radius) && Outline.depth(outline, x, z, radius) > 0;
    }

    @Override
    public double topAt(final double x, final double z, final double radius, final double below) {
        return top <= below && covers(x, z, radius) ? top : Double.NEGATIVE_INFINITY;
    }

    @Override
    public double undersideAt(final double x, final double z, final double radius, final double above) {
        return bottom >= above && covers(x, z, radius) ? bottom : Double.POSITIVE_INFINITY;
    }

    @Override
    public Vec2 push(final double x, final double z, final double radius, final double feet, final double head) {
        if (top <= feet || bottom >= head || !near(x, z, radius)) {
            return null;
        }
        return Outline.push(outline, x, z, radius);
    }

    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        if (top <= feet || bottom >= head
                || !near((ax + bx) / 2, (az + bz) / 2, radius + Math.hypot(bx - ax, bz - az) / 2)) {
            return null;
        }
        return Outline.push(outline, ax, az, bx, bz, radius);
    }

    /**
     * Where a ray first meets it: through one of its sides, or down onto its top, or up into its
     * underside; {@code null} if it misses, or starts inside.
     */
    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        final double up0 = -from.y();
        final double rise = -direction.y();
        if (up0 >= bottom && up0 <= top && Outline.inside(outline, from.x(), from.z())) {
            return null;
        }
        double best = Double.POSITIVE_INFINITY;
        Vec3 normal = null;
        // its top and its underside
        if (Math.abs(rise) > 1e-12) {
            for (final double level : new double[] {top, bottom}) {
                final double t = (level - up0) / rise;
                if (t >= 0 && t < best && t <= reach) {
                    final double px = from.x() + direction.x() * t;
                    final double pz = from.z() + direction.z() * t;
                    if (Outline.inside(outline, px, pz)) {
                        best = t;
                        normal = new Vec3(0, level == top ? -1 : 1, 0);
                    }
                }
            }
        }
        // its sides: each edge, stood up between its bottom and its top
        final double turn = Math.signum(Outline.twiceArea(outline));
        for (int i = 0; i < outline.size(); i++) {
            final Vec2 a = outline.get(i);
            final Vec2 b = outline.get((i + 1) % outline.size());
            final double ex = b.x() - a.x();
            final double ez = b.z() - a.z();
            final double det = direction.x() * ez - direction.z() * ex;
            if (Math.abs(det) < 1e-12) {
                continue;
            }
            final double t = ((a.x() - from.x()) * ez - (a.z() - from.z()) * ex) / det;
            final double s = ((a.x() - from.x()) * direction.z() - (a.z() - from.z()) * direction.x()) / det;
            if (t < 0 || t >= best || t > reach || s < 0 || s > 1) {
                continue;
            }
            final double up = up0 + rise * t;
            if (up < bottom || up > top) {
                continue;
            }
            final double length = Math.hypot(ex, ez);
            // out of the outline: to the right of each edge going anticlockwise
            final Vec3 out = new Vec3(ez / length * turn + 0.0, 0, -ex / length * turn + 0.0);
            // only through its outside: going in, against the way it faces
            if (out.x() * direction.x() + out.z() * direction.z() < 0) {
                best = t;
                normal = out;
            }
        }
        return normal == null ? null : new Hit(from.add(direction.multiply(best)), normal, Surface.BLOCK, best);
    }

    @Override
    public boolean overlaps(final Block block) {
        return bottom < block.top() && top > block.bottom() && Outline.intersects(outline, Outline.corners(block));
    }
}
