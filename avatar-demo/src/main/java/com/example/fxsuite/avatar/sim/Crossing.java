package com.example.fxsuite.avatar.sim;

import java.util.List;

/**
 * What extrusions ({@link Extrusion}) share: where all of them are. Two gables crossed at right
 * angles share a hip roof; two triangles crossed, a square pyramid; three at 60° to each other, a
 * hexagonal one; four at 45°, an octagonal one; two barrel vaults, a cross vault. Its top anywhere
 * the lowest of theirs, its underside the highest; between a body's feet and its head, it is across
 * the ground where all their stretches are.
 */
public record Crossing(List<Extrusion> parts) implements Volume {

    public Crossing {
        parts = List.copyOf(parts);
        if (parts.size() < 2) {
            throw new IllegalArgumentException("a crossing of " + parts.size() + ": it takes two");
        }
        for (final Extrusion part : parts) {
            if (!part.holes().isEmpty()) {
                throw new IllegalArgumentException("crossed extrusions are solid all through");
            }
        }
    }

    /** Two of them, crossed. */
    public Crossing(final Extrusion first, final Extrusion second) {
        this(List.of(first, second));
    }

    @Override
    public double low() {
        return parts.stream().mapToDouble(Extrusion::low).max().orElseThrow();
    }

    @Override
    public double high() {
        return parts.stream().mapToDouble(Extrusion::high).min().orElseThrow();
    }

    /** Its box, seen from above: round all its parts. */
    @Override
    public Volume.Bounds bounds() {
        return parts.stream().map(Extrusion::bounds).reduce(Volume.Bounds::and).orElseThrow();
    }

    @Override
    public boolean covers(final double x, final double z, final double radius) {
        return parts.stream().allMatch(p -> p.covers(x, z, radius));
    }

    @Override
    public double topAt(final double x, final double z, final double radius, final double below) {
        final double top = parts.stream().mapToDouble(p -> p.topAt(x, z, radius)).min().orElseThrow();
        return top <= below ? top : Double.NEGATIVE_INFINITY;
    }

    @Override
    public double undersideAt(final double x, final double z, final double radius, final double above) {
        final double underside = parts.stream().mapToDouble(p -> p.undersideAt(x, z, radius)).max().orElseThrow();
        return underside >= above ? underside : Double.POSITIVE_INFINITY;
    }

    /** Across the ground, where all their stretches between {@code feet} and {@code head} are; empty if nowhere. */
    private List<Vec2> outline(final double feet, final double head) {
        List<Vec2> shared = null;
        for (final Extrusion part : parts) {
            final Block box = part.box(feet, head);
            if (box == null) {
                return List.of();
            }
            shared = shared == null ? Outline.corners(box) : Outline.within(shared, Outline.corners(box));
            if (shared.size() < 3) {
                return List.of();
            }
        }
        return Math.abs(Outline.twiceArea(shared)) > 1e-6 ? shared : List.of();
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

    /** In where it last goes into any of them, out where it first comes out of any. */
    @Override
    public Hit ray(final Vec3 from, final Vec3 direction, final double reach) {
        Extrusion.Span later = null;
        double exit = Double.POSITIVE_INFINITY;
        for (final Extrusion part : parts) {
            final Extrusion.Span span = part.span(from, direction);
            if (span == null) {
                return null;
            }
            if (later == null || span.enter() > later.enter()) {
                later = span;
            }
            exit = Math.min(exit, span.exit());
        }
        if (later.enter() > exit || later.enter() < 0 || later.enter() > reach) {
            return null;
        }
        return new Hit(from.add(direction.multiply(later.enter())), later.in(), Surface.BLOCK, later.enter());
    }

    @Override
    public boolean overlaps(final Block block) {
        final double feet = Math.max(low(), block.bottom());
        final double head = Math.min(high(), block.top());
        if (feet >= head) {
            return false;
        }
        final List<Vec2> shared = outline(feet, head);
        return !shared.isEmpty() && Outline.intersects(shared, Outline.corners(block));
    }
}
