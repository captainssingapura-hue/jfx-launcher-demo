package com.example.fxsuite.avatar.sim;

/**
 * Where a ray met something: the point, the outward direction of the surface there, what it
 * was, how far along the ray, and which piece of the world it was ({@link Obstacles#ray}): its
 * number in the {@link Course}, or -1 for the ground.
 */
public record Hit(Vec3 at, Vec3 normal, Surface on, double distance, int piece) {

    /** Not yet known to be any one piece: the ground, or a block on its own. */
    public Hit(final Vec3 at, final Vec3 normal, final Surface on, final double distance) {
        this(at, normal, on, distance, -1);
    }

    /** The same hit, on piece {@code piece}. */
    public Hit on(final int piece) {
        return new Hit(at, normal, on, distance, piece);
    }
}
