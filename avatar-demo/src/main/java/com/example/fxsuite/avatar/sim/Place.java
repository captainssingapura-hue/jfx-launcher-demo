package com.example.fxsuite.avatar.sim;

/**
 * Where something that moves of itself is at a moment: over ({@code x}, {@code z}), at
 * {@code height} (up, as everywhere in the world's own terms), heading {@code yaw} degrees.
 */
public record Place(double x, double z, double height, double yaw) {

    /**
     * Where a point carried along with this place goes when it moves to {@code to}: turned with it
     * about its middle, and moved and lifted with it. For riding on something that moves.
     */
    public Vec3 carry(final Vec3 point, final Place to) {
        final double turn = Math.toRadians(Num.wrap(to.yaw - yaw));
        final double c = Math.cos(turn);
        final double s = Math.sin(turn);
        final double dx = point.x() - x;
        final double dz = point.z() - z;
        // turning a heading by the turn, as Num.heading turns with yaw
        return new Vec3(to.x + dx * c + dz * s, point.y() - (to.height - height), to.z + dz * c - dx * s);
    }
}
