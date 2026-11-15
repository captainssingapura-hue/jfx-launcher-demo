package com.example.fxsuite.avatar.sim;

/**
 * Something solid in the world shaped more closely than a box ({@link Block}): an outline stood up
 * straight ({@link Prism}); one laid on its side and drawn along ({@link Extrusion}); the part two
 * of those share ({@link Crossing}). What is in the way of a body, or a round, as it is.
 *
 * <p>A body meets it as an upright cylinder, as it meets a block: across the ground, a circle; up
 * and down, from its feet to its head. So what each says is about a circle, or a stroke, at some
 * heights: what it covers, how high its top is under it, how low its underside over it, and how
 * far it must move to clear what of it lies between its feet and its head.
 */
public sealed interface Volume permits Prism, Extrusion, Crossing {

    /** How low and how high any of it is. */
    double low();

    double high();

    /** Whether a circle at ({@code x}, {@code z}) overlaps it, seen from above. */
    boolean covers(double x, double z, double radius);

    /** How high its top is anywhere under a circle at ({@code x}, {@code z}); minus infinity if it is not under it. */
    double topAt(double x, double z, double radius);

    /** How low its underside is anywhere over a circle at ({@code x}, {@code z}); infinity if it is not over it. */
    double undersideAt(double x, double z, double radius);

    /**
     * How far, across the ground, a circle at ({@code x}, {@code z}) must move to clear what of it
     * lies between {@code feet} and {@code head}; {@code null} if it already does.
     */
    Vec2 push(double x, double z, double radius, double feet, double head);

    /** As {@link #push(double, double, double, double, double)}, for a stroke from a to b. */
    Vec2 push(double ax, double az, double bx, double bz, double radius, double feet, double head);

    /** Where a ray from {@code from} along {@code direction} (a unit vector) first meets it, within {@code reach}; {@code null} if it misses. */
    Hit ray(Vec3 from, Vec3 direction, double reach);
}
