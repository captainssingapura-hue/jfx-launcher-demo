package com.example.fxsuite.avatar.sim;

/**
 * A box, its sides square to its frame: in the world, over the ground in x and z, and up (as {@link Place}'s height);
 * in an object's own frame, across (x), along (z) and up. Its sides are part of it: a point on one is in it, and two
 * boxes touching meet.
 */
public record Box(double minX, double maxX, double minZ, double maxZ, double low, double high) {

    public Box {
        if (!(minX <= maxX && minZ <= maxZ && low <= high)) {
            throw new IllegalArgumentException("a box from its least to its most each way: " + minX + ".." + maxX
                    + ", " + minZ + ".." + maxZ + ", " + low + ".." + high);
        }
    }

    /** What a body stood at ({@code x}, {@code z}) takes up: {@code radius} round, from its feet at {@code low}, so tall. */
    public static Box around(final double x, final double z, final double low, final double radius, final double tall) {
        return new Box(x - radius, x + radius, z - radius, z + radius, low, low + tall);
    }

    /** Whether a point is in it, on its sides included. */
    public boolean holds(final double x, final double z, final double up) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ && up >= low && up <= high;
    }

    /** Whether it and another share any of themselves, touching included. */
    public boolean meets(final Box other) {
        return minX <= other.maxX && other.minX <= maxX && minZ <= other.maxZ && other.minZ <= maxZ
                && low <= other.high && other.low <= high;
    }

    /** It grown by {@code side} each way over the ground and by {@code up} under and over it. */
    public Box grown(final double side, final double up) {
        return new Box(minX - side, maxX + side, minZ - side, maxZ + side, low - up, high + up);
    }
}
