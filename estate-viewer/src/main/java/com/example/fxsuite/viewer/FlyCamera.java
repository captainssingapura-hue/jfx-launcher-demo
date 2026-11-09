package com.example.fxsuite.viewer;

import javafx.geometry.Point3D;

/**
 * A free camera, as a spectator flies: a position, a heading ({@code yaw}) and a tilt
 * ({@code pitch}), turned in place rather than around a point. Same angles and axes as
 * {@link Orbit}, so the two convert into each other exactly. Pure maths.
 */
public record FlyCamera(Point3D position, double yaw, double pitch) {

    /** Straight up: JavaFX's y axis points down. */
    public static final Point3D UP = new Point3D(0, -1, 0);

    public FlyCamera {
        pitch = Math.max(-Orbit.MAX_PITCH, Math.min(Orbit.MAX_PITCH, pitch));
    }

    /** The orbit's camera, now free. */
    public static FlyCamera from(final Orbit orbit) {
        return new FlyCamera(orbit.position(), orbit.yaw(), orbit.pitch());
    }

    /** An orbit around the point {@code distance} straight ahead: the same view. */
    public Orbit toOrbit(final double distance) {
        final double kept = Math.max(Orbit.MIN_DISTANCE, Math.min(Orbit.MAX_DISTANCE, distance));
        return new Orbit(position.add(forward().multiply(kept)), kept, yaw, pitch);
    }

    public Point3D forward() {
        return axes().forward();
    }

    public Point3D right() {
        return axes().right();
    }

    /** The direction ahead along the ground: where the camera faces, ignoring its tilt. */
    public Point3D heading() {
        final double y = Math.toRadians(yaw);
        return new Point3D(Math.sin(y), 0, Math.cos(y));
    }

    /**
     * Moved as a drone moves: ahead along the heading and sideways, both level whatever the
     * tilt, and straight up. Only {@code upward} changes the height.
     */
    public FlyCamera cruised(final double ahead, final double rightward, final double upward) {
        final Point3D step = heading().multiply(ahead).add(right().multiply(rightward)).add(UP.multiply(upward));
        return new FlyCamera(position.add(step), yaw, pitch);
    }

    /** Turned in place: right and up for positive amounts. */
    public FlyCamera looked(final double right, final double up) {
        return new FlyCamera(position, yaw + right, pitch + up);
    }

    /**
     * Moved along the view ({@code ahead}, up or down included, as a spectator moves),
     * sideways, and straight up, whatever the tilt.
     */
    public FlyCamera moved(final double ahead, final double rightward, final double upward) {
        final Point3D step = forward().multiply(ahead).add(right().multiply(rightward)).add(UP.multiply(upward));
        return new FlyCamera(position.add(step), yaw, pitch);
    }

    /** The camera a fraction {@code t} of the way to {@code target}, turning the short way round. */
    public FlyCamera towards(final FlyCamera target, final double t) {
        final double turn = ((target.yaw - yaw) % 360 + 540) % 360 - 180;
        return new FlyCamera(position.add(target.position.subtract(position).multiply(t)),
                yaw + turn * t, pitch + (target.pitch - pitch) * t);
    }

    /** Kept inside a box, given as its two opposite corners. */
    public FlyCamera within(final Point3D low, final Point3D high) {
        final Point3D kept = new Point3D(
                Math.max(low.getX(), Math.min(high.getX(), position.getX())),
                Math.max(low.getY(), Math.min(high.getY(), position.getY())),
                Math.max(low.getZ(), Math.min(high.getZ(), position.getZ())));
        return new FlyCamera(kept, yaw, pitch);
    }

    private Orbit axes() {
        return new Orbit(position, Orbit.MIN_DISTANCE, yaw, pitch);
    }
}
