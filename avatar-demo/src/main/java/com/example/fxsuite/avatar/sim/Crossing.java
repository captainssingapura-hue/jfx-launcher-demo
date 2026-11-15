package com.example.fxsuite.avatar.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * What two extrusions ({@link Extrusion}) share: where both are. Two gables crossed at right angles
 * share a hip roof; two triangles crossed, a pyramid; two barrel vaults, a cross vault. Its top
 * anywhere the lower of theirs, its underside the higher; between a body's feet and its head, it is
 * across the ground where both their stretches are.
 */
public record Crossing(Extrusion first, Extrusion second) implements Volume {

    @Override
    public double low() {
        return Math.max(first.low(), second.low());
    }

    @Override
    public double high() {
        return Math.min(first.high(), second.high());
    }

    @Override
    public boolean covers(final double x, final double z, final double radius) {
        return first.covers(x, z, radius) && second.covers(x, z, radius);
    }

    @Override
    public double topAt(final double x, final double z, final double radius) {
        return Math.min(first.topAt(x, z, radius), second.topAt(x, z, radius));
    }

    @Override
    public double undersideAt(final double x, final double z, final double radius) {
        return Math.max(first.undersideAt(x, z, radius), second.undersideAt(x, z, radius));
    }

    /** Across the ground, where both their stretches between {@code feet} and {@code head} are; empty if nowhere. */
    private List<Vec2> outline(final double feet, final double head) {
        final Block a = first.box(feet, head);
        final Block b = second.box(feet, head);
        if (a == null || b == null) {
            return List.of();
        }
        final List<Vec2> shared = Outline.within(corners(a), corners(b));
        return shared.size() >= 3 && Math.abs(Outline.twiceArea(shared)) > 1e-6 ? shared : List.of();
    }

    /** A block's footprint, its four corners anticlockwise. */
    private static List<Vec2> corners(final Block block) {
        final Vec3 right = Num.right(block.yaw());
        final Vec3 ahead = Num.heading(block.yaw());
        final List<Vec2> corners = new ArrayList<>();
        for (final double[] c : new double[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            corners.add(new Vec2(block.x() + right.x() * c[0] * block.halfWidth() + ahead.x() * c[1] * block.halfDepth(),
                    block.z() + right.z() * c[0] * block.halfWidth() + ahead.z() * c[1] * block.halfDepth()));
        }
        return Outline.twiceArea(corners) < 0 ? corners.reversed() : corners;
    }

    @Override
    public Vec2 push(final double x, final double z, final double radius, final double feet, final double head) {
        final List<Vec2> shared = outline(feet, head);
        return shared.isEmpty() ? null : Outline.push(shared, x, z, radius);
    }

    @Override
    public Vec2 push(final double ax, final double az, final double bx, final double bz, final double radius,
                     final double feet, final double head) {
        final List<Vec2> shared = outline(feet, head);
        return shared.isEmpty() ? null : Outline.push(shared, ax, az, bx, bz, radius);
    }

    /** In where it last goes into either, out where it first comes out of either. */
    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        final Extrusion.Span a = first.span(from, direction);
        final Extrusion.Span b = second.span(from, direction);
        if (a == null || b == null) {
            return null;
        }
        final Extrusion.Span later = a.enter() >= b.enter() ? a : b;
        final double exit = Math.min(a.exit(), b.exit());
        if (later.enter() > exit || later.enter() < 0 || later.enter() > reach) {
            return null;
        }
        return new Hit(from.add(direction.multiply(later.enter())), later.normal(), Surface.BLOCK, later.enter());
    }
}
