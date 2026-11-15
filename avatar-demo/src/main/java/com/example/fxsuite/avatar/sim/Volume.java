package com.example.fxsuite.avatar.sim;

/**
 * Something solid in the world shaped more closely than a box ({@link Block}): an outline stood up
 * straight ({@link Prism}); one laid on its side and drawn along, a passage through it if it has one
 * ({@link Extrusion}); the part two of those share ({@link Crossing}). What is in the way of a body,
 * or a round, as it is.
 *
 * <p>A body meets it as an upright cylinder, as it meets a block: across the ground, a circle; up
 * and down, from its feet to its head. So what each says is about a circle, or a stroke, at some
 * heights: what it covers, how high a top of it is under it, how low an underside over it, and how
 * far it must move to clear what of it lies between its feet and its head. With a passage through
 * it, it may have more than one top under a circle (the passage's floor, its own top over the
 * passage) and more than one underside over it (the passage's soffit, its own bottom): so each is
 * asked for, no higher (or no lower) than a height.
 */
public sealed interface Volume permits Prism, Extrusion, Crossing {

    /** How low and how high any of it is. */
    double low();

    double high();

    /** Whether a circle at ({@code x}, {@code z}) overlaps it, seen from above. */
    boolean covers(double x, double z, double radius);

    /**
     * How high its highest top no higher than {@code below} is, anywhere under a circle at
     * ({@code x}, {@code z}); minus infinity if there is none.
     */
    double topAt(double x, double z, double radius, double below);

    /**
     * How low its lowest underside no lower than {@code above} is, anywhere over a circle at
     * ({@code x}, {@code z}); infinity if there is none.
     */
    double undersideAt(double x, double z, double radius, double above);

    /** How high its highest top is anywhere under a circle at ({@code x}, {@code z}); minus infinity if none. */
    default double topAt(final double x, final double z, final double radius) {
        return topAt(x, z, radius, Double.POSITIVE_INFINITY);
    }

    /** How low its lowest underside is anywhere over a circle at ({@code x}, {@code z}); infinity if none. */
    default double undersideAt(final double x, final double z, final double radius) {
        return undersideAt(x, z, radius, Double.NEGATIVE_INFINITY);
    }

    /**
     * How far, across the ground, a circle at ({@code x}, {@code z}) must move to clear what of it
     * lies between {@code feet} and {@code head}; {@code null} if it already does.
     */
    Vec2 push(double x, double z, double radius, double feet, double head);

    /** As {@link #push(double, double, double, double, double)}, for a stroke from a to b. */
    Vec2 push(double ax, double az, double bx, double bz, double radius, double feet, double head);

    /** Where a ray from {@code from} along {@code direction} (a unit vector) first meets it, within {@code reach}; {@code null} if it misses. */
    Hit ray(Vec3 from, Vec3 direction, double reach);

    /** Whether it and {@code block} take up any of the same space: what a platform needs clear to move into. */
    boolean overlaps(Block block);

    /**
     * {@code shape}, given in a building's own frame (its x across, to the right; its z along,
     * ahead; heights up), {@code scale} times as big about its origin, and put where the building
     * stands, {@code at}, turned with it.
     */
    static Volume placed(final Volume shape, final Place at, final double scale) {
        final Vec3 right = Num.right(at.yaw());
        final Vec3 ahead = Num.heading(at.yaw());
        return switch (shape) {
            case Prism p -> new Prism(p.outline().stream().map(v -> new Vec2(
                    at.x() + (right.x() * v.x() + ahead.x() * v.z()) * scale,
                    at.z() + (right.z() * v.x() + ahead.z() * v.z()) * scale)).toList(),
                    p.bottom() * scale, p.top() * scale);
            case Extrusion e -> new Extrusion(at.x() + (right.x() * e.x() + ahead.x() * e.z()) * scale,
                    at.z() + (right.z() * e.x() + ahead.z() * e.z()) * scale, at.yaw() + e.yaw(), e.half() * scale,
                    scaled(e.profile(), scale), e.holes().stream().map(h -> scaled(h, scale)).toList());
            case Crossing c -> new Crossing((Extrusion) placed(c.first(), at, scale),
                    (Extrusion) placed(c.second(), at, scale));
        };
    }

    private static java.util.List<Vec2> scaled(final java.util.List<Vec2> points, final double scale) {
        return points.stream().map(p -> new Vec2(p.x() * scale, p.z() * scale)).toList();
    }
}
